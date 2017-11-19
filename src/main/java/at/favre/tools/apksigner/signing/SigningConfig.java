package at.favre.tools.apksigner.signing;

import at.favre.tools.apksigner.util.FileUtil;

import java.io.File;

/**
 * Model for defining a signing config
 */
public class SigningConfig {
    public enum KeystoreLocation {
        DEBUG_ANDROID_FOLDER, DEBUG_SAME_FOLDER, DEBUG_EMBEDDED, DEBUG_CUSTOM_LOCATION, RELEASE_CUSTOM
    }

    public final KeystoreLocation location;
    public final int configIndex;
    public final boolean isDebugType;
    public final File keystore;
    public final String ksAlias;
    public final String ksPass;
    public final String ksKeyPass;

    public SigningConfig(KeystoreLocation location, int configIndex, boolean isDebugType, File keystore, String ksAlias, String ksPass, String ksKeyPass) {
        this.location = location;
        this.configIndex = configIndex;
        this.isDebugType = isDebugType;
        this.keystore = keystore;
        this.ksAlias = ksAlias;
        this.ksPass = ksPass;
        this.ksKeyPass = ksKeyPass;
    }

    public String description() throws Exception {
        String checksum = FileUtil.createChecksum(keystore, "SHA-256");
        String desc = "[" + configIndex + "] " + checksum.substring(0, 8) + " " + keystore.getCanonicalPath() + " (" + location + ")";
        return desc;
    }
}
