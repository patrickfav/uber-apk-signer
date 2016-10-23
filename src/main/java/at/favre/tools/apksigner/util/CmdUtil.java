package at.favre.tools.apksigner.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class CmdUtil {

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
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader inStreamReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while ((inStreamReader.readLine()) != null) {
                }
            }
            process.waitFor();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static OS getOsType() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return OS.WIN;
        }
        if (osName.contains("win")) {
            return OS.MAC;
        }

        return OS._NIX;
    }

    public enum OS {
        WIN, MAC, _NIX;
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
}
