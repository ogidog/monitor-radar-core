package org.myapp.satellite.radar.ds;

import org.myapp.satellite.radar.NDAI.Stage1;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.nio.file.Paths;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {

        String outputDir, configDir, graphDir, filesList, taskId, taskDir = "";
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

            Routines.writeStatus(taskDir, Routines.TaskStatus.COMPLETED, "");

        } catch (Exception e) {
            Routines.writeStatus(taskDir, Routines.TaskStatus.ERROR, e.getMessage());

            // TODO: убрать
            e.printStackTrace();
        }

    }
}
