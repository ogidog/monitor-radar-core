package org.myapp.satellite.radar.processing;
import java.io.File;

public class Stage0 {

    public static void main(String[] args) {

        String processingDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing";

        new File(processingDir).mkdirs();
        new File(processingDir + File.separator + "applyorbitfile").mkdirs();
        new File(processingDir + File.separator + "stampsexport").mkdirs();
        new File(processingDir + File.separator + "subset").mkdirs();
        new File(processingDir + File.separator + "topophaseremoval").mkdirs();

    }
}
