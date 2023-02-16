package at.favre.tools.apksigner;

import at.favre.tools.apksigner.signing.*;
import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.ui.CLIParser;
import at.favre.tools.apksigner.ui.FileArgParser;
import at.favre.tools.apksigner.util.AndroidApkSignerUtil;
import at.favre.tools.apksigner.util.CmdUtil;
import at.favre.tools.apksigner.util.FileUtil;
import com.android.apksigner.ApkSignerTool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The main tool that manages the logic of the main process while satisfying the passed arguments
 */
public final class SignTool {

    private static final String ZIPALIGN_ALIGNMENT = "4";
    private static final String APK_FILE_EXTENSION = "apk";

    private SignTool() {
    }

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

    private static Result execute(Arg args) {
        List<CmdUtil.Result> executedCommands = new ArrayList<>();
        ZipAlignExecutor zipAlignExecutor = null;
        SigningConfigGen signingConfigGen = null;

        int successCount = 0;
        int errorCount = 0;

        try {
            File outFolder = null;
            List<File> targetApkFiles = new FileArgParser().parseAndSortUniqueFilesNonRecursive(args.apkFile, APK_FILE_EXTENSION);

            if (targetApkFiles.isEmpty()) {
                throw new IllegalStateException("no apk files found in given paths");
            }

            log("source:");

            for (String path : FileArgParser.getDirSummary(targetApkFiles)) {
                log("\t" + path);
            }

            if (args.out != null) {
                outFolder = new File(args.out);

                if (!outFolder.exists()) {
                    outFolder.mkdirs();
                }

                if (!outFolder.exists() || !outFolder.isDirectory()) {
                    throw new IllegalArgumentException("if out directory is provided it must exist and be a path: " + args.out);
                }
            }


            if (!args.skipZipAlign) {
                zipAlignExecutor = new ZipAlignExecutor(args);
                log(zipAlignExecutor.toString());
            }

            if (!args.onlyVerify) {
                log("keystore:");
                signingConfigGen = new SigningConfigGen(args.signArgsList, args.ksIsDebug);
                for (SigningConfig signingConfig : signingConfigGen.signingConfig) {
                    log("\t" + signingConfig.description());
                }
            }

            if (args.lineageFilePath != null) {
                processLineagePath(args);
            }

            long startTime = System.currentTimeMillis();

            int iterCount = 0;

            List<File> tempFilesToDelete = new ArrayList<>();
            for (File targetApkFile : targetApkFiles) {
                iterCount++;
                File rootTargetFile = targetApkFile;

                log("\n" + String.format("%02d", iterCount) + ". " + targetApkFile.getName());

                if (args.dryRun) {
                    log("\t- (skip)");
                    continue;
                }

                if (!args.onlyVerify) {
                    AndroidApkSignerVerify.Result preCheck = verifySign(targetApkFile, rootTargetFile, args.checkCertSha256, false, true);

                    if (preCheck != null && args.allowResign) {
                        log("\tWARNING: already signed - will be resigned. Old certificate info: " + preCheck.getCertCountString() + preCheck.getSchemaVersionInfoString());
                        for (AndroidApkSignerVerify.CertInfo certInfo : preCheck.certInfoList) {
                            log("\t\tSubject: " + certInfo.subjectDn);
                            log("\t\tSHA256: " + certInfo.certSha256);
                        }

                    } else if (preCheck != null) {
                        logErr("\t- already signed SKIP");
                        errorCount++;
                        continue;
                    }
                }

                if (!args.onlyVerify) {
                    log("\n\tSIGN");
                    log("\tfile: " + rootTargetFile.getCanonicalPath() + " (" + FileUtil.getFileSizeMb(targetApkFile) + ")");
                    log("\tchecksum: " + FileUtil.createChecksum(rootTargetFile, "SHA-256") + " (sha256)");

                    targetApkFile = zipAlign(targetApkFile, rootTargetFile, outFolder, zipAlignExecutor, args, executedCommands);

                    if (targetApkFile == null) {
                        throw new IllegalStateException("could not execute zipalign");
                    }

                    if (!args.overwrite && !args.skipZipAlign) {
                        tempFilesToDelete.add(targetApkFile);
                    }

                    targetApkFile = sign(targetApkFile, outFolder, signingConfigGen.signingConfig, args);

                }

                log("\n\tVERIFY");
                log("\tfile: " + targetApkFile.getCanonicalPath() + " (" + FileUtil.getFileSizeMb(targetApkFile) + ")");
                log("\tchecksum: " + FileUtil.createChecksum(targetApkFile, "SHA-256") + " (sha256)");

                boolean zipAlignVerified = args.skipZipAlign || verifyZipAlign(targetApkFile, rootTargetFile, zipAlignExecutor, args, executedCommands);
                boolean sigVerified = verifySign(targetApkFile, rootTargetFile, args.checkCertSha256, args.verbose, false) != null;

                if (zipAlignVerified && sigVerified) {
                    successCount++;
                } else {
                    errorCount++;
                }
            }

            if (iterCount == 0) {
                log("No apks found.");
            }

            deleteTempFiles(args, tempFilesToDelete);

            log(String.format(Locale.US, "\n[%s][v%s]\nSuccessfully processed %d APKs and %d errors in %.2f seconds.",
                    new Date().toString(), CmdUtil.jarVersion(), successCount, errorCount, (double) (System.currentTimeMillis() - startTime) / 1000.0));

            if (args.debug) {
                log(getCommandHistory(executedCommands));
            }
        } catch (Exception e) {
            logException(args, executedCommands, e);
            return new Result(true, successCount, errorCount);
        } finally {
            cleanup(zipAlignExecutor, signingConfigGen);
        }
        return new Result(false, successCount, errorCount);
    }

