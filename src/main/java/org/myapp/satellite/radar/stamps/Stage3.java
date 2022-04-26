
package org.myapp.satellite.radar.stamps;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.Common;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Stage3 {

    public static void process(String tasksDir, String resultsDir, String username, String taskId) throws Exception {

        String operationTaskDir = Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE3);
        if (Files.exists(Paths.get(operationTaskDir))) {
            Common.deleteDir(new File(operationTaskDir));
        }
        new File(operationTaskDir).mkdirs();

        String[] files1 = Common.getFiles(Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE2), "_cor.dim");
        String[] files2 = Common.getFiles(Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE2), "_topo.dim");

        for (int i = 0; i < files1.length; i++) {
            Path path = Paths.get(files1[i]);
            Charset charset = StandardCharsets.UTF_8;
            String content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll("<NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>",
                    "<NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
            Files.write(path, content.getBytes(charset));

            path = Paths.get(files2[i]);
            content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll("<NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>",
                    "<NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
            Files.write(path, content.getBytes(charset));
        }

        Graph graph = Common.readGraphFile(Common.getGraphFile(resultsDir, username, taskId, Common.OperationName.STAMPS_EXPORT));

        Pattern p = Pattern.compile("\\d{8}_\\d{8}");
        for (int i = 0; i < files1.length; i++) {
            Matcher m = p.matcher(files1[i]);
            m.find();
            String file1Name = m.group();

            graph.getNode("Read").getConfiguration().getChild("file").setValue(files1[i]);
            for (int j = 0; j < files2.length; j++) {
                m = p.matcher(files2[j]);
                m.find();
                String file2Name = m.group();

                if (file1Name.equals(file2Name)) {
                    graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(files2[j]);
                    break;
                }
            }
            graph.getNode("StampsExport").getConfiguration().getChild("targetFolder").setValue(operationTaskDir);

            String targetGraphFile = operationTaskDir + File.separator + file1Name + Common.OperationPrefix.STAMPS_EXPORT + ".xml";
            FileWriter fileWriter = new FileWriter(targetGraphFile);
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            Common.runGPTScript(targetGraphFile, Common.OperationName.STAMPS_STAGE3);

        }
    }
}
