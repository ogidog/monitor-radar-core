package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Stage8 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String ndaiDir = outputDir + File.separator + "ndai";
            String ndaiFile = ndaiDir + File.separator + "ndai.dim";
            String stablePointIndexesDir = outputDir + File.separator + "stablepointindexes";
            String stackFile = outputDir + File.separator + "stack" + File.separator + "stack.dim";

            if (Files.exists(Paths.get(ndaiDir))) {
                Files.walk(Paths.get(ndaiDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(ndaiDir).mkdirs();

            String[] stablePointIndexFiles = Files.walk(Paths.get(stablePointIndexesDir))
                    .filter(path -> path.toString().endsWith(".dat"))
                    .map(path -> path.getFileName().toString()).toArray(String[]::new);

            HashMap<String, Float> bandsRhoStable = new HashMap();
            Product stackProduct = ProductIO.readProduct(stackFile);
            ProductIO.writeProduct(stackProduct, ndaiFile, "BEAM-DIMAP");
            stackProduct.closeIO();
            stackProduct.dispose();

            stackProduct = ProductIO.readProduct(ndaiFile);
            for (String stablePointIndexFile : stablePointIndexFiles) {
                String year = stablePointIndexFile.replace(".dat", "").split("_")[2];

                File file = new File(stablePointIndexesDir + File.separator + stablePointIndexFile);
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                int[] stablePointIndexes = new int[(int) file.length() / 4];
                int j = 0;
                while (ois.available() > 0) {
                    stablePointIndexes[j] = ois.readInt();
                    j += 1;
                }
                ois.close();

                String[] stackBandNames = Arrays.stream(stackProduct.getBandNames())
                        .filter(name -> name.contains(year)).toArray(String[]::new);
                float cohStablePointsSum = 0.0f;
                for (String stackBandName : stackBandNames) {
                    Band stackBand = stackProduct.getBand(stackBandName);
                    stackBand.readRasterDataFully();
                    for (int i = 0; i < stablePointIndexes.length; i++) {
                        int x = stablePointIndexes[i] % stackBand.getRasterWidth();
                        int y = stablePointIndexes[i] / stackBand.getRasterWidth();
                        cohStablePointsSum += stackBand.getPixelFloat(x, y);
                    }
                    bandsRhoStable.put(stackBandName, cohStablePointsSum / stablePointIndexes.length);
                    cohStablePointsSum = 0.0f;
                }
            }

            Band[] stackBands = stackProduct.getBands();
            for (Band band : stackBands) {
                float rhoStable = bandsRhoStable.get(band.getName());
                if (rhoStable < 0.8) {
                    stackProduct.removeBand(band);
                } else {
                    band.readRasterDataFully();
                    ProductData pd = band.getRasterData();
                    for (int i = 0; i < band.getNumDataElems(); i++) {
                        int x = i % band.getRasterWidth();
                        int y = i / band.getRasterWidth();
                        float ndai = (rhoStable - band.getPixelFloat(x, y)) / (rhoStable + band.getPixelFloat(x, y));
                        band.setPixelFloat(x, y, ndai);
                    }
                }
            }
            ProductIO.writeProduct(stackProduct, ndaiFile, "BEAM-DIMAP");
            stackProduct.closeIO();
            stackProduct.dispose();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String configDir, String taskId) throws Exception {

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            throw new Exception("Stage9: Fail to read parameters");
        }

        String taskDir = outputDir + "" + File.separator + taskId;
        String ndaiDir = taskDir + File.separator + "ndai";
        String ndaiFile = ndaiDir + File.separator + "ndai.dim";
        String stablePointIndexesDir = taskDir + File.separator + "stablepointindexes";
        String stackFile = taskDir + File.separator + "stack" + File.separator + "stack.dim";

        if (Files.exists(Paths.get(ndaiDir))) {
            Routines.deleteDir(new File(ndaiDir));
        }
        new File(ndaiDir).mkdirs();

        String[] stablePointIndexFiles = Files.walk(Paths.get(stablePointIndexesDir))
                .filter(path -> path.toString().endsWith(".dat"))
                .map(path -> path.getFileName().toString()).toArray(String[]::new);

        HashMap<String, Float> bandsRhoStable = new HashMap();
        Product stackProduct = ProductIO.readProduct(stackFile);
        ProductIO.writeProduct(stackProduct, ndaiFile, "BEAM-DIMAP");
        stackProduct.closeIO();
        stackProduct.dispose();

        stackProduct = ProductIO.readProduct(ndaiFile);
        for (String stablePointIndexFile : stablePointIndexFiles) {
            String year = stablePointIndexFile.replace(".dat", "").split("_")[2];

            File file = new File(stablePointIndexesDir + File.separator + stablePointIndexFile);
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            int[] stablePointIndexes = new int[(int) file.length() / 4];
            int j = 0;
            while (ois.available() > 0) {
                stablePointIndexes[j] = ois.readInt();
                j += 1;
            }
            ois.close();

            String[] stackBandNames = Arrays.stream(stackProduct.getBandNames())
                    .filter(name -> name.contains(year)).toArray(String[]::new);
            float cohStablePointsSum = 0.0f;
            for (String stackBandName : stackBandNames) {
                Band stackBand = stackProduct.getBand(stackBandName);
                stackBand.readRasterDataFully();
                for (int i = 0; i < stablePointIndexes.length; i++) {
                    int x = stablePointIndexes[i] % stackBand.getRasterWidth();
                    int y = stablePointIndexes[i] / stackBand.getRasterWidth();
                    cohStablePointsSum += stackBand.getPixelFloat(x, y);
                }
                bandsRhoStable.put(stackBandName, cohStablePointsSum / stablePointIndexes.length);
                cohStablePointsSum = 0.0f;
            }
        }

        float rhoStableThres = Float.valueOf(String.valueOf(((HashMap) parameters.get("StablePoints")).get("rhoStableThres")));
        Band[] stackBands = stackProduct.getBands();
        for (Band band : stackBands) {
            float rhoStable = bandsRhoStable.get(band.getName());
            if (rhoStable < rhoStableThres) {
                stackProduct.removeBand(band);
            } else {
                band.readRasterDataFully();
                ProductData pd = band.getRasterData();
                for (int i = 0; i < band.getNumDataElems(); i++) {
                    int x = i % band.getRasterWidth();
                    int y = i / band.getRasterWidth();
                    float ndai = (rhoStable - band.getPixelFloat(x, y)) / (rhoStable + band.getPixelFloat(x, y));
                    band.setPixelFloat(x, y, ndai);
                }
            }
        }
        ProductIO.writeProduct(stackProduct, ndaiFile, "BEAM-DIMAP");
        stackProduct.closeIO();
        stackProduct.dispose();

    }

    static HashMap getParameters(String configDir) {

        try {
            HashMap<String, HashMap> stageParameters = new HashMap<>();

            // Subset
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "stable_points.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters = (HashMap) jsonObject.get("parameters");

            String rhoStableThres = ((HashMap) jsonParameters.get("rhoStableThres")).get("value").toString();
            HashMap parameters = new HashMap();
            parameters.put("rhoStableThres", rhoStableThres);
            stageParameters.put("StablePoints", parameters);

            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
