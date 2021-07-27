package org.myapp.satellite.radar.NDAI;

import org.hsqldb.Routine;
import org.myapp.utils.Routines;

import java.io.BufferedInputStream;
import java.io.File;

public class Stage14 {

    public static void process(String outputDir, String taskId) throws Exception {
        String taskDir = outputDir + "" + File.separator + taskId;
        Routines.runScript("/opt/ndai_proc/bin/Stage14.sh", taskDir, "Stage14");
    }

}
