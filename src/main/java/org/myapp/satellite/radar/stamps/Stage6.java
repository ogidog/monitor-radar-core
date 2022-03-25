package org.myapp.satellite.radar.stamps;

import org.myapp.utils.Common;

import java.io.File;

public class Stage6 {

    public static void process(String tasksDir, String username, String taskId) throws Exception {

        String operationTaskDir = Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE3);
        Common.runScript("/opt/stamps_proc/bin/Stage6.sh", operationTaskDir, Common.OperationName.STAMPS_STAGE6);

    }

}
