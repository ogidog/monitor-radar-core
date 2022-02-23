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
                    Common.OperationName.APPLY_ORBIT_FILE, Common.OperationName.DATASET, Common.OperationName.S1_TOPS_SPLIT,
                    Common.OperationName.DATASET, Common.OperationName.SUBSET
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
            Graph graph = Common.readGraphFile(Common.getGraphDir(resultsDir, username, taskId) + File.separator + Common.OperationName.APPLY_ORBIT_FILE + ".xml");
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

    static HashMap getParameters1(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // DataSet
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "dataset.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("DataSet",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            // TOPSARSplit
            fileReader = new FileReader(configDir + File.separator + "s1_tops_split.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("TOPSARSplit",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            fileReader.close();

            // ApplyOrbitFile
            fileReader = new FileReader(configDir + File.separator + "apply_orbit_file.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            parameters.put("polyDegree", Integer.valueOf(((HashMap) jsonParameters.get("polyDegree")).get("value").toString()));
            parameters.put("continueOnFail", Boolean.valueOf(((HashMap) jsonParameters.get("continueOnFail")).get("value").toString()));
            parameters.put("orbitType", ((HashMap) jsonParameters.get("orbitType")).get("value"));
            stageParameters.put("ApplyOrbitFile", parameters);

            fileReader.close();

            // Subset
            fileReader = new FileReader(configDir + File.separator + "subset.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            parameters = new HashMap();
            parameters.put("geoRegion", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }
}
