package org.myapp.satellite.radar.msbas;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.sentinel1.gpf.BackGeocodingOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Graph1;

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
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String stage2Dir = outputDir + "" + File.separator + "stage2";
            String backgeocodingDir = outputDir + File.separator + "backgeocoding";
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

            if (Files.exists(Paths.get(backgeocodingDir))) {
                Files.walk(Paths.get(backgeocodingDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(backgeocodingDir).mkdirs();


            if (Files.exists(Paths.get(stage2Dir))) {
                Files.walk(Paths.get(stage2Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage2Dir).mkdirs();

            OperatorSpi spi;
            BackGeocodingOp op;
            Product targetProduct;
            float modelledCoherenceThreshold = 0.93f;

            String[] productNames = new String[products.length];
            for (int i = 0; i < products.length; i++) {
                productNames[i] = products[i].getName();
            }

            boolean visited[] = new boolean[products.length];
            Arrays.fill(visited, false);
            visited[0] = true;
            visited[1] = true;
            Graph1 network = null;

            Product[] sourceProducts = new Product[2];
            ArrayList<String[]> pairs = new ArrayList<>();
            //while (!verifyAllEqual(visited)) {
            //    network = new Graph1(products.length);
                for (int i = 0; i < products.length-3; i++) {
                    for (int j = i + 1; j < i + 3; j++) {
                        pairs.add(new String[]{products[i].getName(), products[j].getName()});
                        /*spi = new BackGeocodingOp.Spi();
                        op = (BackGeocodingOp) spi.createOperator();
                        sourceProducts[0] = products[i];
                        sourceProducts[1] = products[j];
                        op.setSourceProducts(sourceProducts);
                        targetProduct = op.getTargetProduct();
                        float modelledCoherence = Float.valueOf(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getElement("Baselines").getElementAt(0).getElementAt(1).getAttribute("Modelled Coherence").getData().toString());
                        if (modelledCoherence > modelledCoherenceThreshold) {
                            network.addEdge(i, j);
                        }
                        targetProduct.closeProductReader();
                        targetProduct.closeProductWriter();
                        targetProduct.closeIO();
                        targetProduct.dispose();
                        op.dispose();*/
                    }
                }
                //visited = network.DFS(0);
                //modelledCoherenceThreshold -= 0.01f;
            //}
            //System.out.println("Modelled coherence: " + String.valueOf(modelledCoherenceThreshold + 0.01f));

            for (int i = 0; i < products.length; i++) {
                products[i].closeIO();
            }

            /*Iterator<LinkedList<Integer>> masterIter = Arrays.stream(network.adj).iterator();
            int k = 0;
            while (masterIter.hasNext()) {
                String masterName = productNames[k];
                Iterator<Integer> slaveIter = masterIter.next().iterator();
                while (slaveIter.hasNext()) {
                    String slaveName = productNames[slaveIter.next()];
                    pairs.add(new String[]{masterName, slaveName});
                }
                k += 1;
            }*/

            Sentinel1Utils s1u = new Sentinel1Utils(products[0]);
            int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);
            String polarization = s1u.getPolarizations()[0].toString();

            String graphFile;
            if (numOfBurst > 1) {
                graphFile = "esd.xml";
            } else {
                graphFile = "backgeocoding.xml";
            }

            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // BackGeocoding
            ((HashMap) parameters.get("BackGeocoding")).forEach((key, value) -> {
                graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");
            for (int i = 0; i < pairs.size(); i++) {
                String masterProductDate = pairs.get(i)[0].split("_")[5].split("T")[0];
                String slaveProductDate = pairs.get(i)[1].split("_")[5].split("T")[0];
                graph.getNode("Read").getConfiguration().getChild("file").setValue(applyorbitfileDir + File.separator + pairs.get(i)[0] + ".dim");
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(applyorbitfileDir + File.separator + pairs.get(i)[1] + ".dim");
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(backgeocodingDir + File.separator + masterProductDate + "_" + polarization + "_" + slaveProductDate + "_" + polarization + ".dim");

                FileWriter fileWriter = new FileWriter(stage2Dir + File.separator + masterProductDate + "_" + polarization + "_" + slaveProductDate + "_" + polarization + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage2Dir + File.separator + masterProductDate + "_" + polarization + "_" + slaveProductDate + "_" + polarization + ".xml");
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
            JSONParser parser = new JSONParser();
            stageParameters = new HashMap<>();

            // BackGeocoding
            parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "back_geocoding.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("BackGeocoding", parameters);
            fileReader.close();

            // GoldsteinPhaseFiltering
            // TODO:GoldsteinPhaseFiltering set up parameters

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static boolean verifyAllEqual(boolean[] array1) {
        boolean firstElem = array1[0];
        for (int i = 0; i < array1.length; i++) {
            if (firstElem != array1[i])
                return false;
        }
        return true;
    }
}
