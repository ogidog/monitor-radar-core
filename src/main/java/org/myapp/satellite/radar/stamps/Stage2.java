
package org.myapp.satellite.radar.stamps;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;


public class Stage2 {

    public static void main(String[] args) {

        /* outputDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing"
        snapDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\.snap"
        configDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\config"
        filesList="Y:\\Satellites\\Sentinel-1A\\S1A_IW_SLC__1SDV_20170122T002755_20170122T002824_014937_018613_A687.zip,Y:\\Satellites\\Sentinel-1A\\S1A_IW_SLC__1SDV_20170215T002754_20170215T002824_015287_0190E5_24DE.zip"*/

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String outputDir = consoleParameters.get("outputDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();
        String configDir = consoleParameters.get("configDir").toString();
        String graphDir = consoleParameters.get("graphDir").toString();
        String filesList = consoleParameters.get("filesList").toString();

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            System.out.println("Fail to read parameters.");
            return;
        }

        String[] files;
        try {
            files = Files.walk(Paths.get(filesList)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String esdDir = outputDir + "" + File.separator + "esd";
        if (Files.exists(Paths.get(esdDir))) {
            try {
                Files.walk(Paths.get(esdDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        String topophaseremovalDir = outputDir + "" + File.separator + "topophaseremoval";
        if (Files.exists(Paths.get(topophaseremovalDir))) {
            try {
                Files.walk(Paths.get(topophaseremovalDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        String stage2Dir = outputDir + "" + File.separator + "stage2";
        if (Files.exists(Paths.get(stage2Dir))) {
            try {
                Files.walk(Paths.get(stage2Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        new File(esdDir).mkdirs();
        new File(topophaseremovalDir).mkdirs();
        new File(stage2Dir).mkdirs();

        Product[] products = Arrays.stream(files).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        try {
            Sentinel1Utils s1u = new Sentinel1Utils(products[0]);
            int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);

            String graphFile;
            if (numOfBurst > 1) {
                graphFile = "stamps_prep.xml";
            } else {
                graphFile = "stamps_prep_without_esd.xml";
            }
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            String masterProductPath = Paths.get(graph.getNode("Read").getConfiguration().getChild("file").getValue()).getParent().toString();
            String masterProductName = Paths.get(graph.getNode("Read").getConfiguration().getChild("file").getValue())
                    .getFileName().toString().replace(".dim", "");

            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");

            for (int i = 0; i < products.length; i++) {
                Product product = products[i];
                String slaveProductName = product.getName();
                if (!masterProductName.equals(slaveProductName)) {
                    graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + slaveProductName + ".dim");
                    graph.getNode("Write").getConfiguration().getChild("file")
                            .setValue(esdDir + File.separator + slaveProductName + "_Stack_Deb.dim");
                    graph.getNode("Write(2)").getConfiguration().getChild("file")
                            .setValue(topophaseremovalDir + File.separator + slaveProductName + "_Stack_Ifg_Deb_DInSAR.dim");

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

            // Interferogram
            fileReader = new FileReader(configDir + File.separator + "interferogram_formation.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("Interferogram",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            fileReader.close();

            // TopoPhaseRemoval
            fileReader = new FileReader(configDir + File.separator + "topo_phase_removal.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");
            jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("TopoPhaseRemoval",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            fileReader.close();

            // Subset
            fileReader = new FileReader(configDir + File.separator + "subset.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            HashMap parameters = new HashMap();
            parameters.put("geoRegion", "POLYGON ((" + geoRegionCoordinates + "))");
            stageParameters.put("Subset", parameters);

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}
