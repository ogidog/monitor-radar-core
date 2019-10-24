package org.myapp.satellite.radar.mintpy;

import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.util.HashMap;

public class Stage0 {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();

        new File(workingDir).mkdirs();
        new File(workingDir + File.separator + "applyorbitfile").mkdirs();
        new File(workingDir + File.separator + "backgeocoding").mkdirs();
        new File(workingDir + File.separator + "esd").mkdirs();

    }
}
