
package org.myapp.satellite.radar.ds;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

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

            String applyorbitfileDir = outputDir + File.separator + "applyorbitfile";
            String[] files = Files.walk(Paths.get(applyorbitfileDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);


            String esdDir = outputDir + "" + File.separator + "esd";
            if (Files.exists(Paths.get(esdDir))) {
                Files.walk(Paths.get(esdDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String subsetEsdDir = outputDir + "" + File.separator + "subsetesd";
            if (Files.exists(Paths.get(subsetEsdDir))) {
                Files.walk(Paths.get(subsetEsdDir))
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

            new File(esdDir).mkdirs();
            new File(subsetEsdDir).mkdirs();
            new File(stage2Dir).mkdirs();

            Product[] products = Arrays.stream(files).map(file -> {
                try {
                    return ProductIO.readProduct(file);
                } catch (Exception e) {
                    return null;
                }
            }).toArray(Product[]::new);

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            Sentinel1Utils s1u = new Sentinel1Utils(products[0]);
            int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);
            String graphFile;
            if (numOfBurst > 1) {
                graphFile = "ds_prep.xml";
            } else {
                graphFile = "ds_prep_without_esd.xml";
            }
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

            String masterProductName = InSARStackOverview.findOptimalMasterProduct(products).getName();
            String masterProductPath = applyorbitfileDir;

            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");

            graph.getNode("Read").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + masterProductName + ".dim");
            for (int i = 0; i < products.length; i++) {
                Product product = products[i];
                String slaveProductName = product.getName();
                if (!masterProductName.equals(slaveProductName)) {
                    graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + slaveProductName + ".dim");
                    graph.getNode("Write").getConfiguration().getChild("file")
                            .setValue(esdDir + File.separator + slaveProductName + "_Stack_Deb.dim");

                    graph.getNode("Write(2)").getConfiguration().getChild("file")
                            .setValue(subsetEsdDir + File.separator + slaveProductName + "_Stack_Deb.dim");

                    FileWriter fileWriter = new FileWriter(stage2Dir + File.separator + slaveProductName + ".xml");
                    GraphIO.write(graph, fileWriter);
                    fileWriter.flush();
                    fileWriter.close();

                    cmdWriter.println("gpt " + stage2Dir + File.separator + slaveProductName + ".xml");
                }
            }

            Arrays.stream(products).forEach(product -> {
                try {
                    product.closeIO();
                } catch (IOException e) {
                    System.out.println(e);
                }
            });

            cmdWriter.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static void process(String outputDir, String configDir, String graphDir, String taskId) throws Exception {

        String taskDir = outputDir + File.separator + taskId;

        String applyorbitfileDir = taskDir + File.separator + "applyorbitfile";
        String[] files = Files.walk(Paths.get(applyorbitfileDir)).filter(path -> {
            if (path.toString().endsWith(".dim")) {
                return true;
            } else {
                return false;
            }
        }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

        String esdDir = taskDir + "" + File.separator + "esd";
        if (Files.exists(Paths.get(esdDir))) {
            Routines.deleteDir(new File(esdDir));
        }
        new File(esdDir).mkdirs();

        String subsetEsdDir = taskDir + "" + File.separator + "subsetesd";
        if (Files.exists(Paths.get(subsetEsdDir))) {
            Routines.deleteDir(new File(subsetEsdDir));
        }
        new File(subsetEsdDir).mkdirs();

        String stage2Dir = taskDir + "" + File.separator + "stage2";
        if (Files.exists(Paths.get(stage2Dir))) {
            Routines.deleteDir(new File(stage2Dir));
        }
        new File(stage2Dir).mkdirs();

        Product[] products = Arrays.stream(files).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            throw new Exception("Stage2: Fail to read parameters.");
        }

        Sentinel1Utils s1u = new Sentinel1Utils(products[0]);
        int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);
        String graphFile;
        if (numOfBurst > 1) {
            graphFile = "ds_prep.xml";
        } else {
            graphFile = "ds_prep_without_esd.xml";
        }
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

        String masterProductName = InSARStackOverview.findOptimalMasterProduct(products).getName();
        String masterProductPath = applyorbitfileDir;

        graph.getNode("Read").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + masterProductName + ".dim");
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            String slaveProductName = product.getName();
            if (!masterProductName.equals(slaveProductName)) {
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + slaveProductName + ".dim");
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(esdDir + File.separator + slaveProductName + "_Stack_Deb.dim");

                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(subsetEsdDir + File.separator + slaveProductName + "_Stack_Deb.dim");

                FileWriter fileWriter = new FileWriter(stage2Dir + File.separator + slaveProductName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                Routines.runGPTScript(stage2Dir + File.separator + slaveProductName + ".xml","Stage2");

            }
        }

        Arrays.stream(products).forEach(product -> {
            try {
                product.closeIO();
            } catch (IOException e) {
                System.out.println(e);
            }
        });

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

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
