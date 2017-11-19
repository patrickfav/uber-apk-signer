package at.favre.tools.apksigner.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

public final class CmdUtil {

    private CmdUtil() {
    }

    public static Result runCmd(String[] cmdArray) {
        StringBuilder logStringBuilder = new StringBuilder();
        Exception exception = null;
        int exitValue = -1;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader inStreamReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String s;
                while ((s = inStreamReader.readLine()) != null) {
                    if (!s.isEmpty()) logStringBuilder.append(s).append("\n");
                }
            }
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            exception = e;
        }
        return new Result(logStringBuilder.toString(), exception, cmdArray, exitValue);
    }

    public static boolean canRunCmd(String[] cmd) {
        Result result = runCmd(cmd);
        return result.exception == null;
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static File checkAndGetFromPATHEnvVar(final String matchesExecutable) {
        String separator = ":";
        if (getOsType() == OS.WIN) {
            separator = ";";
        }

        String[] pathParts = System.getenv("PATH").split(separator);
        for (String pathPart : pathParts) {
            File pathFile = new File(pathPart);

            if (pathFile.isFile() && pathFile.getName().toLowerCase().contains(matchesExecutable)) {
                return pathFile;
            } else if (pathFile.isDirectory()) {
                File[] matchedFiles = pathFile.listFiles(pathname -> pathname.getName().toLowerCase().contains(matchesExecutable));

                for (File matchedFile : matchedFiles) {
                    if (CmdUtil.canRunCmd(new String[]{matchedFile.getAbsolutePath()})) {
                        return matchedFile;
                    }
                }
            }
        }
        return null;
    }

    public static OS getOsType() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return OS.WIN;
        }
        if (osName.contains("mac")) {
            return OS.MAC;
        }

        return OS._NIX;
    }

    public enum OS {
        WIN, MAC, _NIX
    }

    public static class Result {
        public final Exception exception;
        public final String out;
        public final String cmd;
        public final int exitValue;

        public Result(String out, Exception exception, String[] cmd, int exitValue) {
            this.out = out;
            this.exception = exception;
            this.cmd = Arrays.toString(cmd);
            this.exitValue = exitValue;
        }

        @Override
        public String toString() {
            return cmd + "\n" + out + "\n";
        }

        public boolean success() {
            return exitValue == 0;
        }
    }

    public static String jarVersion() {
        return CmdUtil.class.getPackage().getImplementationVersion();
    }
}
