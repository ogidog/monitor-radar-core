package org.myapp.satellite.radar.ps;

import org.myapp.utils.Routines;

import java.io.File;

public class Stage6 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        Routines.runScript("/opt/stamps_proc/bin/Stage6.sh", taskDir, "Stage6");

    }

}
