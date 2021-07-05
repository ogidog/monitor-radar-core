package org.myapp.satellite.radar.NDAI;

import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.CustomErrorHandler;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {

        String outputDir, configDir, graphDir, filesList, taskId, resultDir = "";
        int firstStep, lastStep;

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            outputDir = consoleParameters.get("outputDir").toString();
            configDir = consoleParameters.get("configDir").toString();
            graphDir = consoleParameters.get("graphDir").toString();
            filesList = consoleParameters.get("filesList").toString();
            firstStep = Integer.valueOf(consoleParameters.get("firstStep").toString());
            lastStep = Integer.valueOf(consoleParameters.get("lastStep").toString());
            taskId = consoleParameters.get("taskId").toString();

            resultDir = Paths.get(configDir).getParent().toString();

            if (CustomErrorHandler.checkPreviousErrors(resultDir)) {
                return;
            }

            if (firstStep <= 1 && lastStep >= 1) {
                Stage1.process(outputDir, configDir, graphDir, filesList, taskId);
            }
            if (firstStep <= 2 && lastStep >= 2) {
                Stage2.process(outputDir, configDir, graphDir, taskId);
            }
            if (firstStep <= 3 && lastStep >= 3) {
                Stage3.process(outputDir, configDir, graphDir, taskId);
            }
            if (firstStep <= 4 && lastStep >= 4) {
                Stage4.process(outputDir, graphDir, taskId);
            }
            if (firstStep <= 5 && lastStep >= 5) {
                Stage5.process(outputDir, graphDir, taskId);
            }
            if (firstStep <= 6 && lastStep >= 6) {
                Stage6.process(outputDir, configDir, graphDir, taskId);
            }

        } catch (Exception e) {
            CustomErrorHandler.writeErrorToFile(e.getMessage(), resultDir + File.separator + "ERROR");

            // TODO: убрать
            e.printStackTrace();
        }

    }
}
