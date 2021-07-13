package org.myapp.satellite.radar.sbas;

import org.myapp.utils.Routines;

import java.io.File;

public class Stage10 {

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        Routines.runScript("/opt/mintpy_proc/bin/Stage10.sh", taskDir, "Stage10");

    }

}
