package at.favre.tools.apksigner;

import at.favre.tools.apksigner.signing.AndroidApkSignerVerify;
import at.favre.tools.apksigner.signing.SigningConfig;
import at.favre.tools.apksigner.signing.SigningConfigGen;
import at.favre.tools.apksigner.signing.ZipAlignExecutor;
import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.ui.CLIParser;
import at.favre.tools.apksigner.ui.FileArgParser;
import at.favre.tools.apksigner.util.AndroidApkSignerUtil;
import at.favre.tools.apksigner.util.CmdUtil;
import at.favre.tools.apksigner.util.FileUtil;
import com.android.apksigner.ApkSignerTool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.*;

/**
 * The main tool that manages the logic of the main process while satisfying the passed arguments
 */
public class SignTool {

    private static final String ZIPALIGN_ALIGNMENT = "4";

    public static void main(String[] args) {
        Result result = mainExecute(args);
        if (result != null && result.error) {
            System.exit(1);
        } else if (result != null && result.unsuccessful > 0) {
            System.exit(2);
        }
    }

    static Result mainExecute(String[] args) {
        Arg arguments = CLIParser.parse(args);

        if (arguments != null) {
            return execute(arguments);
        }
        return null;
    }

    private static Result execute(Arg arguments) {
        List<CmdUtil.Result> executedCommands = new ArrayList<>();
        ZipAlignExecutor zipAlignExecutor = null;
        SigningConfigGen signingConfigGen = null;

        int successCount = 0;
        int errorCount = 0;

        try {
            File outFolder = null;
            List<File> targetApkFiles = new FileArgParser().parseAndSortUniqueFilesNonRecursive(arguments.apkFile);

            log("source:");

            for (File targetApkFile : targetApkFiles) {
                log("\t" + targetApkFile.getCanonicalPath());
            }

            if (arguments.out != null) {
                outFolder = new File(arguments.out);

                if (!outFolder.exists()) {
                    outFolder.mkdirs();
                }

                if (!outFolder.exists() || !outFolder.isDirectory()) {
                    throw new IllegalArgumentException("if out directory is provided it must exist and be a path: " + arguments.out);
                }
            }

            if (!arguments.skipZipAlign) {
                zipAlignExecutor = new ZipAlignExecutor(arguments);
                log(zipAlignExecutor.toString());
            }
            if (!arguments.onlyVerify) {
                log("keystore:");
                signingConfigGen = new SigningConfigGen(arguments.signArgsList, arguments.ksIsDebug);
                for (SigningConfig signingConfig : signingConfigGen.signingConfig) {
                    log("\t" + signingConfig.description());
                }
            }

            long startTime = System.currentTimeMillis();

            int iterCount = 0;

            List<File> tempFilesToDelete = new ArrayList<>();
            for (File targetApkFile : targetApkFiles) {
                if (targetApkFile.isFile() && FileUtil.getFileExtension(targetApkFile).toLowerCase().equals("apk")) {
                    iterCount++;
                    File rootTargetFile = targetApkFile;

                    log("\n" + String.format("%02d", iterCount) + ". " + targetApkFile.getName());

                    if (arguments.dryRun) {
                        log("\t- (skip)");
                        continue;
                    }

                    if (!arguments.onlyVerify) {
                        AndroidApkSignerVerify.Result preCheck = verifySign(targetApkFile, rootTargetFile, false, true);

                        if (preCheck.verified && arguments.allowResign) {
                            log("\tWARNING: already signed - will be resigned. Old certificate info: " + preCheck.getCertCountString() + preCheck.getSchemaVersionInfoString());
                            for (AndroidApkSignerVerify.CertInfo certInfo : preCheck.certInfoList) {
                                log("\t\tSubject: " + certInfo.subjectDn);
                                log("\t\tSHA256: " + certInfo.certSha256);
                            }

                        } else if (preCheck.verified) {
                            logErr("\t- already signed SKIP");
                            errorCount++;
                            continue;
                        }
                    }

                    if (!arguments.onlyVerify) {
                        log("\n\tSIGN");
                        log("\tfile: " + rootTargetFile.getCanonicalPath());
                        log("\tchecksum : " + FileUtil.createChecksum(rootTargetFile, "SHA-256") + " (sha256)");


                        targetApkFile = zipAlign(targetApkFile, rootTargetFile, outFolder, zipAlignExecutor, arguments, executedCommands);

                        if (targetApkFile == null) {
                            throw new IllegalStateException("could not execute zipalign");
                        }

                        if (!arguments.overwrite && !arguments.skipZipAlign) {
                            tempFilesToDelete.add(targetApkFile);
                        }

                        targetApkFile = sign(targetApkFile, outFolder, signingConfigGen.signingConfig, arguments);

                    }

                    log("\n\tVERIFY");
                    log("\tfile: " + targetApkFile.getCanonicalPath());
                    log("\tchecksum : " + FileUtil.createChecksum(targetApkFile, "SHA-256") + " (sha256)");

                    boolean zipAlignVerified = arguments.skipZipAlign || verifyZipAlign(targetApkFile, rootTargetFile, zipAlignExecutor, arguments, executedCommands);
                    boolean sigVerified = verifySign(targetApkFile, rootTargetFile, arguments.verbose, false) != null;

                    if (zipAlignVerified && sigVerified) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                }
            }

            if (iterCount == 0) {
                log("No apks found.");
            }

            for (File file : tempFilesToDelete) {
                if (arguments.verbose) {
                    log("delete temp file " + file);
                }
                file.delete();
            }


            log(String.format(Locale.US, "\n[%s][v%s]\nSuccessfully processed %d APKs and %d errors in %.2f seconds.",
                    new Date().toString(), CmdUtil.jarVersion(), successCount, errorCount, (double) (System.currentTimeMillis() - startTime) / 1000.0));

            if (arguments.debug) {
                log(getCommandHistory(executedCommands));
            }
        } catch (Exception e) {
            logErr(e.getMessage());

            if (arguments.debug) {
                e.printStackTrace();
                logErr(getCommandHistory(executedCommands));
            } else {
                logErr("Run with '-debug' parameter to get additional information.");
            }
            return new Result(true, successCount, errorCount);
        } finally {
            if (zipAlignExecutor != null) {
                zipAlignExecutor.cleanUp();
            }

            if (signingConfigGen != null) {
                signingConfigGen.cleanUp();
            }
        }
        return new Result(false, successCount, errorCount);
    }

