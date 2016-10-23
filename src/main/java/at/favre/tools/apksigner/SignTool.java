package at.favre.tools.apksigner;

import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.ui.CLIParser;
import at.favre.tools.apksigner.util.CmdUtil;
import at.favre.tools.apksigner.util.FileUtil;
import com.android.apksigner.ApkSignerTool;

import java.io.File;
import java.util.*;

public class SignTool {

    public static void main(String[] args) {
        Arg arguments = CLIParser.parse(args);

        if (arguments != null) {
            execute(arguments);
        }
    }

    private static void execute(Arg arguments) {
        List<CmdUtil.Result> executedCommands = new ArrayList<>();

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

            if (arguments.out != null) {
                outFolder = new File(arguments.out);

                if (!outFolder.exists()) {
                    outFolder.mkdirs();
                }

                if (!outFolder.exists() || !outFolder.isDirectory()) {
                    throw new IllegalArgumentException("if out directory is provided it must exist and be a path: " + arguments.out);
                }
            }

            for (File targetApkFile : targetApkFiles) {
                if (targetApkFile.isFile() && FileUtil.getFileExtension(targetApkFile).toLowerCase().equals("apk")) {
                    log("\n\r" + targetApkFile.getName());
                    targetApkFile = zipAlign(false, targetApkFile, outFolder, arguments, executedCommands);
                    targetApkFile = sign(targetApkFile, outFolder, arguments);
                    zipAlign(true, targetApkFile, outFolder, arguments, executedCommands);
                    verify(targetApkFile, arguments);

                }
            }

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
        }
    }

    private static File zipAlign(boolean onlyVerify, File targetApkFile, File outFolder, Arg arguments, List<CmdUtil.Result> cmdList) {
        if (!arguments.skipZipAlign) {
            ZipAlignExecutor executor = new ZipAlignExecutor(arguments);

            File outFile = targetApkFile;
            if (!onlyVerify) {
                if (!arguments.overwrite) {
                    String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
                    fileName = fileName.replace("-unaligned", "");
                    fileName += "_aligned";
                    outFile = new File(outFolder, fileName + "." + FileUtil.getFileExtension(targetApkFile));
                }

                if (outFile.exists()) {
                    outFile.delete();
                }
            }

            if (executor.isExecutableFound()) {
                String logMsg = "\t- ";

                if (!onlyVerify) {
                    CmdUtil.Result zipAlignResult = CmdUtil.runCmd(new String[]{executor.zipAlignExecutable.getAbsolutePath(), "4", targetApkFile.getAbsolutePath(), outFile.getAbsolutePath()});
                    cmdList.add(zipAlignResult);
                    if (zipAlignResult.success()) {
                        logMsg += "aligned & ";
                    } else {
                        logMsg += "could not align";
                    }
                }

                CmdUtil.Result zipAlignVerifyResult = CmdUtil.runCmd(new String[]{executor.zipAlignExecutable.getAbsolutePath(), "-c", "4", outFile.getAbsolutePath()});
                cmdList.add(zipAlignVerifyResult);
                if (zipAlignVerifyResult.success()) {
                    logMsg += "zipalign verified";
                } else {
                    logMsg += "zipalign verify failed";
                }


                log(logMsg + " (" + outFile.getName() + ")");
            } else {
                throw new IllegalArgumentException("could not find zipalign - either skip it or provide a proper location");
            }
            return outFile;
        }
        return targetApkFile;
    }

    private static File sign(File targetApkFile, File outFolder, Arg arguments) {
        try {
            File outFile = targetApkFile;

            if (!arguments.overwrite) {
                String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
                fileName = fileName.replace("-unsigned", "");
                fileName += "_signed";
                outFile = new File(outFolder, fileName + "." + FileUtil.getFileExtension(targetApkFile));
            }

            if (outFile.exists()) {
                outFile.delete();
            }

            SigningConfig signingConfig = new SigingConfigEngine().generate(arguments);

            ApkSignerTool.main(new String[]{
                    "sign",
                    "--ks", signingConfig.keystore.getAbsolutePath(),
                    "--ks-pass", signingConfig.ksPass == null ? "stdout" : "pass:" + signingConfig.ksPass,
                    "--key-pass", signingConfig.ksKeyPass == null ? "stdout" : "pass:" + signingConfig.ksPass,
                    "--ks-key-alias", signingConfig.ksAlias,
                    "--out", outFile.getAbsolutePath(),
                    targetApkFile.getAbsolutePath()
            });

            log("\t- signed [" + signingConfig.location + "] (" + outFile.getName() + ")");

            return outFile;
        } catch (Exception e) {
            throw new IllegalStateException("could not sign " + targetApkFile + ": " + e.getMessage(), e);
        }
    }

    private static void verify(File targetApkFile, Arg arguments) {
        try {
            ApkSignerTool.main(new String[]{
                    "verify",
                    targetApkFile.getAbsolutePath()
            });
            log("\t- signature verified");
        } catch (Exception e) {
            throw new IllegalStateException("could not verify " + targetApkFile + ": " + e.getMessage());
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
}
