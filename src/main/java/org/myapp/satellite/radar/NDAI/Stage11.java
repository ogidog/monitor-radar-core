package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Process1;
import org.myapp.utils.Routines;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Stage11 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String configDir = consoleParameters.get("configDir").toString();

            String stage11Dir = outputDir + "" + File.separator + "stage11";
            String tcFilteredAvgNDAIFile = outputDir + File.separator + "avgndai" + File.separator + "tcfilteredavgndai";
            String filteredAvgNDAIFile = outputDir + File.separator + "avgndai" + File.separator + "filteredavgndai.dim";

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            if (Files.exists(Paths.get(stage11Dir))) {
                Files.walk(Paths.get(stage11Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage11Dir).mkdirs();

            String graphFile = "tc.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Terrain Correction
            ((HashMap) parameters.get("TerrainCorrection")).forEach((key, value) -> {
                graph.getNode("Terrain-Correction").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            PrintWriter cmdWriter = new PrintWriter(stage11Dir + File.separator + "stage11.cmd", "UTF-8");
            graph.getNode("Read").getConfiguration().getChild("file").setValue(filteredAvgNDAIFile);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(tcFilteredAvgNDAIFile);
            FileWriter fileWriter = new FileWriter(stage11Dir + File.separator + "tcfilteredavgndai.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            cmdWriter.println("gpt " + stage11Dir + File.separator + "tcfilteredavgndai.xml");
            cmdWriter.close();

        } catch (Exception e) {
            Process1.writeErrorToFile(e.getMessage(), "/mnt/task" + File.separator + "ERROR");
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String configDir, String graphDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        String stage11Dir = taskDir + "" + File.separator + "stage11";
        String tcFilteredAvgNDAIFile = taskDir + File.separator + "avgndai" + File.separator + "tcfilteredavgndai";
        String filteredAvgNDAIFile = taskDir + File.separator + "avgndai" + File.separator + "filteredavgndai.dim";

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            System.out.println("Fail to read parameters.");
            return;
        }

        if (Files.exists(Paths.get(stage11Dir))) {
            Routines.deleteDir(new File(stage11Dir));
        }
        new File(stage11Dir).mkdirs();

        String graphFile = "tc.xml";
        FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        // Terrain Correction
        ((HashMap) parameters.get("TerrainCorrection")).forEach((key, value) -> {
            graph.getNode("Terrain-Correction").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        PrintWriter cmdWriter = new PrintWriter(stage11Dir + File.separator + "stage11.cmd", "UTF-8");
        graph.getNode("Read").getConfiguration().getChild("file").setValue(filteredAvgNDAIFile);
        graph.getNode("Write").getConfiguration().getChild("file")
                .setValue(tcFilteredAvgNDAIFile);
        FileWriter fileWriter = new FileWriter(stage11Dir + File.separator + "tcfilteredavgndai.xml");
        GraphIO.write(graph, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        Routines.runGPTScript(stage11Dir + File.separator + "tcfilteredavgndai.xml", "Stage11");
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            JSONParser parser = new JSONParser();
            stageParameters = new HashMap<>();

            // TerrainCorrection
            parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "terrain_correction.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("TerrainCorrection", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
