package org.myapp.satellite.radar.ds;

import org.myapp.utils.Routines;

import java.io.File;

public class Stage8 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        Routines.runScript("/opt/ds_proc/bin/Stage8.sh", taskDir, "Stage8");

    }

}