    private static File zipAlign(File targetApkFile, File rootTargetFile, File outFolder, ZipAlignExecutor executor, Arg arguments, List<CmdUtil.Result> cmdList) {
        if (!arguments.skipZipAlign) {

            String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
            fileName = fileName.replace("-unaligned", "");
            fileName += "-aligned";
            File outFile = new File(outFolder != null ? outFolder : targetApkFile.getParentFile(), fileName + "." + FileUtil.getFileExtension(targetApkFile));

            if (outFile.exists()) {
                outFile.delete();
            }

            if (executor.isExecutableFound()) {
                String logMsg = "\t- ";

                CmdUtil.Result zipAlignResult = CmdUtil.runCmd(CmdUtil.concat(executor.zipAlignExecutable, new String[]{ZIPALIGN_ALIGNMENT, targetApkFile.getAbsolutePath(), outFile.getAbsolutePath()}));
                cmdList.add(zipAlignResult);
                if (zipAlignResult.success()) {
                    logMsg += "zipalign success";
                } else {
                    logMsg += "could not align ";
                }

                logConditionally(logMsg, outFile, !rootTargetFile.equals(outFile), false);

                if (arguments.overwrite) {
                    targetApkFile.delete();
                    outFile.renameTo(targetApkFile);
                    outFile = targetApkFile;
                }
                return zipAlignResult.success() ? outFile : null;
            } else {
                throw new IllegalArgumentException("could not find zipalign - either skip it or provide a proper location");
            }

        }
        return targetApkFile;
    }


    private static boolean verifyZipAlign(File targetApkFile, File rootTargetFile, ZipAlignExecutor executor, Arg arguments, List<CmdUtil.Result> cmdList) {
        if (!arguments.skipZipAlign) {
            if (executor.isExecutableFound()) {
                String logMsg = "\t- ";

                CmdUtil.Result zipAlignVerifyResult = CmdUtil.runCmd(CmdUtil.concat(executor.zipAlignExecutable, new String[]{"-c", ZIPALIGN_ALIGNMENT, targetApkFile.getAbsolutePath()}));
                cmdList.add(zipAlignVerifyResult);
                boolean success = zipAlignVerifyResult.success();

                if (success) {
                    logMsg += "zipalign verified";
                } else {
                    logMsg += "zipalign VERIFY FAILED";
                }

                logConditionally(logMsg, targetApkFile, !targetApkFile.equals(rootTargetFile), !success);

                return zipAlignVerifyResult.success();
            } else {
                throw new IllegalArgumentException("could not find zipalign - either skip it or provide a proper location");
            }
        }
        return true;
    }

