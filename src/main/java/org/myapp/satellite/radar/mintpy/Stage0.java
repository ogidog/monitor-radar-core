package org.myapp.satellite.radar.mintpy;

import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.util.HashMap;

public class Stage0 {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String outputDir = consoleParameters.get("outputDir").toString();

        new File(outputDir).mkdirs();
        new File(outputDir + File.separator + "applyorbitfile").mkdirs();
        new File(outputDir + File.separator + "backgeocoding").mkdirs();
        new File(outputDir + File.separator + "esd").mkdirs();

    }
}
