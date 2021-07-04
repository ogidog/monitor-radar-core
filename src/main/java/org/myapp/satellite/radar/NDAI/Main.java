package org.myapp.satellite.radar.NDAI;

import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.CustomErrorHandler;

import java.io.File;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {

        String outputDir, configDir, graphDir, filesList, taskid;
        int firstStep, lastStep;

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            outputDir = consoleParameters.get("outputDir").toString();
            configDir = consoleParameters.get("configDir").toString();
            graphDir = consoleParameters.get("graphDir").toString();
            filesList = consoleParameters.get("filesList").toString();
            firstStep = Integer.valueOf(consoleParameters.get("firstStep").toString());
            lastStep = Integer.valueOf(consoleParameters.get("lastStep").toString());
            taskid = consoleParameters.get("taskid").toString();

            if (firstStep <= 1 && lastStep >= 1) {
                Stage1.process(outputDir, configDir, graphDir, filesList, taskid);
            }

        } catch (Exception e) {
            CustomErrorHandler.writeErrorToFile(e.getMessage(), "/mnt/task" + File.separator + "ERROR");
            e.printStackTrace();
        }

    }
}
