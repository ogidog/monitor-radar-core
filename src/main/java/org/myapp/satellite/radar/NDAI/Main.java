package org.myapp.satellite.radar.NDAI;

import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.nio.file.Paths;
import java.util.HashMap;

public class Main extends Stage1 {

    public static void main(String[] args) {

        String outputDir, configDir, graphDir, filesList, taskId, taskDir = "", kmlObjectUrl;
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
            kmlObjectUrl = consoleParameters.get("kmlObjectUrl").toString();

            taskDir = Paths.get(configDir).getParent().toString();

            if (Routines.checkPreviousErrors(taskDir)) {
                return;
            }

            Routines.writeStatus(taskDir, Routines.TaskStatus.PROCESSING, "");

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
            if (firstStep <= 7 && lastStep >= 7) {
                Stage7.process(outputDir, taskId);
            }
            if (firstStep <= 8 && lastStep >= 8) {
                Stage8.process(outputDir, configDir, taskId);
            }
            if (firstStep <= 9 && lastStep >= 9) {
                Stage9.process(outputDir, graphDir, taskId);
            }
            if (firstStep <= 10 && lastStep >= 10) {
                Stage10.process(outputDir, taskId);
            }
            if (firstStep <= 11 && lastStep >= 11) {
                Stage11.process(outputDir, configDir, graphDir, taskId);
            }
            if (firstStep <= 12 && lastStep >= 12) {
                Stage12.process(outputDir, taskId);
            }
            if (firstStep <= 13 && lastStep >= 13) {
                Stage13.process(outputDir, configDir, kmlObjectUrl, taskId);
            }
            if (firstStep <= 14 && lastStep >= 14) {
                Stage14.process(outputDir, taskId);
            }

            Routines.writeStatus(taskDir, Routines.TaskStatus.COMPLETED, "");

        } catch (Exception e) {
            Routines.writeStatus(taskDir, Routines.TaskStatus.ERROR, e.getMessage());

            // TODO: убрать
            e.printStackTrace();
        }

    }
}
