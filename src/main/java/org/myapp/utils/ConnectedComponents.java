package org.myapp.utils;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectedComponents {

    public static void main(String[] args) {

        /*
        workingDir="F:\\mnt\\fast\\dockers\\monitor-radar-core\\monitor_radar_usr\\processing\\1580805641883"
        resultDir="F:\\mnt\\hdfs\\user\\monitor_radar_usr\\monitor-radar-core\\results\\1580805641883"
        connectedComponentPercent=10
        minCoh=0.87
         */

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String resultDir = consoleParameters.get("resultDir").toString();
        int connectedComponentPercent = Integer.valueOf(consoleParameters.get("connectedComponentPercent").toString());
        float minCoh = Float.valueOf(consoleParameters.get("minCoh").toString());


        try {

            String configDir = resultDir + File.separator + "config";
            HashMap<String, HashMap> snaphuParameters = getParameters(configDir);
            Object generateConnectedComponentsFile = ((HashMap) snaphuParameters.get("Snaphu")).get("generateConnectedComponentsFile");

            if (!Boolean.valueOf(generateConnectedComponentsFile.toString())) {
                return;
            }

            List<String> pairPaths = new ArrayList<>();

            Product[] products = Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_coh_tc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            pairPaths.add(file.getParent());
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);

            int maxWidth = products[0].getSceneRasterWidth(), maxHeight = products[0].getSceneRasterHeight();
            for (Product product : products) {
                maxWidth = product.getSceneRasterWidth() > maxWidth ? product.getSceneRasterWidth() : maxWidth;
                maxHeight = product.getSceneRasterHeight() > maxHeight ? product.getSceneRasterHeight() : maxHeight;
            }

            float[][] aveCoh = new float[maxHeight][maxWidth];
            for (Product product : products) {
                product.getBandAt(0).readRasterDataFully();
                float[] data = ((ProductData.Float) product.getBandAt(0).getData()).getArray();
                for (int y = 0; y < product.getSceneRasterHeight(); y++) {
                    for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                        aveCoh[y][x] = aveCoh[y][x] + data[y * product.getSceneRasterWidth() + x];
                    }
                }
                product.closeIO();
            }
            for (int y = 0; y < maxHeight; y++) {
                for (int x = 0; x < maxWidth; x++) {
                    aveCoh[y][x] = aveCoh[y][x] / (float) products.length;
                }
            }


            HashMap<Double[], ArrayList> maxCoh = new HashMap<>();
            for (int y = 0; y < maxHeight; y++) {
                for (int x = 0; x < maxWidth; x++) {
                    if (aveCoh[y][x] > minCoh) {
                        PixelPos pixelPos = new PixelPos(x, y);
                        GeoPos geoPos = new GeoPos();
                        products[0].getBandAt(0).getGeoCoding().getGeoPos(pixelPos, geoPos);
                        maxCoh.put(new Double[]{geoPos.lat, geoPos.lon}, new ArrayList());
                    }
                }
            }

            Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_cc.dim"))
                    .forEach(file -> {

                        try {

                            Product product = ProductIO.readProduct(file.toFile());
                            Band[] bands = product.getBands();
                            String ccBandName = Arrays.stream(bands).filter(band -> band.getName().contains("Unw_"))
                                    .map(Band::getName).toArray(String[]::new)[0];

                            product.getBand(ccBandName).readRasterDataFully();
                            float[] data = ((ProductData.Float) product.getBand(ccBandName).getData()).getArray();
                            int counter = 0;
                            for (int i = 0; i < data.length; i++) {
                                if (data[i] > 0.0) {
                                    counter += 1;
                                }
                            }

                            if (((float) counter / (float) data.length) * 100 > connectedComponentPercent) {
                                maxCoh.entrySet().stream().forEach(entry -> {
                                    double lat = entry.getKey()[0];
                                    double lon = entry.getKey()[1];
                                    GeoPos geoPos = new GeoPos(lat, lon);
                                    PixelPos pixelPos = new PixelPos();
                                    int width = product.getBand(ccBandName).getRasterWidth();
                                    product.getBand(ccBandName).getGeoCoding().getPixelPos(geoPos, pixelPos);
                                    if (data[(int) pixelPos.getY() * width + (int) pixelPos.getX()] > 0.0) {
                                        entry.getValue().add(file.getParent().toString());
                                    }
                                });

                                // TODO: убрать
                                // System.out.println(ccBandName + " -> " + counter + " points (" + file.getParent() + ")");
                                //
                            }

                            product.closeIO();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            Iterator iterator = maxCoh.keySet().iterator();
            int maxConnectedComponentPathAmount = 0;
            ArrayList<String> maxConnectedComponentPaths = null;
            Double[] maxConnectedComponentPoint = null;
            while (iterator.hasNext()) {
                Object connectedComponentPoint = iterator.next();
                ArrayList<String> connectedComponentPaths = maxCoh.get(connectedComponentPoint);
                if (connectedComponentPaths.size() > maxConnectedComponentPathAmount) {
                    maxConnectedComponentPathAmount = connectedComponentPaths.size();
                    maxConnectedComponentPaths = connectedComponentPaths;
                    maxConnectedComponentPoint = (Double[]) connectedComponentPoint;
                }
            }

            // TODO: убрать
            // System.out.println("Y:" + maxConnectedComponentPoint[1] + ", X:" + maxConnectedComponentPoint[0]);
            //

            String excludedIfgIndex = "";
            for (int i = 0; i < pairPaths.size(); i++) {
                if (!maxConnectedComponentPaths.contains(pairPaths.get(i))) {
                    excludedIfgIndex = excludedIfgIndex + i + ",";
                }
            }
            excludedIfgIndex = excludedIfgIndex.substring(0, excludedIfgIndex.length() - 1).trim();

            modifyMintPyConfigFile(workingDir, configDir, maxConnectedComponentPoint[1], maxConnectedComponentPoint[0], excludedIfgIndex);

            /*
            pairPaths.stream().filter(path -> !includedPairPaths.contains(Paths.get(path)))
                    .forEach(path -> {
                        //removeDirectory(new File(path));
                    });
             */


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDirectory(aFile);
                }
            }
            dir.delete();
        } else {
            dir.delete();
        }
    }

    public static void modifyMintPyConfigFile(String workingDir, String configDir, double y, double x, String excludedIfgIndex) {
        try {
            // TODO: данные и другие параметры из smallbaseline.cfg менять из файла smallbaseline.json, когда будет добавлен раздел настройки MintPy в monitor-radar-frontend

            String smallBaseLineAppConfig = Files.lines(Paths.get(configDir + File.separator + "smallbaselineApp.cfg")).map(line -> {
                if (line.contains("mintpy.load.connCompFile")) {
                    return "mintpy.load.connCompFile = /home/work/*_*/*_cc*.data/Unw*.img";
                }
                if (line.contains("mintpy.network.tempBaseMax")) {
                    return "mintpy.network.tempBaseMax = 250";
                }
                if (line.contains("mintpy.network.perpBaseMax")) {
                    return "mintpy.network.perpBaseMax = 250";
                }
                if (line.contains("mintpy.reference.lalo")) {
                    return "mintpy.reference.lalo = " + x + "," + y;
                }
                if (line.contains("mintpy.unwrapError.method")) {
                    return "mintpy.unwrapError.method = bridging";
                }
                if (line.contains("mintpy.unwrapError.ramp")) {
                    return "mintpy.unwrapError.ramp = linear";
                }
                if (line.contains("mintpy.network.excludeIfgIndex")) {
                    return "mintpy.network.excludeIfgIndex = " + excludedIfgIndex;
                }
                return line;
            }).collect(Collectors.joining("\n"));

            // Write ifgs12_excluded.txt to file
            PrintWriter out = new PrintWriter(workingDir + File.separator + "prep" + File.separator + "smallbaselineApp.cfg");
            out.println(smallBaseLineAppConfig);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // DataSet
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "snaphu.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("Snaphu",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }
}