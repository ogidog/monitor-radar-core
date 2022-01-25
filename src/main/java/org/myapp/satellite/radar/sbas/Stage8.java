package org.myapp.satellite.radar.sbas;

import org.myapp.utils.Common;

import java.io.File;

public class Stage8 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        Common.runScript("/opt/mintpy_proc/bin/Stage8.sh", taskDir, "Stage8");

    }

}
