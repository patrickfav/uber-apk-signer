package at.favre.tools.apksigner.util;

import java.io.File;

/**
 * Created by Christina on 16.10.2016.
 */
public class FileUtil {

    public static String getFileExtension(File file) {
        if (file == null) {
            return "";
        }
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }

    public static String getFileNameWithoutExtension(File file) {
        String fileName = file.getName();
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }
}
