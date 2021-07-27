package org.myapp.satellite.radar.NDAI;

import org.myapp.utils.Routines;

import java.io.*;

public class Stage12 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        Routines.runScript("/opt/ndai_proc/bin/Stage12.sh", taskDir, "Stage12");

    }

}
