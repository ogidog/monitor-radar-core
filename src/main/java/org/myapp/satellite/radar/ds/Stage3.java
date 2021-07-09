package org.myapp.satellite.radar.ds;

import org.myapp.utils.Routines;

import java.io.File;

public class Stage3 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        Routines.runScript("/opt/ds_proc/bin/Stage3.sh", taskDir, "Stage3");

    }

}
