package at.favre.tools.apksigner;

import at.favre.tools.apksigner.signing.AndroidApkSignerVerify;
import at.favre.tools.apksigner.signing.SigningConfig;
import at.favre.tools.apksigner.signing.SigningConfigGen;
import at.favre.tools.apksigner.signing.ZipAlignExecutor;
import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.ui.CLIParser;
import at.favre.tools.apksigner.util.CmdUtil;
import at.favre.tools.apksigner.util.FileUtil;
import com.android.apksigner.ApkSignerTool;

import java.io.File;
import java.util.*;

public class SignTool {

    private static final String ZIPALIGN_ALIGNMENT = "4";

    public static void main(String[] args) {
        Result result = mainExecute(args);
        if (result != null && result.error) {
            System.exit(1);
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
            File argApkFile = new File(arguments.apkFile);
            File outFolder;
            List<File> targetApkFiles = new ArrayList<>();

            if (argApkFile.exists() && argApkFile.isDirectory()) {
                Collections.addAll(targetApkFiles, argApkFile.listFiles());
                outFolder = argApkFile;
            } else if (argApkFile.exists()) {
                targetApkFiles.add(argApkFile);
                outFolder = argApkFile.getParentFile();
            } else {
                throw new IllegalArgumentException("provided apk path " + arguments.apkFile + " does not exist");
            }

            log("Using source directory '" + outFolder.getAbsolutePath() + "'.");

            Collections.sort(targetApkFiles);

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
                signingConfigGen = new SigningConfigGen(arguments);
                log(signingConfigGen.signingConfig.description());
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
                        log("\t - (skip)");
                        continue;
                    }

                    if (!arguments.onlyVerify && verifySign(targetApkFile, rootTargetFile, false, true)) {
                        logErr("\t - already signed SKIP");
                        errorCount++;
                        continue;
                    }

                    if (!arguments.onlyVerify) {
                        targetApkFile = zipAlign(targetApkFile, rootTargetFile, outFolder, zipAlignExecutor, arguments, executedCommands);

                        if (targetApkFile == null) {
                            throw new IllegalStateException("could not execute zipalign");
                        }

                        if (!arguments.overwrite) {
                            tempFilesToDelete.add(targetApkFile);
                        }

                        targetApkFile = sign(targetApkFile, rootTargetFile, outFolder, signingConfigGen.signingConfig, arguments);

                    }

                    logChecksum(rootTargetFile, targetApkFile);

                    boolean zipAlignVerified = arguments.skipZipAlign || verifyZipAlign(targetApkFile, rootTargetFile, zipAlignExecutor, arguments, executedCommands);
                    boolean sigVerified = verifySign(targetApkFile, rootTargetFile, arguments.verbose, false);

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


            log(String.format(Locale.US, "\n[v%s|%s] Successfully processed %d APKs and %d errors in %.2f seconds.",
                    CmdUtil.jarVersion(), new Date().toString(), successCount, errorCount, (double) (System.currentTimeMillis() - startTime) / 1000.0));

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

    private static void logChecksum(File before, File after) throws Exception {
        if (after == null || before.equals(after)) {
            log("\t- checksum [sha256]: " + FileUtil.createChecksum(before, "SHA-256"));
        } else {
            log("\t- checksum [sha256]: " + FileUtil.createChecksum(before, "SHA-256") + " (original)");
            log("\t- checksum [sha256]: " + FileUtil.createChecksum(after, "SHA-256") + " (singed)");

        }
    }

    private static File zipAlign(File targetApkFile, File rootTargetFile, File outFolder, ZipAlignExecutor executor, Arg arguments, List<CmdUtil.Result> cmdList) {
        if (!arguments.skipZipAlign) {

            String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
            fileName = fileName.replace("-unaligned", "");
            fileName += "-aligned";
            File outFile = new File(outFolder, fileName + "." + FileUtil.getFileExtension(targetApkFile));

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

    private static File sign(File targetApkFile, File rootTargetFile, File outFolder, SigningConfig signingConfig, Arg arguments) {
        try {
            File outFile = targetApkFile;

            if (!arguments.overwrite) {
                String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
                fileName = fileName.replace("-unsigned", "");
                if (signingConfig.isDebugType) {
                    fileName += "-debugSigned";
                } else {
                    fileName += "-signed";
                }
                outFile = new File(outFolder, fileName + "." + FileUtil.getFileExtension(targetApkFile));

                if (outFile.exists()) {
                    outFile.delete();
                }
            }

            String[] argArr = new String[]{
                    "sign",
                    "--ks", signingConfig.keystore.getAbsolutePath(),
                    "--ks-pass", signingConfig.ksPass == null ? "stdin" : "pass:" + signingConfig.ksPass,
                    "--key-pass", signingConfig.ksKeyPass == null ? "stdin" : "pass:" + signingConfig.ksKeyPass,
                    "--ks-key-alias", signingConfig.ksAlias,
                    "--out", outFile.getAbsolutePath()
            };

//            if (arguments.verbose) {
//                argArr = CmdUtil.concat(argArr, new String[]{"--verbose"});
//            }

            argArr = CmdUtil.concat(argArr, new String[]{
                    targetApkFile.getAbsolutePath()
            });

            ApkSignerTool.main(argArr);

            String logMsg = "\t- sign success";


            if (!rootTargetFile.equals(outFile)) {
                logMsg += " (" + outFile.getName() + ")";
            }

            log(logMsg);


            return outFile;
        } catch (Exception e) {
            throw new IllegalStateException("could not sign " + targetApkFile + ": " + e.getMessage(), e);
        }
    }

    private static boolean verifySign(File targetApkFile, File rootTargetFile, boolean verbose, boolean noLog) {
        try {
            AndroidApkSignerVerify verifier = new AndroidApkSignerVerify();
            AndroidApkSignerVerify.Result result = verifier.verify(targetApkFile, null, null, false);

            if (!noLog) {
                String logMsg;

                if (result.verified) {
                    logMsg = "\t- signature verified [" + (result.v1Schema ? "v1" : "") + (result.v1Schema && result.v2Schema ? ", " : "") + (result.v2Schema ? "v2" : "") + "] ";
                } else {
                    logMsg = "\t- signature VERIFY FAILED (" + targetApkFile.getName() + ")";
                }

                logConditionally(logMsg, targetApkFile, !rootTargetFile.equals(targetApkFile), !result.verified);

                if (result.verified) {
                    for (AndroidApkSignerVerify.CertInfo certInfo : result.certInfoList) {
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
                    }
                }
            }
            return result.verified;
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
        if (appendFile) {
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
