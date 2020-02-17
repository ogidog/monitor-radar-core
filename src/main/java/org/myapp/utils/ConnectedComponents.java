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
import java.util.stream.Collectors;

public class ConnectedComponents {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        int amount = Integer.valueOf(consoleParameters.get("amount").toString());


        try {
            ArrayList<Path> paths = new ArrayList();
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
                            if (((float) counter / (float) data.length) * 100 > amount) {
                                System.out.println(ccBandName + " -> " + counter + " points (" + file.getParent() + ")");
                            } else {
                                paths.add(file.getParent());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            paths.stream().forEach(path -> {
                try {
                    Files.walk(path)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (Exception e) {
                    System.out.println(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
