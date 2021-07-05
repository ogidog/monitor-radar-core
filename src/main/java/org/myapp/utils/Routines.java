package org.myapp.utils;

import java.io.File;
import java.nio.file.Files;
import java.util.Locale;

public class Routines {

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    public static String getGPTScriptName() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows")) {
            return "gpt.exe";
        } else {
            return "/usr/local/snap/bin/gpt";
        }
    }

}
