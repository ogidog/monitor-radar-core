package org.myapp.satellite.radar.tools;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.satellite.radar.shared.TOPSARSplitOpEnv;
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

public class ApplyOrbitFiles {

    public static void main(String[] args) {

        String resultDir = "";

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String tasksDir = consoleParameters.get("tasksDir").toString();
            String resultsDir = consoleParameters.get("resultsDir").toString();
            String filesList = consoleParameters.get("filesList").toString();
            String username = consoleParameters.get("username").toString();
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
                throw new Exception("ApplyOrbitFile: Fail to read parameters.");
            }

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            String applyorbitfileTaskDir = taskDir + "" + File.separator + "apply_orbit_file";
            if (Files.exists(Paths.get(applyorbitfileTaskDir))) {
                Common.deleteDir(new File(applyorbitfileTaskDir));
            }
            new File(applyorbitfileTaskDir).mkdirs();

            String applyorbitfileResultDir = resultsDir + File.separator + "public" + File.separator + "apply_orbit_file";
            if (Files.exists(Paths.get(applyorbitfileResultDir))) {
                Common.deleteDir(new File(applyorbitfileResultDir));
            }
            new File(applyorbitfileResultDir).mkdirs();

            TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
            String graphFile = "apply_orbit_file.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

            Pattern p = Pattern.compile("\\d{8}");

            for (int i = 0; i < files.length; i++) {

                Matcher m = p.matcher(files[i]);
                m.find();
                String productDate = m.group();

                String targetFile = applyorbitfileTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE;
                String targetGraphFile = applyorbitfileTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE;
                String subsetTargetFile = applyorbitfileTaskDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE + Common.OperationPrefix.SUBSET;
                String subsetImgFile = applyorbitfileResultDir + File.separator + productDate + Common.OperationPrefix.APPLY_ORBIT_FILE;

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

                Common.runGPTScript(targetGraphFile + ".xml", "ApplyOrbitFile");

                Product product = ProductIO.readProduct(subsetTargetFile + ".dim");
                VirtualBand sourceBand = (VirtualBand) Arrays.stream(product.getBands())
                        .filter(band -> band.getName().toLowerCase().contains("intensity"))
                        .toArray()[0];
                Common.exportProductToImg(sourceBand, 0.3f, 0.7f, new File(subsetImgFile + ".jpg"), "JPG", false);
                product.closeIO();

                Files.deleteIfExists(Paths.get(subsetTargetFile + ".dim"));
                Common.deleteDir(new File(subsetTargetFile + ".data"));
            }

            topsarSplitOpEnv.Dispose();

            Common.writeStatus(resultDir, Common.TaskStatus.COMPLETED, "");

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
