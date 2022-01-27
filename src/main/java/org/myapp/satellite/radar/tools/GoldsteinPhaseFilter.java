package org.myapp.satellite.radar.tools;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
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

public class GoldsteinPhaseFilter {

    public static void main(String[] args) {
        String outputDir, filesList, taskId, resultDir = "";

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            outputDir = consoleParameters.get("outputDir").toString();
            resultDir = consoleParameters.get("resultDir").toString();
            filesList = consoleParameters.get("filesList").toString();
            taskId = consoleParameters.get("taskId").toString();

            String configDir = resultDir + File.separator + taskId + File.separator + "config";
            String graphDir = resultDir + File.separator + taskId + File.separator + "graphs";

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                throw new Exception("GoldsteinPhaseFilter: Fail to read parameters.");
            }

            String taskDir = outputDir + File.separator + taskId;

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            String goldsteinphasefilteringTaskDir = taskDir + "" + File.separator + "goldsteinphasefiltering";
            if (Files.exists(Paths.get(goldsteinphasefilteringTaskDir))) {
                Common.deleteDir(new File(goldsteinphasefilteringTaskDir));
            }
            new File(goldsteinphasefilteringTaskDir).mkdirs();

            String goldsteinphasefilteringResultDir = resultDir + File.separator + taskId + File.separator + "public" + File.separator + "goldsteinphasefiltering";
            if (Files.exists(Paths.get(goldsteinphasefilteringResultDir))) {
                Common.deleteDir(new File(goldsteinphasefilteringResultDir));
            }
            new File(goldsteinphasefilteringResultDir).mkdirs();

            String graphFile = "goldstein_phase_filtering.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

            // GoldsteinPhaseFiltering
            ((HashMap) parameters.get("GoldsteinPhaseFiltering")).forEach((key, value) -> {
                graph.getNode("GoldsteinPhaseFiltering").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });


            for (int i = 0; i < files.length; i++) {
                String productDate = Paths.get(files[i]).getFileName().toString().replace(Common.OperationPrefix.INTERFEROGRAM, "").replace(".dim", "");

                String targetFile = goldsteinphasefilteringTaskDir + File.separator + productDate + Common.OperationPrefix.GOLDSTEIN_PHASE_FILTERING;
                String targetGraphFile = goldsteinphasefilteringTaskDir + File.separator + productDate + Common.OperationPrefix.GOLDSTEIN_PHASE_FILTERING;
                String subsetTargetFile = goldsteinphasefilteringTaskDir + File.separator + productDate + Common.OperationPrefix.GOLDSTEIN_PHASE_FILTERING + Common.OperationPrefix.SUBSET;
                String subsetImgFile1 = goldsteinphasefilteringResultDir + File.separator + productDate + Common.OperationPrefix.GOLDSTEIN_PHASE_FILTERING + Common.OperationPrefix.SUBSET;
                String subsetImgFile2 = goldsteinphasefilteringResultDir + File.separator + productDate + Common.OperationPrefix.COHERENCE + Common.OperationPrefix.SUBSET;

                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(subsetTargetFile + ".dim");
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file").setValue(targetFile + ".dim");

                FileWriter fileWriter = new FileWriter(targetGraphFile + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                Common.runGPTScript(targetGraphFile + ".xml", "goldsteinphasefiltering");

                Product product = ProductIO.readProduct(subsetTargetFile + ".dim");
                Band sourceBand = (Band) Arrays.stream(product.getBands())
                        .filter(band -> band.getName().toLowerCase().contains("phase"))
                        .toArray()[0];
                Common.exportProductToImg(sourceBand, 0.3f, 0.7f, new File(subsetImgFile1 + ".jpg"), "JPG", true);
                sourceBand = (Band) Arrays.stream(product.getBands())
                        .filter(band -> band.getName().toLowerCase().contains("coh"))
                        .toArray()[0];
                Common.exportProductToImg(sourceBand, 0.3f, 0.7f, new File(subsetImgFile2 + ".jpg"), "JPG", false);
                product.closeIO();

                Files.deleteIfExists(Paths.get(subsetTargetFile + ".dim"));
                Common.deleteDir(new File(subsetTargetFile + ".data"));
            }


        } catch (Exception ex) {
            //TODO: delete
            ex.printStackTrace();
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;
        JSONParser parser;
        FileReader fileReader;
        JSONObject jsonObject;
        HashMap jsonParameters;
        HashMap<String, HashMap> jsonParameters1;
        HashMap parameters;

        try {
            stageParameters = new HashMap<>();
            parser = new JSONParser();

            // Subset
            fileReader = new FileReader(configDir + File.separator + "subset.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            parameters = new HashMap();
            parameters.put("geoRegion", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

            // GoldsteinPhaseFiltering
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "goldstein_phase_filtering.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("GoldsteinPhaseFiltering", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}

