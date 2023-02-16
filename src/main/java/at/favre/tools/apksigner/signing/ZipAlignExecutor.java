package at.favre.tools.apksigner.signing;

import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.util.CmdUtil;
import at.favre.tools.apksigner.util.FileUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

/**
 * Responsible for deciding and finding the zipalign executable used by the tool.
 */
public class ZipAlignExecutor {
    private enum Location {
        CUSTOM, PATH, BUILT_IN
    }

    public static final String ZIPALIGN_NAME = "zipalign";

    private String[] zipAlignExecutable;
    private Location location;
    private File tmpFolder;

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

                    String zipAlignFileName, libFolder;
                    List<String> libFiles = new ArrayList<>();
                    if (osType == CmdUtil.OS.WIN) {
                        zipAlignFileName = "win-zipalign_33_0_2.exe";
                        libFolder = "binary-lib/windows-33_0_2/";
                        libFiles.add(libFolder + "libwinpthread-1.dll");
                    } else if (osType == CmdUtil.OS.MAC) {
                        zipAlignFileName = "mac-zipalign-33_0_2";
                    } else {
                        zipAlignFileName = "linux-zipalign-33_0_2";
                        libFolder = "binary-lib/linux-lib64-33_0_2/";
                        libFiles.add(libFolder + "libc++.so");
                    }

                    tmpFolder = Files.createTempDirectory("uapksigner-").toFile();
                    File tmpZipAlign = File.createTempFile(zipAlignFileName, null, tmpFolder);
                    Files.copy(
                            Objects.requireNonNull(
                                    getClass().getClassLoader().getResourceAsStream(zipAlignFileName),
                                    "could not load built-in zipalign " + zipAlignFileName
                            ),
                            tmpZipAlign.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    if (osType != CmdUtil.OS.WIN) {
                        Set<PosixFilePermission> perms = new HashSet<>();
                        perms.add(PosixFilePermission.OWNER_EXECUTE);

                        Files.setPosixFilePermissions(tmpZipAlign.toPath(), perms);

                        File libFolderFile = new File(tmpFolder, "lib64");
                        if (!libFolderFile.mkdirs()) {
                            throw new IllegalStateException("could not create " + libFolderFile);
                        }

                        for (String libFile : libFiles) {
                            File lib64File = new File(libFolderFile, new File(libFile).getName());

                            if (!lib64File.createNewFile()) {
                                throw new IllegalStateException("could not create " + lib64File);
                            }

                            Files.setPosixFilePermissions(lib64File.toPath(), perms);

                            Files.copy(
                                    Objects.requireNonNull(
                                            getClass().getClassLoader().getResourceAsStream(libFile),
                                            "could not load built-in lib file " + libFile
                                    ),
                                    lib64File.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                    } else {
                        for (String libFile : libFiles) {
                            System.out.println(libFile);
                            System.out.println(tmpFolder);

                            Files.copy(
                                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(libFile), "could not load lib file " + libFile),
                                    new File(tmpFolder, new File(libFile).getName()).toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                        }

                    }

                    zipAlignExecutable = new String[]{tmpZipAlign.getAbsolutePath()};
                    location = Location.BUILT_IN;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not find location for zipalign. Try to set it in PATH or use the --zipAlignPath argument. Optionally you could skip zipalign with --skipZipAlign. " + e.getMessage(), e);
        }
    }

    public boolean isExecutableFound() {
        return zipAlignExecutable != null;
    }

    public void cleanUp() {
        if (tmpFolder != null) {
            FileUtil.removeRecursive(tmpFolder.toPath());
            tmpFolder = null;
        }
    }

    public String[] getZipAlignExecutable() {
        return zipAlignExecutable;
    }

    @Override
    public String toString() {
        return "zipalign location: " + location + " \n\t" + zipAlignExecutable[0];
    }
}
