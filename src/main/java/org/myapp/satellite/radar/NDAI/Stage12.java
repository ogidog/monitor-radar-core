package org.myapp.satellite.radar.NDAI;

import java.io.*;

public class Stage12 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;

        ProcessBuilder pb = new ProcessBuilder("/opt/ndai_proc/bin/Stage12.sh", taskDir);
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException("Stage12: execution of GPT script failed");
        }
    }

}
