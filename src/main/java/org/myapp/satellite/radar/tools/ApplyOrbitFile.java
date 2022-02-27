package org.myapp.satellite.radar.tools;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.satellite.radar.common.TOPSARSplitOpEnv;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ApplyOrbitFile {

    public static void main(String[] args) {

        String operationResultDir = "";

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String tasksDir = consoleParameters.get("tasksDir").toString();
            String resultsDir = consoleParameters.get("resultsDir").toString();
            String filesList = consoleParameters.get("filesList").toString();
            String username = consoleParameters.get("username").toString();
            String taskId = consoleParameters.get("taskId").toString();

            HashMap parameters = Common.getParameters(Common.getConfigDir(resultsDir, username, taskId), new String[]{
                    Common.OperationName.APPLY_ORBIT_FILE, Common.OperationName.DATABASE, Common.OperationName.S1_TOPS_SPLIT,
                    Common.OperationName.DATABASE, Common.OperationName.SUBSET
            });

            String operationTaskDir = Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.APPLY_ORBIT_FILE);
            if (Files.exists(Paths.get(operationTaskDir))) {
                Common.deleteDir(new File(operationTaskDir));
            }
            new File(operationTaskDir).mkdirs();

            operationResultDir = Common.getOperationResultDir(resultsDir, username, taskId, Common.OperationName.APPLY_ORBIT_FILE);
            if (Files.exists(Paths.get(operationResultDir))) {
                Common.deleteDir(new File(operationResultDir));
            }
            new File(operationResultDir).mkdirs();

            if (Common.checkPreviousErrors(operationResultDir)) {
                Common.deletePreviousErrors(operationResultDir);
            }
            Common.writeStatus(operationResultDir, Common.TaskStatus.PROCESSING, "");

            // Set graph
            Graph graph = Common.readGraphFile(Common.getGraphFile(resultsDir, username, taskId, Common.OperationName.APPLY_ORBIT_FILE));
            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get(Common.OperationName.SUBSET)).get("geoRegion").toString() + "))");

            TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
            String[] files = Common.getFiles(filesList);
            Pattern p = Pattern.compile("\\d{8}");
            for (int i = 0; i < files.length; i++) {

                Matcher m = p.matcher(files[i]);
                m.find();
                String productDate = m.group();

                String targetFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE;
                String targetGraphFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE;
                String subsetTargetFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE + Common.OperationPrefix.SUBSET;
                String subsetImgFile = operationResultDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE;

                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(subsetTargetFile + ".dim");

                topsarSplitOpEnv.getSplitParameters(files[i], parameters);

                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(targetFile + ".dim");
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topsarSplitOpEnv.getSubSwath());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topsarSplitOpEnv.getFirstBurstIndex());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topsarSplitOpEnv.getLastBurstIndex());

                FileWriter fileWriter = new FileWriter(targetGraphFile + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                Common.runGPTScript(targetGraphFile + ".xml", Common.OperationName.APPLY_ORBIT_FILE);

                Product product = ProductIO.readProduct(subsetTargetFile + ".dim");
                Band sourceBand = (Band) Arrays.stream(product.getBands())
                        .filter(band -> band.getName().toLowerCase().contains("intensity"))
                        .toArray()[0];
                Common.exportProductToImg(sourceBand, 0.3f, 0.7f, new File(subsetImgFile + ".jpg"), "JPG", false);
                product.closeIO();

                Files.deleteIfExists(Paths.get(subsetTargetFile + ".dim"));
                Common.deleteDir(new File(subsetTargetFile + ".data"));
            }
            topsarSplitOpEnv.Dispose();

            Common.writeStatus(operationResultDir, Common.TaskStatus.COMPLETED, "");

        } catch (Exception e) {
            Common.writeStatus(operationResultDir, Common.TaskStatus.ERROR, e.getMessage());

            // TODO: delete
            e.printStackTrace();
        }
    }
}
