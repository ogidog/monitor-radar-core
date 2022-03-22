package org.myapp.satellite.radar.stamps;

import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.nio.file.*;
import java.util.HashMap;

public class Stage5 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String geoDir = outputDir + File.separator + "stampsexport" + File.separator + "geo";
            String topophaseremovalDir = outputDir + File.separator + "topophaseremoval";
            Path[] newLatLonfiles = Files.walk(Paths.get(topophaseremovalDir)).filter(path -> {
                if (path.toString().endsWith(".lat.img") || path.toString().endsWith(".lon.img")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath()).toArray(Path[]::new);

            Files.copy(newLatLonfiles[0], Paths.get(geoDir + File.separator + newLatLonfiles[0].getFileName().toString().replace(".img", "")), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newLatLonfiles[1], Paths.get(geoDir + File.separator + newLatLonfiles[1].getFileName().toString().replace(".img", "")), StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + File.separator + taskId;

        String geoDir = taskDir + File.separator + "stampsexport" + File.separator + "geo";
        String topophaseremovalDir = taskDir + File.separator + "topophaseremoval";
        Path[] newLatLonfiles = Files.walk(Paths.get(topophaseremovalDir)).filter(path -> {
            if (path.toString().endsWith(".lat.img") || path.toString().endsWith(".lon.img")) {
                return true;
            } else {
                return false;
            }
        }).map(path -> path.toAbsolutePath()).toArray(Path[]::new);

        Files.copy(newLatLonfiles[0], Paths.get(geoDir + File.separator + newLatLonfiles[0].getFileName().toString().replace(".img", "")), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(newLatLonfiles[1], Paths.get(geoDir + File.separator + newLatLonfiles[1].getFileName().toString().replace(".img", "")), StandardCopyOption.REPLACE_EXISTING);

    }

}
