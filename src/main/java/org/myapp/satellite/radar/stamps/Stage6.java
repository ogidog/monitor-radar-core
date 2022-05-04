package org.myapp.satellite.radar.stamps;

import org.myapp.utils.Common;

import java.io.File;

public class Stage6 {

    public static void process(String tasksDir, String resultsDir, String userId, String taskId) throws Exception {

        String operationTaskDir = Common.getOperationTaskDir(tasksDir, userId, taskId, Common.OperationName.STAMPS_STAGE3);
        String resultDir = Common.getResultDir(resultsDir, userId, taskId);
        String[] command = {"/opt/stamps-proc/bin/Stage6.sh", operationTaskDir, resultDir};
        Common.runScript(command, operationTaskDir, Common.OperationName.STAMPS_STAGE6);

    }

}
