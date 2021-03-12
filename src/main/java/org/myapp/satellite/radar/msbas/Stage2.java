package org.myapp.satellite.radar.msbas;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
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
import java.text.SimpleDateFormat;
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

            Sentinel1Utils s1u = new Sentinel1Utils(products[0]);
            int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);
            String polarization = s1u.getPolarizations()[0].toString();

            String graphFile;
            if (numOfBurst > 1) {
                graphFile = "esd.xml";
            } else {
                graphFile = "backgeocoding.xml";
            }

            /*if (Files.exists(Paths.get(backgeocodingDir))) {
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
            new File(stage2Dir).mkdirs();*/

            OperatorSpi spi;
            BackGeocodingOp op;
            Product targetProduct;
            float modelledCoherenceThreshold = 0.93f;

            Graph1 network = new Graph1(products.length);
            for (int i = 0; i < products.length; i++) {
                int masterDate = Integer.valueOf(products[i].getName().split("_")[5].split("T")[0].trim());
                //network.addEdge(i, masterDate);
                for (int j = i + 1; j < products.length; j++) {
                    spi = new BackGeocodingOp.Spi();
                    op = (BackGeocodingOp) spi.createOperator();
                    op.setSourceProducts(new Product[]{products[i], products[j]});
                    targetProduct = op.getTargetProduct();
                    int slaveDate = Integer.valueOf(products[j].getName().split("_")[5].split("T")[0].trim());
                    float modelledCoherence = Float.valueOf(targetProduct.getMetadataRoot().getElement("Abstracted_Metadata").getElement("Baselines").getElementAt(0).getElementAt(1).getAttribute("Modelled Coherence").getData().toString());
                    if (modelledCoherence > modelledCoherenceThreshold) {
                        network.addEdge(i, slaveDate);
                    }
                }
            }
            boolean visited[] = network.DFS(products.length-1);


            /*FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // BackGeocoding
            ((HashMap) parameters.get("BackGeocoding")).forEach((key, value) -> {
                graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            String masterProductName = InSARStackOverview.findOptimalMasterProduct(products).getName();
            String masterProductPath = applyorbitfileDir;
            String masterProductDate = masterProductName.split("_")[5].split("T")[0];
            String slaveProductDate;

            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");
            graph.getNode("Read").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + masterProductName + ".dim");
            for (int i = 0; i < products.length; i++) {
                Product product = products[i];
                String slaveProductName = product.getName();
                if (!masterProductName.equals(slaveProductName)) {
                    slaveProductDate = slaveProductName.split("_")[5].split("T")[0];
                    graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + slaveProductName + ".dim");
                    graph.getNode("Write").getConfiguration().getChild("file")
                            .setValue(backgeocodingDir + File.separator + masterProductDate + "_" + polarization + "_" + slaveProductDate + "_" + polarization + ".dim");

                    FileWriter fileWriter = new FileWriter(stage2Dir + File.separator + masterProductDate + "_" + polarization + "_" + slaveProductDate + "_" + polarization + ".xml");
                    GraphIO.write(graph, fileWriter);
                    fileWriter.flush();
                    fileWriter.close();

                    cmdWriter.println("gpt " + stage2Dir + File.separator + masterProductDate + "_" + polarization + "_" + slaveProductDate + "_" + polarization + ".xml");
                }
            }

            Arrays.stream(products).forEach(product -> {
                try {
                    product.closeIO();
                } catch (IOException e) {
                    System.out.println(e);
                }
            });

            cmdWriter.close();*/

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
}
