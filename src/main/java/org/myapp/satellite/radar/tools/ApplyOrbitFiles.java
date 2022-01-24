package org.myapp.satellite.radar.tools;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.satellite.radar.shared.TOPSARSplitOpEnv;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ApplyOrbitFiles {

    public static void main(String[] args) {

        String outputDir, configDir, graphDir, filesList, taskId, resultDir = "";

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            outputDir = consoleParameters.get("outputDir").toString();
            configDir = consoleParameters.get("configDir").toString();
            graphDir = consoleParameters.get("graphDir").toString();
            filesList = consoleParameters.get("filesList").toString();
            resultDir = consoleParameters.get("resultDir").toString();
            taskId = consoleParameters.get("taskId").toString();

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

            String applyorbitfileTaskDir = taskDir + "" + File.separator + "applyorbitfile";
            if (Files.exists(Paths.get(applyorbitfileTaskDir))) {
                Routines.deleteDir(new File(applyorbitfileTaskDir));
            }
            new File(applyorbitfileTaskDir).mkdirs();

            String applyorbitfileResultDir = resultDir + "" + File.separator + "public" + File.separator + "applyorbitfile";
            if (Files.exists(Paths.get(applyorbitfileResultDir))) {
                Routines.deleteDir(new File(applyorbitfileResultDir));
            }
            new File(applyorbitfileResultDir).mkdirs();

            TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
            String graphFile = "applyorbitfile.xml";
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

                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(applyorbitfileResultDir + File.separator + productDate + "_sub.dim");

                topsarSplitOpEnv.getSplitParameters(files[i], parameters);

                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(taskDir + File.separator + "applyorbitfile" + File.separator
                                + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + "_Orb.dim");
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topsarSplitOpEnv.getSubSwath());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topsarSplitOpEnv.getFirstBurstIndex());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topsarSplitOpEnv.getLastBurstIndex());

                FileWriter fileWriter = new FileWriter(applyorbitfileTaskDir + File.separator
                        + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                Routines.runGPTScript(applyorbitfileTaskDir + File.separator
                        + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml", "applyorbitfile");
            }

            topsarSplitOpEnv.Dispose();

        } catch (Exception ex) {

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
