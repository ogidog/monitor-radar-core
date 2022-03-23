
package org.myapp.satellite.radar.stamps;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.satellite.radar.common.TOPSARSplitOpEnv;
import org.myapp.utils.Common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Stage1 {

    public static void process(String tasksDir, String resultsDir, String username, String taskId, String filesList) throws Exception {

        HashMap parameters = Common.getParameters(Common.getConfigDir(resultsDir, username, taskId), new String[]{
                Common.OperationName.APPLY_ORBIT_FILE, Common.OperationName.S1_TOPS_SPLIT,
                Common.OperationName.DATABASE, Common.OperationName.SUBSET
        });

        String operationTaskDir = Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE1);
        if (Files.exists(Paths.get(operationTaskDir))) {
            Common.deleteDir(new File(operationTaskDir));
        }
        new File(operationTaskDir).mkdirs();

        // Set graph
        Graph graph = Common.readGraphFile(Common.getGraphFile(resultsDir, username, taskId, Common.OperationName.APPLY_ORBIT_FILE));

        String[] files = Common.getFiles(filesList);
        TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();

        Pattern p = Pattern.compile("\\d{8}");
        for (int i = 0; i < files.length; i++) {

            Matcher m = p.matcher(files[i]);
            m.find();
            String productDate = m.group();

            String targetFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE + ".dim";
            String targetGraphFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE + ".xml";

            topsarSplitOpEnv.getSplitParameters(files[i], parameters);

            graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
            graph.getNode("Write").getConfiguration().getChild("file").setValue(targetFile);
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topsarSplitOpEnv.getSubSwath());
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topsarSplitOpEnv.getFirstBurstIndex());
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topsarSplitOpEnv.getLastBurstIndex());

            FileWriter fileWriter = new FileWriter(targetGraphFile);
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            Common.runGPTScript(targetGraphFile, Common.OperationName.STAMPS_STAGE1);
        }

        topsarSplitOpEnv.Dispose();

    }
}
