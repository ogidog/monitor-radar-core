package org.myapp.satellite.radar.processing;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

public class Stage3 {

    public static void main(String[] args) {

        /*inputDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\subset" outputDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\topophaseremoval"  graphsDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\graphs" configDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\config" snapDir="F:\intellij-idea-workspace\monitor-radar-core-v3\.snap"*/

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String inputDir = consoleParameters.get("inputDir").toString();
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphsDir = consoleParameters.get("graphsDir").toString();
        String configDir = consoleParameters.get("configDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();

        int numOfProcesses = 3;

        try {

            String[] sourceProducts = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                return Character.isDigit(path.toString().charAt(path.toString().length() - 1));
            }).map(path -> path.toString()).toArray(String[]::new);

            String tmpDir = new File("").getAbsolutePath();

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

            FileWriter fileWriter = new FileWriter(tmpDir + File.separator + "TopoPhaseRemoval.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            for (int index = 0; index < sourceProducts.length; index += numOfProcesses) {
                String[] subsetProductFiles = Arrays.stream(sourceProducts).skip(index).limit(numOfProcesses).toArray(String[]::new);
                runGraphParallel(subsetProductFiles, tmpDir, snapDir, index);
            }

            Files.deleteIfExists(Paths.get(tmpDir + File.separator + "TopoPhaseRemoval.xml"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void runGraphParallel(String[] subsetProductFiles, String tmpDir, String snapDir, int sourceProductIndex) {

        IntStream.range(0, subsetProductFiles.length).parallel().forEach(index -> {
            try {

                Reader fileReader1 = new FileReader(tmpDir + File.separator + "TopoPhaseRemoval.xml");
                Graph graph1 = GraphIO.read(fileReader1);
                fileReader1.close();

                graph1.getNode("Read").getConfiguration().getChild("file").setValue(subsetProductFiles[index] + File.separator + "subset_master_Stack_Deb.dim");
                graph1.getNode("Write").getConfiguration().getChild("file").setValue(subsetProductFiles[index].replace("subset", "topophaseremoval") + File.separator + "subset_master_Stack_Deb_ifg_dinsar.dim");

                FileWriter fileWriter1 = new FileWriter(tmpDir + File.separator + "TopoPhaseRemoval" + (index) + ".xml");
                GraphIO.write(graph1, fileWriter1);
                fileWriter1.flush();
                fileWriter1.close();

                ProcessBuilder processBuilder = new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt" +
                        (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""),
                        tmpDir + File.separator + "TopoPhaseRemoval" + (index) + ".xml", "-Dsnap.userdir=" + snapDir
                ).inheritIO();
                Process p = processBuilder.start();
                p.waitFor();
                p.destroy();

                Files.deleteIfExists(Paths.get(tmpDir + File.separator + "TopoPhaseRemoval" + (index) + ".xml"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void _main(String[] args) {

        // String inputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\subset";
        // String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\topophaseremoval";
        // String graphsDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\graphs";
        // String configDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\config";

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String inputDir = consoleParameters.get("inputDir").toString();
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphsDir = consoleParameters.get("graphsDir").toString();
        String configDir = consoleParameters.get("configDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();

        try {

            String[] filePatchesDirs = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                return Character.isDigit(path.toString().charAt(path.toString().length() - 1));
            }).map(path -> path.toString()).sorted().toArray(String[]::new);

            String tmpDir = new File("").getAbsolutePath();

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

            FileWriter fileWriter = new FileWriter(tmpDir + File.separator + "TopoPhaseRemoval.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            IntStream.range(0, filePatchesDirs.length).forEach(index -> {
                new File(outputDir + File.separator + (index + 1)).mkdirs();

                try {

                    Reader fileReader1 = new FileReader(tmpDir + File.separator + "TopoPhaseRemoval.xml");
                    Graph graph1 = GraphIO.read(fileReader1);
                    fileReader1.close();

                    graph1.getNode("Read").getConfiguration().getChild("file").setValue(filePatchesDirs[index] + File.separator + "subset_master_Stack_Deb.dim");
                    graph1.getNode("Write").getConfiguration().getChild("file").setValue(outputDir + File.separator + (index + 1) + File.separator + "subset_master_Stack_Deb_ifg_dinsar.dim");

                    FileWriter fileWriter1 = new FileWriter(tmpDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml");
                    GraphIO.write(graph1, fileWriter1);
                    fileWriter1.flush();
                    fileWriter1.close();

                    ProcessBuilder processBuilder =
                            new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt" +
                                    (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""),
                                    tmpDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml", "-Dsnap.userdir=" + snapDir
                            ).inheritIO();
                    Process p = processBuilder.start();
                    p.waitFor();
                    p.destroy();

                    Files.deleteIfExists(Paths.get(tmpDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Files.deleteIfExists(Paths.get(tmpDir + File.separator + "TopoPhaseRemoval.xml"));

        } catch (Exception e) {
            e.printStackTrace();
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
