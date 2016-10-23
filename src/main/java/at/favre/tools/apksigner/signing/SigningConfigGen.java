package at.favre.tools.apksigner.signing;

import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.util.CmdUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SigningConfigGen {

    private static String WIN_DEBUG_KS_DEFAULT = "\\.android\\debug.keystore";
    private static String NIX_DEBUG_KS_DEFAULT = "~/.android/debug.keystore";
    private static String DEBUG_KEYSTORE = "debug.keystore";

    private File tempDebugFile;

    public final SigningConfig signingConfig;

    public SigningConfigGen(Arg arguments) {
        signingConfig = generate(arguments);
    }

    private SigningConfig generate(Arg arguments) {
        if (arguments.ksFile == null) {

            File debugKeystore = null;
            SigningConfig.KeystoreLocation location = SigningConfig.KeystoreLocation.DEBUG_EMBEDDED;
            CmdUtil.OS osType = CmdUtil.getOsType();

            try {
                File rootFolder = new File(SigningConfigGen.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
                File sameFolderStore = new File(rootFolder, DEBUG_KEYSTORE);
                if (sameFolderStore.exists()) {
                    debugKeystore = sameFolderStore;
                    location = SigningConfig.KeystoreLocation.DEBUG_SAME_FOLDER;
                }
            } catch (Exception e) {
            }

            if (debugKeystore == null) {
                if (osType == CmdUtil.OS.WIN) {
                    String userPath = System.getenv().get("USERPROFILE");
                    if (userPath != null) {
                        File userDebugKeystoreFile = new File(userPath, WIN_DEBUG_KS_DEFAULT);
                        if (userDebugKeystoreFile.exists()) {
                            debugKeystore = userDebugKeystoreFile;
                        }
                    }
                    location = SigningConfig.KeystoreLocation.DEBUG_ANDROID_FOLDER;
                } else if (new File(NIX_DEBUG_KS_DEFAULT).exists()) {
                    debugKeystore = new File(NIX_DEBUG_KS_DEFAULT);
                    location = SigningConfig.KeystoreLocation.DEBUG_ANDROID_FOLDER;
                }
            }

            if (debugKeystore == null) {
                try {
                    File embeddedDebugKeystore = new File(getClass().getClassLoader().getResource(DEBUG_KEYSTORE).getFile());
                    tempDebugFile = File.createTempFile("temp_", "_" + embeddedDebugKeystore.getName());
                    Files.copy(embeddedDebugKeystore.toPath(), tempDebugFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    location = SigningConfig.KeystoreLocation.DEBUG_EMBEDDED;
                    debugKeystore = tempDebugFile;
                } catch (Exception e) {
                    throw new IllegalStateException("could not load embedded debug keystore: " + e.getMessage(), e);
                }
            }

            return new SigningConfig(
                    location,
                    true, debugKeystore,
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
                    false, keystore,
                    arguments.ksAliasName,
                    arguments.ksPass,
                    arguments.ksKeyPass);
        }
    }

    public void cleanUp() {
        if (tempDebugFile != null && tempDebugFile.exists()) {
            tempDebugFile.delete();
            tempDebugFile = null;
        }
    }
}
