package at.favre.tools.apksigner.ui;


public class Arg {
    public String apkFile;

    public boolean debugSign;

    public String ksFile;
    public String ksPass;
    public String ksKeyPass;
    public String ksAliasName;

    public boolean overwrite = false;
    public boolean dryRun = false;
    public boolean verbose = false;
    public boolean skipZipAlign = false;
    public boolean debug = false;
    public boolean onlyVerify = false;

    public String zipAlignPath;

    public Arg() {
    }

    public Arg(String apkFile, boolean debugSign, String ksFile, String ksPass, String ksKeyPass, String ksAliasName,
               boolean overwrite, boolean dryRun, boolean verbose, boolean skipZipAlign, boolean debug, boolean onlyVerify, String zipAlignPath) {
        this.apkFile = apkFile;
        this.debugSign = debugSign;
        this.ksFile = ksFile;
        this.ksPass = ksPass;
        this.ksKeyPass = ksKeyPass;
        this.ksAliasName = ksAliasName;
        this.overwrite = overwrite;
        this.dryRun = dryRun;
        this.verbose = verbose;
        this.skipZipAlign = skipZipAlign;
        this.debug = debug;
        this.onlyVerify = onlyVerify;
        this.zipAlignPath = zipAlignPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Arg arg = (Arg) o;

        if (debugSign != arg.debugSign) return false;
        if (overwrite != arg.overwrite) return false;
        if (dryRun != arg.dryRun) return false;
        if (verbose != arg.verbose) return false;
        if (skipZipAlign != arg.skipZipAlign) return false;
        if (debug != arg.debug) return false;
        if (onlyVerify != arg.onlyVerify) return false;
        if (apkFile != null ? !apkFile.equals(arg.apkFile) : arg.apkFile != null) return false;
        if (ksFile != null ? !ksFile.equals(arg.ksFile) : arg.ksFile != null) return false;
        if (ksPass != null ? !ksPass.equals(arg.ksPass) : arg.ksPass != null) return false;
        if (ksKeyPass != null ? !ksKeyPass.equals(arg.ksKeyPass) : arg.ksKeyPass != null) return false;
        if (ksAliasName != null ? !ksAliasName.equals(arg.ksAliasName) : arg.ksAliasName != null) return false;
        return zipAlignPath != null ? zipAlignPath.equals(arg.zipAlignPath) : arg.zipAlignPath == null;

    }

    @Override
    public int hashCode() {
        int result = apkFile != null ? apkFile.hashCode() : 0;
        result = 31 * result + (debugSign ? 1 : 0);
        result = 31 * result + (ksFile != null ? ksFile.hashCode() : 0);
        result = 31 * result + (ksPass != null ? ksPass.hashCode() : 0);
        result = 31 * result + (ksKeyPass != null ? ksKeyPass.hashCode() : 0);
        result = 31 * result + (ksAliasName != null ? ksAliasName.hashCode() : 0);
        result = 31 * result + (overwrite ? 1 : 0);
        result = 31 * result + (dryRun ? 1 : 0);
        result = 31 * result + (verbose ? 1 : 0);
        result = 31 * result + (skipZipAlign ? 1 : 0);
        result = 31 * result + (debug ? 1 : 0);
        result = 31 * result + (onlyVerify ? 1 : 0);
        result = 31 * result + (zipAlignPath != null ? zipAlignPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Arg{" +
                "apkFile='" + apkFile + '\'' +
                ", debugSign=" + debugSign +
                ", ksFile='" + ksFile + '\'' +
                ", ksPass='" + ksPass + '\'' +
                ", ksKeyPass='" + ksKeyPass + '\'' +
                ", ksAliasName='" + ksAliasName + '\'' +
                ", overwrite=" + overwrite +
                ", dryRun=" + dryRun +
                ", verbose=" + verbose +
                ", skipZipAlign=" + skipZipAlign +
                ", debug=" + debug +
                ", onlyVerify=" + onlyVerify +
                ", zipAlignPath='" + zipAlignPath + '\'' +
                '}';
    }
}
