package at.favre.tools.apksigner.signing;

import java.io.File;

public class SigningConfig {
    public enum KeystoreLocation {DEBUG_ANDROID_FOLDER, DEBUG_SAME_FOLDER, DEBUG_EMBEDDED, RELEASE_CUSTOM}

    public final KeystoreLocation location;
    public final boolean isDebugType;
    public final File keystore;
    public final String ksAlias;
    public final String ksPass;
    public final String ksKeyPass;

    public SigningConfig(KeystoreLocation location, boolean isDebugType, File keystore, String ksAlias, String ksPass, String ksKeyPass) {
        this.location = location;
        this.isDebugType = isDebugType;
        this.keystore = keystore;
        this.ksAlias = ksAlias;
        this.ksPass = ksPass;
        this.ksKeyPass = ksKeyPass;
    }
}
