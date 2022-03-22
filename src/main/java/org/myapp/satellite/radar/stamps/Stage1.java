
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
import java.util.stream.Collectors;


public class Stage1 {

    public static void process(String outputDir, String configDir, String graphDir, String filesList, String taskId) throws Exception {

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            throw new Exception("Stage1: Fail to read parameters.");
        }

        String taskDir = outputDir + File.separator + taskId;

        String[] files;
        if (!filesList.contains(",")) {
            files = Files.walk(Paths.get(filesList)).skip(1)
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
        } else {
            files = filesList.split(",");
        }

        if (Files.exists(Paths.get(taskDir))) {
            Common.deleteDir(new File(taskDir));
        }
        new File(taskDir).mkdirs();
        String stage1Dir = taskDir + "" + File.separator + "stage1";
        new File(taskDir + File.separator + "applyorbitfile").mkdirs();
        new File(stage1Dir).mkdirs();

        TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
        String graphFile = "applyorbitfile.xml";
        FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();


        for (int i = 0; i < files.length; i++) {

            topsarSplitOpEnv.getSplitParameters(files[i], parameters);

            graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(taskDir + File.separator + "applyorbitfile" + File.separator
                            + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + "_Orb.dim");
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topsarSplitOpEnv.getSubSwath());
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topsarSplitOpEnv.getFirstBurstIndex());
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topsarSplitOpEnv.getLastBurstIndex());

            FileWriter fileWriter = new FileWriter(stage1Dir + File.separator
                    + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            Common.runGPTScript(stage1Dir + File.separator
                    + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml","Stage1");
        }

        topsarSplitOpEnv.Dispose();

    }
}
