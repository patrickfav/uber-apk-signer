package at.favre.tools.apksigner.ui;

import java.io.File;
import java.util.*;

/**
 * Parses and checks the file input argument
 */
public class FileArgParser {

    public List<File> parseAndSortUniqueFilesNonRecursive(String[] files) {
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
                Collections.addAll(fileSet, apkFile.listFiles());
            } else if (apkFile.exists()) {
                fileSet.add(apkFile);
            } else {
                throw new IllegalArgumentException("provided apk path or file '" + file + "' does not exist");
            }
        }

        List<File> resultList = new ArrayList<>(fileSet);
        Collections.sort(resultList);
        return resultList;
    }
}
