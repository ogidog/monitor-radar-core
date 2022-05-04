package org.myapp.satellite.radar.stamps;

import org.myapp.utils.Common;

import java.io.File;
import java.nio.file.*;

public class Stage5 {

    public static void process(String tasksDir, String userId, String taskId) throws Exception {

        String operationTaskDir = Common.getOperationTaskDir(tasksDir, userId, taskId, Common.OperationName.STAMPS_STAGE3) + File.separator + "geo";
        Path latFilePath = Paths.get(Common.getFiles(Common.getOperationTaskDir(tasksDir, userId, taskId, Common.OperationName.STAMPS_STAGE2), ".lat.img")[0]);
        Path lonFilePath = Paths.get(Common.getFiles(Common.getOperationTaskDir(tasksDir, userId, taskId, Common.OperationName.STAMPS_STAGE2), ".lon.img")[0]);

        Files.copy(latFilePath, Paths.get(operationTaskDir + File.separator + latFilePath.getFileName().toString().replace(".img", "")), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(lonFilePath, Paths.get(operationTaskDir + File.separator + lonFilePath.getFileName().toString().replace(".img", "")), StandardCopyOption.REPLACE_EXISTING);

    }

}
