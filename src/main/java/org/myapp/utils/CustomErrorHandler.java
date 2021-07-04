package org.myapp.utils;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CustomErrorHandler {

    public static void writeErrorToFile(String message, String file) {
        try {

            PrintWriter pr = new PrintWriter(file);
            pr.print(message);
            pr.flush();
            pr.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkPreviousErrors() {
        if (Files.exists(Paths.get("/mnt/task/ERROR"))) {
            return true;
        } else {
            return false;
        }
    }
}
