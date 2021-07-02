package org.myapp.utils;

import java.io.PrintWriter;

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
}
