
package org.myapp.satellite.radar.sbas;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Graph1;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Stage2 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String stage2Dir = outputDir + "" + File.separator + "stage2";
            String intfDir = outputDir + File.separator + "intf";
            String applyorbitfileDir = outputDir + File.separator + "applyorbitfile";

            String[] files;
            files = Files.walk(Paths.get(applyorbitfileDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            Product[] products = Arrays.stream(files).map(file -> {
                try {
                    return ProductIO.readProduct(file);
                } catch (Exception e) {
                    return null;
                }
            }).toArray(Product[]::new);

            if (Files.exists(Paths.get(intfDir))) {
                Files.walk(Paths.get(intfDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(intfDir).mkdirs();

            if (Files.exists(Paths.get(stage2Dir))) {
                Files.walk(Paths.get(stage2Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage2Dir).mkdirs();

            ArrayList<String[]> pairs = new ArrayList<>();
            String content = new String(Files.readAllBytes(Paths.get(configDir + File.separator + "selectNetwork.template")));

            if (content.contains("delaunay")) {
                InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(products);

                List<String> configLines = Files.readAllLines(Paths.get(configDir + File.separator + "selectNetwork.template"));

                int bPerpMax = Integer.valueOf(configLines.stream().filter(line -> line.contains("perpBaseMax")).toArray(String[]::new)[0].split("=")[1].trim());
                int bTempMax = Integer.valueOf(configLines.stream().filter(line -> line.contains("tempBaseMax")).toArray(String[]::new)[0].split("=")[1].trim());
                int currBPerp = 40, currBTemp = 45;

                Graph1 network = null;
                boolean visited[] = new boolean[products.length];
                boolean isFullyConnectedGraph = false;

                int i1, j1 = 0;
                for (i1 = currBPerp; i1 < bPerpMax; i1++) {
                    if (isFullyConnectedGraph) {
                        break;
                    }
                    for (j1 = currBTemp; j1 < bTempMax; j1++) {
                        if (isFullyConnectedGraph) {
                            break;
                        }
                        network = new Graph1(products.length);
                        for (int i = 0; i < stackOverview.length; i++) {
                            InSARStackOverview.IfgPair[] masterSlavePairs = stackOverview[i].getMasterSlave();
                            for (int j = i + 1; j < masterSlavePairs.length; j++) {
                                if (Math.abs(masterSlavePairs[j].getPerpendicularBaseline()) <= i1 &&
                                        Math.abs(masterSlavePairs[j].getTemporalBaseline()) <= j1) {
                                    network.addEdge(i, j);
                                }
                            }
                        }

                        Arrays.fill(visited, false);
                        visited[0] = true;
                        visited = network.DFS(0);
                        isFullyConnectedGraph = true;
                        for (boolean item : visited) {
                            if (!item) {
                                isFullyConnectedGraph = false;
                                break;
                            }
                        }
                    }
                }

                if (isFullyConnectedGraph) {
                    System.out.println("Fully connected graph with bPerp = " + i1 + ", bTemp = " + j1);

                    String[] productNames = new String[products.length];
                    for (int i = 0; i < products.length; i++) {
                        productNames[i] = products[i].getName();
                    }

                    Iterator<LinkedList<Integer>> masterIter = Arrays.stream(network.adj).iterator();
                    int k = 0;
                    while (masterIter.hasNext()) {
                        String masterName = productNames[k];
                        Iterator<Integer> slaveIter = masterIter.next().iterator();
                        while (slaveIter.hasNext()) {
                            String slaveName = productNames[slaveIter.next()];
                            pairs.add(new String[]{masterName, slaveName});
                        }
                        k += 1;
                    }

                } else {
                    System.out.println("Not fully connected graph. You should increase bPerpMax and/or bTempMax parameters");
                    return;
                }
            }

            if (content.contains("sequential")) {
                for (int i = 0; i < products.length - 3; i++) {
                    for (int j = i + 1; j < i + 3; j++) {
                        pairs.add(new String[]{products[i].getName(), products[j].getName()});
                    }
                }
            }

            String graphFile = "filt_intf.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

            // BackGeocoding
            ((HashMap) parameters.get("BackGeocoding")).forEach((key, value) -> {
                graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

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
            String masterProductDate, slaveProductDate, masterProductName, slaveProductName;

            for (String[] pair : pairs) {
                masterProductDate = pair[0].split(" ")[0].split("T")[0].split("_")[5];
                slaveProductDate = pair[1].split(" ")[0].split("T")[0].split("_")[5];

                graph.getNode("Read").getConfiguration().getChild("file").setValue(applyorbitfileDir + File.separator + pair[0] + ".dim");
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(applyorbitfileDir + File.separator + pair[1] + ".dim");
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(intfDir + File.separator + masterProductDate + "_" + slaveProductDate + ".dim");

                FileWriter fileWriter = new FileWriter(stage2Dir + File.separator
                        + masterProductDate + "_" + slaveProductDate + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage2Dir + File.separator + masterProductDate + "_" + slaveProductDate + ".xml");
            }
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
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
