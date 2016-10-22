package at.favre.tools.apksigner;

public class AdbLocationFinder {
    public enum Location {PATH, PATH_WIN, CUSTOM, WIN_DEFAULT, MAC_DEFAULT, LINUX_DEFAULT}

    private static final String[] PATH_ADB = new String[]{"adb"};
    private static final String[] PATH_WIN_ADB = new String[]{"cmd", "/c", "adb"};

    private static final String[] WIN_DEFAULT = new String[]{"%USERPROFILE%\\AppData\\Local\\Android\\sdk\\platform-tools\\adb.exe"};
    private static final String[] WIN_DEFAULT2 = new String[]{"cmd", "/c", "%USERPROFILE%\\AppData\\Local\\Android\\sdk\\platform-tools\\adb.exe"};
    private static final String[] WIN_DEFAULT3 = new String[]{"%ANDROID_HOME%\\platform-tools\\adb.exe"};

    private static final String[] MAC_DEFAULT = new String[]{"/usr/local/opt/android-sdk/platform-tools/adb"};
    private static final String[] MAC_DEFAULT2 = new String[]{"~/Library/Android/sdk/platform-tools/adb"};
    private static final String[] MAC_DEFAULT3 = new String[]{"$ANDROID_HOME/platform-tools/adb"};

    private static final String[] LINUX_DEFAULT = new String[]{"/usr/local/opt/android-sdk/platform-tools/adb"};
    private static final String[] LINUX_DEFAULT2 = new String[]{"$ANDROID_HOME/platform-tools/adb"};


    LocationResult find(String customPath) {
        String osName = System.getProperty("os.name").toLowerCase();

        if (customPath != null && CmdUtil.canRunCmd(new String[]{customPath})) {
            return new LocationResult(Location.CUSTOM, new String[]{customPath});
        }

        if (CmdUtil.canRunCmd(PATH_ADB)) {
            return new LocationResult(Location.PATH, PATH_ADB);
        } else {
            if (osName.contains("win")) {
                if (CmdUtil.canRunCmd(PATH_WIN_ADB)) {
                    return new LocationResult(Location.PATH_WIN, PATH_WIN_ADB);
                }
                if (CmdUtil.canRunCmd(WIN_DEFAULT)) {
                    return new LocationResult(Location.WIN_DEFAULT, WIN_DEFAULT);
                }
                if (CmdUtil.canRunCmd(WIN_DEFAULT2)) {
                    return new LocationResult(Location.WIN_DEFAULT, WIN_DEFAULT2);
                }
                if (CmdUtil.canRunCmd(WIN_DEFAULT3)) {
                    return new LocationResult(Location.WIN_DEFAULT, WIN_DEFAULT3);
                }
            } else if (osName.contains("mac")) {
                if (CmdUtil.canRunCmd(MAC_DEFAULT)) {
                    return new LocationResult(Location.MAC_DEFAULT, MAC_DEFAULT);
                }
                if (CmdUtil.canRunCmd(MAC_DEFAULT2)) {
                    return new LocationResult(Location.MAC_DEFAULT, MAC_DEFAULT2);
                }
                if (CmdUtil.canRunCmd(MAC_DEFAULT3)) {
                    return new LocationResult(Location.MAC_DEFAULT, MAC_DEFAULT3);
                }
            } else if (osName.contains("nix")) {
                if (CmdUtil.canRunCmd(LINUX_DEFAULT)) {
                    return new LocationResult(Location.LINUX_DEFAULT, LINUX_DEFAULT);
                }
                if (CmdUtil.canRunCmd(LINUX_DEFAULT2)) {
                    return new LocationResult(Location.LINUX_DEFAULT, LINUX_DEFAULT2);
                }
            }
        }

        throw new IllegalStateException("Could not find adb. Not found in PATH or the usual default locations. Did you install " +
                "the Android SDK and set adb to PATH? See: http://stackoverflow.com/questions/20564514");
    }

    public static class LocationResult {
        public final Location location;
        public final String[] args;

        public LocationResult(Location location, String[] args) {
            this.location = location;
            this.args = args;
        }

        public String arg() {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                sb.append(arg + " ");
            }
            return sb.toString();
        }
    }
}
