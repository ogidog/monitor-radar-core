package org.myapp.satellite.radar.stamps;

import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Common;

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

            if (Common.checkPreviousErrors(taskDir)) {
                return;
            }

            Common.writeStatus(taskDir, Common.TaskStatus.PROCESSING, "");

            if (firstStep <= 1 && lastStep >= 1) {
                Stage1.process(outputDir, configDir, graphDir, filesList, taskId);
            }
            if (firstStep <= 2 && lastStep >= 2) {
                Stage2.process(outputDir, configDir, graphDir, taskId);
            }
            if (firstStep <= 3 && lastStep >= 3) {
                Stage3.process(outputDir, graphDir, taskId);
            }
            if (firstStep <= 4 && lastStep >= 4) {
                Stage4.process(outputDir, graphDir, taskId);
            }
            if (firstStep <= 5 && lastStep >= 5) {
                Stage5.process(outputDir, taskId);
            }
            if (firstStep <= 6 && lastStep >= 6) {
                Stage6.process(outputDir, taskId);
            }

            Common.writeStatus(taskDir, Common.TaskStatus.COMPLETED, "");

        } catch (Exception e) {
            Common.writeStatus(taskDir, Common.TaskStatus.ERROR, e.getMessage());

            // TODO: убрать
            e.printStackTrace();
        }

    }
}
