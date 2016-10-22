package at.favre.tools.apksigner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class CmdUtil {

    public static Result runCmd(String[] cmdArray) {
        StringBuilder logStringBuilder = new StringBuilder();
        Exception exception = null;
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
        } catch (Exception e) {
            exception = e;
        }
        return new Result(logStringBuilder.toString(), exception, cmdArray);
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

    public static class Result {
        public final Exception exception;
        public final String out;
        public final String cmd;

        public Result(String out, Exception exception, String[] cmd) {
            this.out = out;
            this.exception = exception;
            this.cmd = Arrays.toString(cmd);
        }

        @Override
        public String toString() {
            return cmd+"\n"+out+"\n";
        }
    }
}
