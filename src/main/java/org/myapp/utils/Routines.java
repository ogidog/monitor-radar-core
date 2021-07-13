package org.myapp.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Routines {

    public enum TaskStatus {

        COMPLETED("Completed"),
        ERROR("Error"),
        PROCESSING("Processing");

        public final String label;

        private TaskStatus(String label) {
            this.label = label;
        }
    }

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

    public static void runScript(String scriptFile, String param, String stageName) throws Exception {
        ProcessBuilder pb;
        pb = new ProcessBuilder(scriptFile, param);
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException(stageName + " : execution of script failed");
        }
    }

    public static void runScript(String[] command, String execDir, String stageName) throws Exception {
        ProcessBuilder pb;
        pb = new ProcessBuilder(command);
        pb.directory(new File(execDir));
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException(stageName + " : execution of script failed");
        }
    }

    public static void writeStatus(String taskDir, TaskStatus status, String message) {
        try {
            PrintWriter pr;
            switch (status) {
                case COMPLETED:
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("PROCESSING").label));
                    pr = new PrintWriter(taskDir + File.separator + TaskStatus.valueOf("COMPLETED").label);
                    pr.print("");
                    pr.flush();
                    pr.close();
                    break;

                case PROCESSING:
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("COMPLETED").label));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("ERROR").label));
                    pr = new PrintWriter(taskDir + File.separator + TaskStatus.valueOf("PROCESSING").label);
                    pr.print("");
                    pr.flush();
                    pr.close();
                    break;

                case ERROR:
                    Files.deleteIfExists(Paths.get(taskDir + TaskStatus.valueOf("COMPLETED").label));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("PROCESSING").label));
                    pr = new PrintWriter(taskDir + File.separator + TaskStatus.valueOf("ERROR").label);
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
        if (Files.exists(Paths.get(resultDir + File.separator + TaskStatus.valueOf("ERROR").label))) {
            return true;
        } else {
            return false;
        }
    }
}
