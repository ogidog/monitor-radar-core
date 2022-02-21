package org.myapp.satellite.radar.tools;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.satellite.radar.shared.TOPSARSplitOpEnv;
import org.myapp.utils.Common;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackGeocoding {

    public static void main(String[] args) {


        String resultDir = "";

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String tasksDir = consoleParameters.get("tasksDir").toString();
            String resultsDir = consoleParameters.get("resultsDir").toString();
            String username = consoleParameters.get("username").toString();
            String filesList = consoleParameters.get("filesList").toString();
            String taskId = consoleParameters.get("taskId").toString();

            String configDir = resultsDir + File.separator + username + File.separator + taskId + File.separator + "config";
            String graphDir = resultsDir + File.separator + username + File.separator + taskId + File.separator + "graphs";
            resultDir = resultsDir + File.separator + username + File.separator + taskId;
            String taskDir = tasksDir + File.separator + username + File.separator + taskId;

            if (Common.checkPreviousErrors(resultDir)) {
                return;
            }
            Common.writeStatus(resultDir, Common.TaskStatus.PROCESSING, "");

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                throw new Exception("BackGeocoding: Fail to read parameters.");
            }

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            String backgeocodingTaskDir = taskDir + "" + File.separator + "back_geocoding";
            if (Files.exists(Paths.get(backgeocodingTaskDir))) {
                Common.deleteDir(new File(backgeocodingTaskDir));
            }
            new File(backgeocodingTaskDir).mkdirs();

            String backgeocodingResultDir = resultDir + File.separator + username + File.separator + taskId + File.separator + "public" + File.separator + "back_geocoding";
            if (Files.exists(Paths.get(backgeocodingResultDir))) {
                Common.deleteDir(new File(backgeocodingResultDir));
            }
            new File(backgeocodingResultDir).mkdirs();

            String graphFile = "back_geocoding.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // BackGeocoding
            ((HashMap) parameters.get("BackGeocoding")).forEach((key, value) -> {
                graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

            for (int i = 0; i < files.length / 2; i++) {

                Pattern p = Pattern.compile("\\d{8}");
                Matcher m = p.matcher(Paths.get(files[2 * i]).getFileName().toString());
                m.find();
                String masterProductDate = m.group();
                m = p.matcher(Paths.get(files[2 * i + 1]).getFileName().toString());
                m.find();
                String slaveProductDate = m.group();

                String productDate = masterProductDate + "_" + slaveProductDate;

                String targetFile = backgeocodingTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING;
                String targetGraphFile = backgeocodingTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING;
                String subsetTargetFile = backgeocodingTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING + Common.OperationPrefix.SUBSET;
                String subsetImgFile = backgeocodingResultDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING;

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

                Common.runGPTScript(targetGraphFile + ".xml", "BackGeocoding");

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

                Common.writeStatus(resultDir, Common.TaskStatus.COMPLETED, "");
            }

        } catch (Exception ex) {
            Common.writeStatus(resultDir, Common.TaskStatus.ERROR, ex.getMessage());

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
