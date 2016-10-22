package at.favre.tools.apksigner;

import at.favre.tools.apksigner.ui.Arg;

import java.io.File;

public class SigingConfigEngine {

    private static String WIN_DEBUG_KS_DEFAULT = "%USERPROFILE%\\.android\\android.keystore";
    private static String NIX_DEBUG_KS_DEFAULT = "~/.android/debug.keystore";

    public SigningConfig generate(Arg arguments) {
        if (arguments.debugSign) {
            if (arguments.ksFile != null) {
                throw new IllegalArgumentException("debugSign param was given, but still ks location was passed");
            }

            File debugKeystore = null;
            SigningConfig.KeystoreLocation location = SigningConfig.KeystoreLocation.DEBUG_EMBEDDED;
            if (new File(WIN_DEBUG_KS_DEFAULT).exists()) {
                debugKeystore = new File(WIN_DEBUG_KS_DEFAULT);
                location = SigningConfig.KeystoreLocation.DEBUG_ANDROID_FOLDER;
            } else if (new File(NIX_DEBUG_KS_DEFAULT).exists()) {
                debugKeystore = new File(NIX_DEBUG_KS_DEFAULT);
                location = SigningConfig.KeystoreLocation.DEBUG_ANDROID_FOLDER;
            } else {
                try {
                    if (new File(SigingConfigEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "debug.keystore").exists()) {
                        debugKeystore = new File(SigingConfigEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "debug.keystore");
                        location = SigningConfig.KeystoreLocation.DEBUG_SAME_FOLDER;
                    }
                } catch (Exception e) {
                    //ignore

                }
            }

            if (debugKeystore == null) {
                //todo load embedded keystore
            }

            return new SigningConfig(
                    location,
                    debugKeystore,
                    "androiddebugkey",
                    "android",
                    "android"
            );
        } else {
            File keystore = new File(arguments.ksFile);

            if (arguments.ksFile == null || !keystore.exists() || keystore.isDirectory()) {
                throw new IllegalArgumentException("passed keystore does not exist: " + arguments.ksFile);
            }

            if (arguments.ksAliasName == null || arguments.ksAliasName.trim().isEmpty()) {
                throw new IllegalArgumentException("when you provide your own keystore you must pass the keystore alias name");
            }

            return new SigningConfig(
                    SigningConfig.KeystoreLocation.RELEASE_CUSTOM,
                    keystore,
                    arguments.ksAliasName,
                    arguments.ksPass,
                    arguments.ksKeyPass);
        }
    }
}
