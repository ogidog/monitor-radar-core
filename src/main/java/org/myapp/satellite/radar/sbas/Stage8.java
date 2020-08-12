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
            cmdWriter.println("docker run -it -v " + prepDir + ":/home/work/ ogidog/mintpy:latest python /home/python/MintPy/mintpy/smallbaselineApp.py /home/work/smallbaselineApp.cfg");
            cmdWriter.println("docker run -it -v " + prepDir + ":/home/work/ ogidog/mintpy:latest python /home/python/MintPy/mintpy/save_qgis.py -g /home/work/inputs/geometryGeo.h5 -o /home/work/velocity.shp /home/work/timeseries.h5");
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
