package org.myapp.satellite.radar.tools;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.Common;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackGeocoding {

    public static void main(String[] args) {


        String operationResultDir = "";

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String tasksDir = consoleParameters.get("tasksDir").toString();
            String resultsDir = consoleParameters.get("resultsDir").toString();
            String username = consoleParameters.get("username").toString();
            String filesList = consoleParameters.get("filesList").toString();
            String taskId = consoleParameters.get("taskId").toString();


            HashMap parameters = Common.getParameters(Common.getConfigDir(resultsDir, username, taskId), new String[]{
                    Common.OperationName.BACK_GEOCODING, Common.OperationName.SUBSET
            });

            String operationTaskDir = Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.BACK_GEOCODING);
            if (Files.exists(Paths.get(operationTaskDir))) {
                Common.deleteDir(new File(operationTaskDir));
            }
            new File(operationTaskDir).mkdirs();

            operationResultDir = Common.getOperationResultDir(resultsDir, username, taskId, Common.OperationName.BACK_GEOCODING);
            if (Files.exists(Paths.get(operationResultDir))) {
                Common.deleteDir(new File(operationResultDir));
            }
            new File(operationResultDir).mkdirs();

            if (Common.checkPreviousErrors(operationResultDir)) {
                Common.deletePreviousErrors(operationResultDir);
            }
            Common.writeStatus(operationResultDir, Common.TaskStatus.PROCESSING, "");

            // Set graph
            Graph graph = Common.readGraphFile(Common.getGraphFile(resultsDir, username, taskId, Common.OperationName.BACK_GEOCODING));
            // BackGeocoding
            ((HashMap) parameters.get(Common.OperationName.BACK_GEOCODING)).forEach((key, value) -> {
                graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });
            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get(Common.OperationName.SUBSET)).get("geoRegion").toString() + "))");

            String[] files = Common.getFiles(filesList);
            for (int i = 0; i < files.length / 2; i++) {

                Pattern p = Pattern.compile("\\d{8}");
                Matcher m = p.matcher(Paths.get(files[2 * i]).getFileName().toString());
                m.find();
                String masterProductDate = m.group();
                m = p.matcher(Paths.get(files[2 * i + 1]).getFileName().toString());
                m.find();
                String slaveProductDate = m.group();

                String productDate = masterProductDate + "_" + slaveProductDate;

                String targetFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING;
                String targetGraphFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING;
                String subsetTargetFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING + Common.OperationPrefix.SUBSET;
                String subsetImgFile = operationResultDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING;

                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(subsetTargetFile + ".dim");

                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[2 * i]);
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(files[2 * i + 1]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(targetFile + ".dim");

                FileWriter fileWriter = new FileWriter(targetGraphFile + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                Common.runGPTScript(targetGraphFile + ".xml", Common.OperationName.BACK_GEOCODING);

                Product product = ProductIO.readProduct(subsetTargetFile + ".dim");
                VirtualBand sourceBand = (VirtualBand) Arrays.stream(product.getBands())
                        .filter(band -> band.getName().toLowerCase().contains("intensity") && band.getName().toLowerCase().contains("mst"))
                        .toArray()[0];
                Common.exportProductToImg(sourceBand, 0.3f, 0.7f, new File(subsetImgFile + "_mst.jpg"), "JPG", false);
                sourceBand = (VirtualBand) Arrays.stream(product.getBands())
                        .filter(band -> band.getName().toLowerCase().contains("intensity") && band.getName().toLowerCase().contains("slv"))
                        .toArray()[0];
                Common.exportProductToImg(sourceBand, 0.3f, 0.7f, new File(subsetImgFile + "_slv.jpg"), "JPG", false);
                product.closeIO();

                Files.deleteIfExists(Paths.get(subsetTargetFile + ".dim"));
                Common.deleteDir(new File(subsetTargetFile + ".data"));

                Common.writeStatus(operationResultDir, Common.TaskStatus.COMPLETED, "");
            }

        } catch (Exception ex) {
            Common.writeStatus(operationResultDir, Common.TaskStatus.ERROR, ex.getMessage());

            // TODO: delete
            ex.printStackTrace();
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {

            stageParameters = new HashMap<>();

            // BackGeocoding
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "back_geocoding.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("BackGeocoding", parameters);
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
