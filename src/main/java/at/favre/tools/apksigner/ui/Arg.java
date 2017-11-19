package at.favre.tools.apksigner.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The model for the passed arguments
 */
public class Arg {
    //CHECKSTYLE:OFF -- I do want a concise class with only public access
    public String[] apkFile;
    public String out;

    public List<SignArgs> signArgsList = new ArrayList<>();

    public boolean overwrite = false;
    public boolean dryRun = false;
    public boolean verbose = false;
    public boolean skipZipAlign = false;
    public boolean debug = false;
    public boolean onlyVerify = false;
    public boolean ksIsDebug = false;
    public boolean allowResign;

    public String zipAlignPath;
    public String[] checkCertSha256;
    //CHECKSTYLE:ON

    Arg() {
    }

    Arg(String[] apkFile, String out, List<SignArgs> list,
        boolean overwrite, boolean dryRun, boolean verbose, boolean skipZipAlign, boolean debug, boolean onlyVerify,
        String zipAlignPath, boolean ksIsDebug, boolean allowResign, String[] checkCertSha256) {
        this.apkFile = apkFile;
        this.out = out;
        this.signArgsList = list;
        this.overwrite = overwrite;
        this.dryRun = dryRun;
        this.verbose = verbose;
        this.skipZipAlign = skipZipAlign;
        this.debug = debug;
        this.onlyVerify = onlyVerify;
        this.zipAlignPath = zipAlignPath;
        this.ksIsDebug = ksIsDebug;
        this.allowResign = allowResign;
        this.checkCertSha256 = checkCertSha256;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Arg arg = (Arg) o;

        if (overwrite != arg.overwrite) return false;
        if (dryRun != arg.dryRun) return false;
        if (verbose != arg.verbose) return false;
        if (skipZipAlign != arg.skipZipAlign) return false;
        if (debug != arg.debug) return false;
        if (onlyVerify != arg.onlyVerify) return false;
        if (ksIsDebug != arg.ksIsDebug) return false;
        if (allowResign != arg.allowResign) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(apkFile, arg.apkFile)) return false;
        if (out != null ? !out.equals(arg.out) : arg.out != null) return false;
        if (signArgsList != null ? !signArgsList.equals(arg.signArgsList) : arg.signArgsList != null) return false;
        if (zipAlignPath != null ? !zipAlignPath.equals(arg.zipAlignPath) : arg.zipAlignPath != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(checkCertSha256, arg.checkCertSha256);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(apkFile);
        result = 31 * result + (out != null ? out.hashCode() : 0);
        result = 31 * result + (signArgsList != null ? signArgsList.hashCode() : 0);
        result = 31 * result + (overwrite ? 1 : 0);
        result = 31 * result + (dryRun ? 1 : 0);
        result = 31 * result + (verbose ? 1 : 0);
        result = 31 * result + (skipZipAlign ? 1 : 0);
        result = 31 * result + (debug ? 1 : 0);
        result = 31 * result + (onlyVerify ? 1 : 0);
        result = 31 * result + (ksIsDebug ? 1 : 0);
        result = 31 * result + (allowResign ? 1 : 0);
        result = 31 * result + (zipAlignPath != null ? zipAlignPath.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(checkCertSha256);
        return result;
    }

    @Override
    public String toString() {
        return "Arg{" +
                "apkFile=" + Arrays.toString(apkFile) +
                ", out='" + out + '\'' +
                ", signArgsList=" + signArgsList +
                ", overwrite=" + overwrite +
                ", dryRun=" + dryRun +
                ", verbose=" + verbose +
                ", skipZipAlign=" + skipZipAlign +
                ", debug=" + debug +
                ", onlyVerify=" + onlyVerify +
                ", ksIsDebug=" + ksIsDebug +
                ", allowResign=" + allowResign +
                ", zipAlignPath='" + zipAlignPath + '\'' +
                ", checkCertSha256=" + Arrays.toString(checkCertSha256) +
                '}';
    }

    //CHECKSTYLE:OFF
    public static class SignArgs implements Comparable<SignArgs> {
        public int index;
        public String ksFile;
        public String alias;
        public String pass;
        public String keyPass;

        SignArgs(int index, String ksFile, String alias, String pass, String keyPass) {
            this.index = index;
            this.ksFile = ksFile;
            this.alias = alias;
            this.pass = pass;
            this.keyPass = keyPass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SignArgs signArgs = (SignArgs) o;

            if (index != signArgs.index) return false;
            if (ksFile != null ? !ksFile.equals(signArgs.ksFile) : signArgs.ksFile != null) return false;
            if (alias != null ? !alias.equals(signArgs.alias) : signArgs.alias != null) return false;
            if (pass != null ? !pass.equals(signArgs.pass) : signArgs.pass != null) return false;
            return keyPass != null ? keyPass.equals(signArgs.keyPass) : signArgs.keyPass == null;

        }

        @Override
        public int hashCode() {
            int result = index;
            result = 31 * result + (ksFile != null ? ksFile.hashCode() : 0);
            result = 31 * result + (alias != null ? alias.hashCode() : 0);
            result = 31 * result + (pass != null ? pass.hashCode() : 0);
            result = 31 * result + (keyPass != null ? keyPass.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "SignArgs{" +
                    "index=" + index +
                    ", ksFile='" + ksFile + '\'' +
                    ", alias='" + alias + '\'' +
                    ", pass='" + pass + '\'' +
                    ", keyPass='" + keyPass + '\'' +
                    '}';
        }

        @Override
        public int compareTo(SignArgs o) {
            return Integer.valueOf(index).compareTo(o.index);
        }
    }
    //CHECKSTYLE:ON
}
