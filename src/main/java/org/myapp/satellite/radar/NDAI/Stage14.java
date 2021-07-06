package org.myapp.satellite.radar.NDAI;

import java.io.BufferedInputStream;
import java.io.File;

public class Stage14 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;

        ProcessBuilder pb = new ProcessBuilder("/opt/ndai_proc/bin/Stage14.sh", taskDir);
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException("Stage14: execution of GPT script failed");
        }
    }

}
