package org.myapp.satellite.radar.processing;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

public class Stage3 {

    public static void main(String[] args) {

        String inputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\subset";
        String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\topophaseremoval";
        String graphsDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\graphs";
        String configDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\config";

        try {

            String[] filePatchesDirs = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                return Character.isDigit(path.toString().charAt(path.toString().length() - 1));
            }).map(path -> path.toString()).toArray(String[]::new);

            HashMap stageParameters = getParameters(configDir);

            Reader fileReader = new FileReader(graphsDir + File.separator + "TopoPhaseRemoval.xml");
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            HashMap interferogramParameters = (HashMap) stageParameters.get("Interferogram");
            Iterator it = interferogramParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                graph.getNode("Interferogram").getConfiguration().getChild(pair.getKey().toString()).setValue(pair.getValue().toString());
            }
            HashMap topoPhaseRemovalParameters = (HashMap) stageParameters.get("TopoPhaseRemoval");
            it = topoPhaseRemovalParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                graph.getNode("TopoPhaseRemoval").getConfiguration().getChild(pair.getKey().toString()).setValue(pair.getValue().toString());
            }

            FileWriter fileWriter = new FileWriter(graphsDir + File.separator + "TopoPhaseRemoval.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            IntStream.range(0, filePatchesDirs.length).parallel().forEach(index -> {
                new File(outputDir + File.separator + (index + 1)).mkdirs();

                try {

                    Reader fileReader1 = new FileReader(graphsDir + File.separator + "TopoPhaseRemoval.xml");
                    Graph graph1 = GraphIO.read(fileReader1);
                    fileReader1.close();

                    graph1.getNode("Read").getConfiguration().getChild("file").setValue(filePatchesDirs[index] + File.separator + "subset_master_Stack_Deb.dim");
                    graph1.getNode("Write").getConfiguration().getChild("file").setValue(outputDir + File.separator + (index + 1) + File.separator + "subset_master_Stack_Deb_ifg_dinsar.dim");

                    FileWriter fileWriter1 = new FileWriter(graphsDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml");
                    GraphIO.write(graph1, fileWriter1);
                    fileWriter1.flush();
                    fileWriter1.close();

                    ProcessBuilder processBuilder =
                            new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt.exe ",
                                    graphsDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml").inheritIO();
                    Process p = processBuilder.start();
                    p.waitFor();
                    p.destroy();

                    Files.deleteIfExists(Paths.get(graphsDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml"));

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });

            return;

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static HashMap getParameters(String configDir) {

        try {

            HashMap<String, HashMap> stageParameters = new HashMap<>();

            JSONParser parser = new JSONParser();

            // Interferogram
            FileReader fileReader = new FileReader(configDir + File.separator + "interferogram_formation.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("Interferogram", parameters);
            fileReader.close();

            // TopoPhaseRemoval
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
            System.out.println(e.getMessage());
            return null;
        }
    }
}