    private static File sign(File targetApkFile, File outFolder, List<SigningConfig> signingConfigs, Arg arguments) {
        try {
            File outFile = targetApkFile;

            if (!arguments.overwrite) {
                String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
                fileName = fileName.replace("-unsigned", "");
                if (signingConfigs.size() == 1 && signingConfigs.get(0).isDebugType) {
                    fileName += "-debugSigned";
                } else {
                    fileName += "-signed";
                }
                outFile = new File(outFolder != null ? outFolder : targetApkFile.getParentFile(), fileName + "." + FileUtil.getFileExtension(targetApkFile));

                if (outFile.exists()) {
                    outFile.delete();
                }
            }

            ByteArrayOutputStream apkSignerToolStream = new ByteArrayOutputStream();
            PrintStream sout = System.out;
            System.setOut(new PrintStream(apkSignerToolStream));
            ApkSignerTool.main(AndroidApkSignerUtil.createApkToolArgs(arguments, signingConfigs, targetApkFile, outFile));
            String output = apkSignerToolStream.toString("UTF-8").trim();
            System.setOut(sout);

            log("\t- sign success");

            if (arguments.verbose && !output.isEmpty()) {
                log("\t\t" + output);
            }

            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("could not sign " + targetApkFile + ": " + e.getMessage(), e);
        }
    }

    private static AndroidApkSignerVerify.Result verifySign(File targetApkFile, File rootTargetFile, boolean verbose, boolean noLog) {
        try {
            AndroidApkSignerVerify verifier = new AndroidApkSignerVerify();
            AndroidApkSignerVerify.Result result = verifier.verify(targetApkFile, null, null, false);

            if (!noLog) {
                String logMsg;

                if (result.verified) {
                    logMsg = "\t- signature verified " + result.getCertCountString() + result.getSchemaVersionInfoString();
                } else {
                    logMsg = "\t- signature VERIFY FAILED (" + targetApkFile.getName() + ")";
                }

                logConditionally(logMsg, targetApkFile, !rootTargetFile.equals(targetApkFile), !result.verified);

                if (!result.errors.isEmpty()) {
                    for (String e : result.errors) {
                        logErr("\t\t" + e);
                    }
                }

                if (verbose && !result.warnings.isEmpty()) {
                    for (String w : result.warnings) {
                        log("\t\t" + w);
                    }
                } else if (!result.warnings.isEmpty()) {
                    log("\t\t" + result.warnings.size() + " warnings");
                }

                if (result.verified) {
                    for (int i = 0; i < result.certInfoList.size(); i++) {
                        AndroidApkSignerVerify.CertInfo certInfo = result.certInfoList.get(i);

                        log("\t\t" + certInfo.subjectDn);
                        log("\t\tSHA256: " + certInfo.certSha256 + " / " + certInfo.sigAlgo);
                        if (verbose) {
                            log("\t\tSHA1: " + certInfo.certSha1);
                            log("\t\t" + certInfo.issuerDn);
                            log("\t\tPublic Key SHA256: " + certInfo.pubSha256);
                            log("\t\tPublic Key SHA1: " + certInfo.pubSha1);
                            log("\t\tPublic Key Algo: " + certInfo.pubAlgo + " " + certInfo.pubKeysize);
                            log("\t\tIssue Date: " + certInfo.beginValidity);

                        }
                        log("\t\tExpires: " + certInfo.expiry.toString());

                        if (i < result.certInfoList.size() - 1) {
                            log("");
                        }
                    }
                }
            }
            return result.verified ? result : null;
        } catch (Exception e) {
            throw new IllegalStateException("could not verifySign " + targetApkFile + ": " + e.getMessage());
        }
    }

    private static String getCommandHistory(List<CmdUtil.Result> executedCommands) {
        StringBuilder sb = new StringBuilder("\nCmd history for debugging purpose:\n-----------------------\n");
        for (CmdUtil.Result executedCommand : executedCommands) {
            sb.append(executedCommand.toString());
        }
        return sb.toString();
    }


    private static void logErr(String msg) {
        System.err.println(msg);
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    private static void logConditionally(String logMsg, File file, boolean appendFile, boolean error) {
        if (appendFile && error) {
            logMsg += " (" + file.getName() + ")";
        }

        if (error) {
            logErr(logMsg);
        } else {
            log(logMsg);
        }
    }

    static class Result {
        final boolean error;
        final int success;
        final int unsuccessful;

        Result(boolean error, int success, int unsuccessful) {
            this.error = error;
            this.success = success;
            this.unsuccessful = unsuccessful;
        }
    }
}
