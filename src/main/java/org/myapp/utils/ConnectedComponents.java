package org.myapp.utils;

import com.twitter.chill.java.ArraysAsListSerializer;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

            int minWidth = products[0].getSceneRasterWidth(), minHeight = products[0].getSceneRasterHeight();
            for (Product product : products) {
                minWidth = product.getSceneRasterWidth() < minWidth ? product.getSceneRasterWidth() : minWidth;
                minHeight = product.getSceneRasterHeight() < minHeight ? product.getSceneRasterHeight() : minHeight;
            }

            float[] aveCoh = new float[minWidth * minHeight];
            for (Product product : products) {
                product.getBandAt(0).readRasterDataFully();
                float[] data = ((ProductData.Float) product.getBandAt(0).getData()).getArray();
                for (int i = 0; i < aveCoh.length; i++) {
                    aveCoh[i] = aveCoh[i] + data[i];
                }
                product.closeIO();
            }
            for (int i = 0; i < aveCoh.length; i++) {
                aveCoh[i] = aveCoh[i] / (float) products.length;
            }
            HashMap<Integer, ArrayList> maxCoh = new HashMap<>();
            for (int i = 0; i < aveCoh.length; i++) {
                if (aveCoh[i] > minCoh) {
                    maxCoh.put(i, new ArrayList());
                }
            }

            Files.walk(Paths.get(workingDir))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_cc.dim"))
                    .forEach(file -> {
                        try {
                            Product sourceProduct = ProductIO.readProduct(file.toFile());
                            Band[] bands = sourceProduct.getBands();
                            String ccBandName = Arrays.stream(bands).filter(band -> band.getName().contains("Unw_"))
                                    .map(Band::getName).toArray(String[]::new)[0];
                            sourceProduct.getBand(ccBandName).readRasterDataFully();
                            float[] data = ((ProductData.Float) sourceProduct.getBand(ccBandName).getData()).getArray();
                            int counter = 0;
                            for (int i = 0; i < data.length; i++) {
                                if (data[i] > 0.0) {
                                    counter += 1;
                                }
                            }
                            sourceProduct.closeIO();
                            if (((float) counter / (float) data.length) * 100 > connectedComponentPercent) {
                                maxCoh.entrySet().stream().forEach(entry -> {
                                    if (data[entry.getKey()] > 0.0) {
                                        entry.getValue().add(file.getParent());
                                    }
                                });

                                // TODO: delete
                                System.out.println(ccBandName + " -> " + counter + " points (" + file.getParent() + ")");
                                //
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });


            int pointWithMaxConnectedComponents = maxCoh.keySet().stream()
                    .max(new Comparator<Integer>() {
                        @Override
                        public int compare(Integer key1, Integer key2) {
                            return maxCoh.get(key1).size() - maxCoh.get(key2).size();
                        }
                    }).get();

            System.out.println("Y:" + pointWithMaxConnectedComponents / minWidth + ", X:" + (pointWithMaxConnectedComponents - (pointWithMaxConnectedComponents / minWidth) * minWidth));

            List<String> includedPairPaths = maxCoh.get(pointWithMaxConnectedComponents);

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