
package org.myapp.satellite.radar.mintpy;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
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
import java.util.Map;
import java.util.stream.Collectors;


public class Stage1 {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();
        String resultDir = consoleParameters.get("resultDir").toString();
        String filesList = consoleParameters.get("filesList").toString();

        String configDir = resultDir + File.separator + "config";

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            System.out.println("Fail to read parameters.");
            return;
        }

        String[] files = filesList.split(",");

        try {
            Files.walk(Paths.get(workingDir + File.separator + "applyorbitfile"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            new File(workingDir + File.separator + "applyorbitfile").mkdirs();
        } catch (Exception e) {
            System.out.println(e);
        }

        TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
        ApplyOrbitFileOpEnv applyOrbitFileOpEnv = new ApplyOrbitFileOpEnv(snapDir);
        WriteOpEnv writeOpEnv = new WriteOpEnv();

        Product targetProduct;

        for (int i = 0; i < files.length; i++) {
            try {

                targetProduct = topsarSplitOpEnv.getTargetProduct(files[i], parameters);

                targetProduct = applyOrbitFileOpEnv.getTargetProduct(targetProduct, parameters);

                if (targetProduct != null) {
                    writeOpEnv.write(workingDir + File.separator + "applyorbitfile", targetProduct);
                }

                targetProduct.closeIO();

            } catch (Exception e) {
                System.out.println(e);
            }

            applyOrbitFileOpEnv.Dispose();
            topsarSplitOpEnv.Dispose();
        }

        HashMap subsetModifiedParameters = new HashMap();
        subsetModifiedParameters.put("geoRegion", topsarSplitOpEnv.getIntersectionGeoRegion());
        saveParameters(configDir, "subset", subsetModifiedParameters);

        HashMap topsarSplitModifiedParameters = new HashMap();
        topsarSplitModifiedParameters.put("firstBurstIndex", topsarSplitOpEnv.getFirstBurstIndex());
        topsarSplitModifiedParameters.put("lastBurstIndex", topsarSplitOpEnv.getLastBurstIndex());
        //topsarSplitModifiedParameters.put("masterName", getOptimalMaster(files));
        saveParameters(configDir, "s1_tops_split", topsarSplitModifiedParameters);

        // Composing interferometric pairs
        System.out.println("\nComposing interferometric pairs...\n");
        composingIntfPairs(files, workingDir);
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // TOPSARSplit
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "s1_tops_split.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

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
            parameters.put("geoRegionCoordinates", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

    static void saveParameters(String configDir, String configName, HashMap<String, String> modifiedParameters) {
        FileReader fileReader;
        FileWriter fileWriter;
        JSONParser jsonParser;
        try {
            fileReader = new FileReader(configDir + File.separator + configName + ".json");
            jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(fileReader);
            fileReader.close();

            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            modifiedParameters.forEach((k, v) -> {
                ((HashMap) jsonParameters.get(k)).put("value", v);
            });
            fileWriter = new FileWriter(configDir + File.separator + configName + ".json");
            fileWriter.write(jsonObject.toJSONString());
            fileWriter.close();

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    static void composingIntfPairs(String[] files, String workingDir) {

        Product[] products = Arrays.stream(files).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        String pairsStr = "";
        int pairCounter = 0;

        try {

            InSARStackOverview.IfgPair[] pairs;
            InSARStackOverview.IfgPair pair;
            InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(products);

            for (int i = 0; i < stackOverview.length; i++) {
                pairs = stackOverview[i].getMasterSlave();
                for (int j = i; j < pairs.length; j++) {
                    pair = pairs[j];
                    /*double bNorm = pair.getPerpendicularBaseline();
                    double bTemp = pair.getTemporalBaseline();
                    double dopplerDiff = pair.getDopplerDifference();
                    double heightAmbiguity = pair.getHeightAmb();*/
                    double coh = pair.getCoherence();

                    if (pair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString().equals(
                            pair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString())) {
                        continue;
                    }
                    // if (Math.abs(bNorm) <= 121 && Math.abs(bTemp) <= 120 && Math.abs(heightAmbiguity) < 300) {
                    // if (Math.abs(bNorm) <= 2500 && Math.abs(bTemp) <= 40) {
                    if (coh >= 0.9) {
                        pairsStr = pairsStr
                                + pair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString() + ","
                                + pair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString() + ";";
                        pairCounter++;
                    }
                }
            }
            pairsStr = pairsStr.substring(0, pairsStr.length() - 1);
            System.out.println("Total interferometric pairs: " + pairCounter);

            if (Files.exists(Paths.get(workingDir + File.separator + "applyorbitfile" + File.separator + "pairs.txt"))) {
                Files.delete(Paths.get(workingDir + File.separator + "applyorbitfile" + File.separator + "pairs.txt"));
            }

            PrintWriter out = new PrintWriter(workingDir + File.separator + "applyorbitfile" + File.separator + "pairs.txt");
            out.print(pairsStr);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    static String getOptimalMaster(String[] files) {
        String masterName = "";
        Product[] products = Arrays.stream(files).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);
        try {
            masterName = InSARStackOverview.findOptimalMasterProduct(products).getName();
        } catch (Exception e) {
            System.out.println(e);
        }
        Arrays.stream(products).forEach(product -> {
            try {
                product.closeIO();
            } catch (IOException e) {
                System.out.println(e);
            }
        });

        return masterName;
    }
}
