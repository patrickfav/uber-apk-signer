package at.favre.tools.apksigner;

import at.favre.tools.apksigner.parser.AdbDevice;
import at.favre.tools.apksigner.parser.AdbDevicesParser;
import at.favre.tools.apksigner.parser.InstalledPackagesParser;
import at.favre.tools.apksigner.parser.PackageMatcher;
import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.ui.CLIParser;
import com.android.apksigner.ApkSignerTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
            List<File> targetApkFiles = new ArrayList<>();

            if (argApkFile.exists() && argApkFile.isDirectory()) {
                Collections.addAll(targetApkFiles, argApkFile.listFiles());
            } else if (argApkFile.exists()) {
                targetApkFiles.add(argApkFile);
            } else {
                throw new IllegalArgumentException("provided apk path " + arguments.apkFile + " does not exist");
            }

            for (File targetApkFile : targetApkFiles) {
                if (targetApkFile.isFile() && FileUtil.getFileExtension(targetApkFile).toLowerCase().equals("apk")) {
                    log("\r" + targetApkFile.getName());
                    zipAlign(targetApkFile, arguments);
                    sign(targetApkFile, arguments);
                    verify(targetApkFile, arguments);
                }
            }

            if (arguments.debug) {
                log(getCommandHistory(executedCommands));
            }
        } catch (Exception e) {
            logErr(e.getMessage());

            if (arguments.debug) {
                logErr(getCommandHistory(executedCommands));
            } else {
                logErr("Run with '-debug' parameter to get additional information.");
            }
        }
    }

    private static void zipAlign(File targetApkFile, Arg arguments) {
        if (!arguments.skipZipAlign) {
            File zipAlignToolFile = null;
            if (arguments.zipAlignPath != null) {
                zipAlignToolFile = new File(arguments.zipAlignPath);
            } else {
                //TODO use embedded
            }

            File outFile = targetApkFile;
            if (!arguments.overwrite) {
                String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
                fileName = fileName.replace("-unaligned", "");
                fileName += "_aligned";
                outFile = new File(targetApkFile.getParentFile(), fileName + "." + FileUtil.getFileExtension(targetApkFile));
            }

            if (zipAlignToolFile != null && zipAlignToolFile.exists() && zipAlignToolFile.isFile()) {
                CmdUtil.runCmd(new String[]{zipAlignToolFile.getAbsolutePath(), "4", targetApkFile.getAbsolutePath(), outFile.getAbsolutePath()});
                log("\r\r- aligned");
            } else {
                throw new IllegalArgumentException("could not find zipalign - either skip it or provide a proper location");
            }
        }
    }

    private static void sign(File targetApkFile, Arg arguments) {
        try {
            File outFile = targetApkFile;

            if (!arguments.overwrite) {
                String fileName = FileUtil.getFileNameWithoutExtension(targetApkFile);
                fileName = fileName.replace("-unsigned", "");
                fileName += "_signed";
                outFile = new File(targetApkFile.getParentFile(), fileName + "." + FileUtil.getFileExtension(targetApkFile));
            }

            SigningConfig signingConfig = new SigingConfigEngine().generate(arguments);

            ApkSignerTool.main(new String[]{
                    "sign",
                    "--ks", arguments.ksFile,
                    "--ks-key-alias", arguments.ksAliasName,
                    "--ks-pass ", signingConfig.ksPass == null ? "stdout" : signingConfig.ksPass,
                    "--key-pass", signingConfig.ksKeyPass == null ? "stdout" : signingConfig.ksPass,
                    "--out", outFile.getAbsolutePath(),
                    targetApkFile.getAbsolutePath()
            });

            log("\r\r- signed (" + signingConfig.location + ")");
        } catch (Exception e) {
            throw new IllegalStateException("could not sign " + targetApkFile + ": " + e.getMessage());
        }
    }

    private static void verify(File targetApkFile, Arg arguments) {
        try {
            ApkSignerTool.main(new String[]{
                    "verify",
                    targetApkFile.getAbsolutePath()
            });
            log("\r\r- verified");
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
