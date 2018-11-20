package org.myapp.satellite.radar.processing;
import java.io.File;

public class Stage0 {

    public static void main(String[] args) {

        String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing";

        new File(outputDir).mkdirs();
        new File(outputDir + File.separator + "applyorbitfile").mkdirs();
        new File(outputDir + File.separator + "stampsexport").mkdirs();
        new File(outputDir + File.separator + "subset").mkdirs();
        new File(outputDir + File.separator + "topophaseremoval").mkdirs();

    }
}
