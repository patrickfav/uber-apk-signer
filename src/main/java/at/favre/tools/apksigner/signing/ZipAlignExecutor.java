package at.favre.tools.apksigner.signing;

import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.util.CmdUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ZipAlignExecutor {
    private enum Location {CUSTOM, PATH, BUILT_IN}

    public static final String ZIPALIGN_NAME = "zipalign";

    public String[] zipAlignExecutable;
    private Location location;
    private File tempLocation;

    public ZipAlignExecutor(Arg arg) {
        findLocation(arg);
    }

    private void findLocation(Arg arg) {
        try {
            if (arg.zipAlignPath != null && new File(arg.zipAlignPath).exists()) {
                File passedPath = new File(arg.zipAlignPath);
                if (passedPath.exists() && passedPath.isFile()) {
                    zipAlignExecutable = new String[]{new File(arg.zipAlignPath).getAbsolutePath()};
                    location = Location.CUSTOM;
                }
            } else {
                File pathFile = CmdUtil.checkAndGetFromPATHEnvVar(ZIPALIGN_NAME);

                if (pathFile != null) {
                    zipAlignExecutable = new String[]{pathFile.getAbsolutePath()};
                    location = Location.PATH;
                    return;
                }

                if (zipAlignExecutable == null) {
                    CmdUtil.OS osType = CmdUtil.getOsType();


                    String fileName;
                    if (osType == CmdUtil.OS.WIN) {
                        fileName = "win-zipalign-24_0_3.exe";
                    } else if (osType == CmdUtil.OS.MAC) {
                        fileName = "mac-zipalign-24_0_3";
                    } else {
                        fileName = "linux-zipalign-24_0_3";
                    }

                    File tempLocation = File.createTempFile("temp_", "_" + fileName);
                    Files.copy(getClass().getClassLoader().getResourceAsStream(fileName), tempLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    if (osType != CmdUtil.OS.WIN) {
                        CmdUtil.runCmd(new String[]{"chmod", "+x", tempLocation.getAbsolutePath()});
                    }

                    zipAlignExecutable = new String[]{tempLocation.getAbsolutePath()};
                    location = Location.BUILT_IN;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not find location for zipalign: " + e.getMessage(), e);
        }
    }

    public boolean isExecutableFound() {
        return zipAlignExecutable != null;
    }

    public void cleanUp() {
        if (tempLocation != null) {
            tempLocation.delete();
            tempLocation = null;
        }
    }

    @Override
    public String toString() {
        return "Using zipalign location " + location + ".";
    }
}
