package org.myapp.satellite.radar.stamps;

import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Common;

import java.nio.file.Paths;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {

        String resultDir = "";

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String tasksDir = consoleParameters.get("tasksDir").toString();
            String resultsDir = consoleParameters.get("resultsDir").toString();
            String filesList = consoleParameters.get("filesList").toString();
            String userId = consoleParameters.get("userId").toString();
            String taskId = consoleParameters.get("taskId").toString();
            int firstStep = Integer.valueOf(consoleParameters.get("firstStep").toString());
            int lastStep = Integer.valueOf(consoleParameters.get("lastStep").toString());

            resultDir = Common.getResultDir(resultsDir, userId, taskId);

            if (Common.checkPreviousErrors(resultDir)) {
                Common.deletePreviousErrors(resultDir);
            }
            Common.writeStatus(resultDir, Common.TaskStatus.PROCESSING, "");

            if (firstStep <= 1 && lastStep >= 1) {
                Stage1.process(tasksDir, resultsDir, userId, taskId, filesList);
            }
            if (firstStep <= 2 && lastStep >= 2) {
                Stage2.process(tasksDir, resultsDir, userId, taskId);
            }
            if (firstStep <= 3 && lastStep >= 3) {
                Stage3.process(tasksDir, resultsDir, userId, taskId);
            }
            if (firstStep <= 4 && lastStep >= 4) {
                Stage4.process(tasksDir, resultsDir, userId, taskId);
            }
            if (firstStep <= 5 && lastStep >= 5) {
                Stage5.process(tasksDir, userId, taskId);
            }
            if (firstStep <= 6 && lastStep >= 6) {
                Stage6.process(tasksDir, resultsDir, userId, taskId);
            }

            Common.writeStatus(resultDir, Common.TaskStatus.COMPLETED, "");

        } catch (Exception e) {
            Common.writeStatus(resultDir, Common.TaskStatus.ERROR, e.getMessage());

            // TODO: убрать
            e.printStackTrace();
        }

    }
}