    private static void processLineagePath(Arg args) throws IOException {
        File lineageFile = new File(args.lineageFilePath);
        if (!lineageFile.exists() || !lineageFile.isFile()) {
            throw new IllegalArgumentException("lineage file either does not exist or is not a file: " + args.lineageFilePath);
        }
        log("lineage:");
        log("\t" + FileUtil.createChecksum(lineageFile, "SHA-256").substring(0, 8) + " " + lineageFile.getCanonicalPath());
    }

    private static void deleteTempFiles(Arg args, List<File> tempFilesToDelete) {
        for (File file : tempFilesToDelete) {
            if (args.verbose) {
                log("delete temp file " + file);
            }
            file.delete();
        }
    }

    private static void cleanup(ZipAlignExecutor zipAlignExecutor, SigningConfigGen signingConfigGen) {
        if (zipAlignExecutor != null) {
            zipAlignExecutor.cleanUp();
        }

        if (signingConfigGen != null) {
            signingConfigGen.cleanUp();
        }
    }

    private static void logException(Arg args, List<CmdUtil.Result> executedCommands, Exception e) {
        logErr(e.getMessage());

        if (args.debug) {
            e.printStackTrace();
            logErr(getCommandHistory(executedCommands));
        } else {
            logErr("Run with '--debug' parameter to get additional information.");
        }
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

                CmdUtil.Result zipAlignResult = CmdUtil.runCmd(CmdUtil.concat(executor.getZipAlignExecutable(), new String[]{"-p", "-v", ZIPALIGN_ALIGNMENT, targetApkFile.getAbsolutePath(), outFile.getAbsolutePath()}));
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

                CmdUtil.Result zipAlignVerifyResult = CmdUtil.runCmd(CmdUtil.concat(executor.getZipAlignExecutable(), new String[]{"-c", ZIPALIGN_ALIGNMENT, targetApkFile.getAbsolutePath()}));
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

    private static AndroidApkSignerVerify.Result verifySign(File targetApkFile, File rootTargetFile, String[] checkHashes, boolean verbose, boolean preCheckVerify) {
        try {
            AndroidApkSignerVerify verifier = new AndroidApkSignerVerify();
            AndroidApkSignerVerify.Result result = verifier.verify(targetApkFile, null, null, null, false);

            if (!preCheckVerify) {
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

                CertHashChecker.Result certHashResult = new CertHashChecker().check(result, checkHashes);
                if (certHashResult != null) {
                    if (!certHashResult.verified) {
                        log("\t- verify with provided hash check failed " + certHashResult.hashSummary());
                        logErr("\t\tERROR: " + certHashResult.errorString);
                    } else {
                        log("\t- verify with provided hash successful " + certHashResult.hashSummary());
                    }
                    return result.verified && certHashResult.verified ? result : null;
                }

            }
            return result.verified ? result : null;
        } catch (Exception e) {
            throw new IllegalStateException("could not verify " + targetApkFile + ": " + e.getMessage(), e);
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
