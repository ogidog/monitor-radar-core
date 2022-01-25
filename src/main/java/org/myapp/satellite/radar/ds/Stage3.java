package org.myapp.satellite.radar.ds;

import org.myapp.utils.Common;

import java.io.File;

public class Stage3 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        Common.runScript("/opt/ds_proc/bin/Stage3.sh", taskDir, "Stage3");

    }

}
