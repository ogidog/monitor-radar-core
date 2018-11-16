package org.myapp.satellite.radar.processing;

import java.io.File;

public class Stage0 {

    public static void main(String[] args) {
        // param1
        String processingDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\";

        new File(processingDir + File.separator + "processing").mkdirs();
        new File(processingDir + File.separator + "processing" + File.separator + "applyorbitfile").mkdirs();
        new File(processingDir + File.separator + "processing" + File.separator +  "stampsexport").mkdirs();
        new File(processingDir + File.separator + "processing" + File.separator +  "subset").mkdirs();
        new File(processingDir + File.separator + "processing" + File.separator +  "topophaseremoval").mkdirs();

    }
}
