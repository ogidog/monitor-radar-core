
package org.myapp.satellite.radar.squeesar;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Stage2 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String filesList = consoleParameters.get("filesList").toString();

            String[] files = Files.walk(Paths.get(filesList)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);


            String topophaseremovalDir = outputDir + "" + File.separator + "topophaseremoval_squeesar";
            if (Files.exists(Paths.get(topophaseremovalDir))) {
                Files.walk(Paths.get(topophaseremovalDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String stage2Dir = outputDir + "" + File.separator + "stage2";
            if (Files.exists(Paths.get(stage2Dir))) {

                Files.walk(Paths.get(stage2Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(topophaseremovalDir).mkdirs();
            new File(stage2Dir).mkdirs();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String graphFile = "stamps_prep_squeesar.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Interferogram
            ((HashMap) parameters.get("Interferogram")).forEach((key, value) -> {
                graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            // TopoPhaseRemoval
            ((HashMap) parameters.get("TopoPhaseRemoval")).forEach((key, value) -> {
                graph.getNode("TopoPhaseRemoval").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });


            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");

            for (int i = 0; i < files.length; i++) {
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(topophaseremovalDir + File.separator + Paths.get(files[i]).getFileName().toString()
                                .replace(".dim", "_Ifg_DInSAR.dim"));
                FileWriter fileWriter = new FileWriter(stage2Dir + File.separator + Paths.get(files[i]).getFileName().toString()
                        .replace(".dim", "_Ifg_DInSAR.xml"));
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage2Dir + File.separator + Paths.get(files[i]).getFileName().toString()
                        .replace(".dim", "_Ifg_DInSAR.xml"));

            }

            cmdWriter.close();

            return;

        } catch (
                Exception e) {
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

            // BackGeocoding
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "back_geocoding.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("BackGeocoding", parameters);
            fileReader.close();

            // Interferogram Formation
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "interferogram_formation.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("Interferogram", parameters);
            fileReader.close();

            // Topo Phase Removal
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "topo_phase_removal.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("TopoPhaseRemoval", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
