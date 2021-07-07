package org.myapp.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    public static void runGPTScript(String graphFile, String stageName) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(getGPTScriptName(), graphFile);
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException(stageName + ": execution of GPT script failed");
        }
    }

    public static void runScript(String scriptFile, String taskDir, String stageName) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(scriptFile, taskDir);
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException(stageName + " : execution of GPT script failed");
        }
    }

    public static void writeStatus(String taskDir, String status, String message) {
        try {
            PrintWriter pr;
            switch (status) {
                case "done":
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "processing"));
                    pr = new PrintWriter(taskDir + File.separator + status);
                    pr.print("");
                    pr.flush();
                    pr.close();
                    break;

                case "processing":
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "done"));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "error"));
                    pr = new PrintWriter(taskDir + File.separator + status);
                    pr.print("");
                    pr.flush();
                    pr.close();
                    break;

                case "error":
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "done"));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "processing"));
                    pr = new PrintWriter(taskDir + File.separator + status);
                    pr.print(message);
                    pr.flush();
                    pr.close();
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkPreviousErrors(String resultDir) {
        if (Files.exists(Paths.get(resultDir + File.separator + "error"))) {
            return true;
        } else {
            return false;
        }
    }
}
