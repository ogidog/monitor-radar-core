package org.myapp.satellite.radar.msbas;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Stage7 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String stage7Dir = outputDir + "" + File.separator + "stage7";
            String snaphuimportDir = outputDir + File.separator + "snaphuimport";
            String phase2dispDir = outputDir + File.separator + "phase2disp";

            String[] files;
            files = Files.walk(Paths.get(snaphuimportDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            if (Files.exists(Paths.get(phase2dispDir))) {
                Files.walk(Paths.get(phase2dispDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(stage7Dir))) {
                Files.walk(Paths.get(stage7Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(phase2dispDir).mkdirs();
            new File(stage7Dir).mkdirs();

            String graphFile = "phase2disp.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage7Dir + File.separator + "stage7.cmd", "UTF-8");
            for (int i = 0; i < files.length; i++) {
                String productName = Paths.get(files[i]).getFileName().toString();
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(phase2dispDir + File.separator + productName.replace(".dim",".dsp.dim"));

                FileWriter fileWriter = new FileWriter(stage7Dir + File.separator + productName.replace(".dim", ".dsp.xml"));
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage7Dir + File.separator + productName.replace(".dim", ".dsp.xml"));
            }

            cmdWriter.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            JSONParser parser = new JSONParser();
            stageParameters = new HashMap<>();

            // Subset
            FileReader fileReader = new FileReader(configDir + File.separator + "subset.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            HashMap parameters = new HashMap();
            parameters.put("geoRegion", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);
            fileReader.close();

            // Interferogram Formation
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "interferogram_formation.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("Interferogram", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
