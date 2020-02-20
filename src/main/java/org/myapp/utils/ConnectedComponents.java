package org.myapp.utils;

import com.twitter.chill.java.ArraysAsListSerializer;
import org.esa.s1tbx.sentinel1.gpf.TOPSARSplitOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.SubsetOp;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class ConnectedComponents {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        int connectedComponentPercent = Integer.valueOf(consoleParameters.get("connectedComponentPercent").toString());
        float minCoh = Float.valueOf(consoleParameters.get("minCoh").toString());


        try {

            List<String> pairPaths = new ArrayList<>();

            Product[] products = Files.walk(Paths.get(workingDir))
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

            Files.walk(Paths.get(workingDir))
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
                                        entry.getValue().add(file.getParent());
                                    }
                                });

                                // TODO: delete
                                //System.out.println(ccBandName + " -> " + counter + " points (" + file.getParent() + ")");
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

            System.out.println("Y:" + maxConnectedComponentPoint[1] + ", X:" + maxConnectedComponentPoint[0]);

            List<String> includedPairPaths = maxConnectedComponentPaths;
            pairPaths.stream().filter(path -> !includedPairPaths.contains(Paths.get(path)))
                    .forEach(path -> {
                        removeDirectory(new File(path));
                    });

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
}