package at.favre.tools.apksigner.ui;

import at.favre.tools.apksigner.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses and checks the file input argument
 */
public class FileArgParser {

    public List<File> parseAndSortUniqueFilesNonRecursive(String[] files, String extensionFilter) {
        if (files == null) {
            throw new IllegalArgumentException("input files must not be null");
        }

        if (files.length == 0) {
            return Collections.emptyList();
        }

        Set<File> fileSet = new HashSet<>();

        for (String file : files) {
            File apkFile = new File(file);

            if (apkFile.exists() && apkFile.isDirectory()) {
                for (File dirFile : apkFile.listFiles()) {
                    if (isCorrectFile(dirFile, extensionFilter)) {
                        fileSet.add(dirFile);
                    }
                }
            } else if (isCorrectFile(apkFile, extensionFilter)) {
                fileSet.add(apkFile);
            } else {
                throw new IllegalArgumentException("provided apk path or file '" + file + "' does not exist");
            }
        }

        List<File> resultList = new ArrayList<>(fileSet);
        Collections.sort(resultList);
        return resultList;
    }

    public static List<String> getDirSummary(List<File> files) {
        Set<File> parents = new HashSet<>();
        for (File file : files) {
            if (file.isDirectory()) {
                parents.add(file);
            } else {
                try {
                    file = new File(file.getCanonicalPath());
                    parents.add(file.getParentFile());
                } catch (IOException e) {
                    throw new IllegalStateException("could not add parent folder", e);
                }
            }
        }

        return parents.stream().map(f -> {
            try {
                return f.getCanonicalPath();
            } catch (IOException e) {
                throw new IllegalStateException("could not get dir summary", e);
            }
        }).sorted().collect(Collectors.toList());
    }

    private static boolean isCorrectFile(File f, String extensionFilter) {
        if (f != null && f.exists() && f.isFile()) {
            return FileUtil.getFileExtension(f).equalsIgnoreCase(extensionFilter);
        }
        return false;
    }
}
