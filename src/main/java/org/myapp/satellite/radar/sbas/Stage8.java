package org.myapp.satellite.radar.sbas;

import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

public class Stage8 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String dockerOutputDir = "/mnt/output";

            String prepDir = outputDir + File.separator + "prep";
            String stage8Dir = outputDir + "" + File.separator + "stage8";

            if (Files.exists(Paths.get(stage8Dir))) {
                Files.walk(Paths.get(stage8Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage8Dir).mkdirs();

            PrintWriter cmdWriter = new PrintWriter(stage8Dir + File.separator + "stage8.cmd", "UTF-8");
            cmdWriter.println("cd " + prepDir);
            cmdWriter.println("docker run --entrypoint python -v " + prepDir + ":" + dockerOutputDir + " -it mintpy_prep:1.0 /home/python/MintPy/mintpy/smallbaselineApp.py " + dockerOutputDir + "/smallbaselineApp.cfg");
            cmdWriter.println("docker run --entrypoint python -v " + prepDir + ":" + dockerOutputDir + " -it mintpy_prep:1.0 /home/python/MintPy/mintpy/save_qgis.py -g "
                    + dockerOutputDir + "/inputs/geometryGeo.h5 -o " + dockerOutputDir + "/velocity.shp " + dockerOutputDir + "/timeseries.h5");
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
