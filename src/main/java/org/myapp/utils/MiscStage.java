package org.myapp.utils;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class MiscStage {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            //String outputDir = consoleParameters.get("outputDir").toString();
            //String graphDir = consoleParameters.get("graphDir").toString();
            String filesList = consoleParameters.get("filesList").toString();

            String[] files;

            files = Files.walk(Paths.get(filesList)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            Product product = ProductIO.readProduct(files[0]);
            product.getBandAt(0).readRasterDataFully();
            short[] data = ((ProductData.Short) product.getBandAt(0).getData()).getArray();

            int idx;
            int nanCounter = 0;
            int nanTreshhold = (int) (product.getSceneRasterWidth() * 0.1);
            int subsetY0 = 0;

            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    idx = width * y + x;
                    if (data[idx] == 0) {
                        nanCounter += 1;
                    }
                }
                if (nanCounter < nanTreshhold) {
                    subsetY0 = y;
                    break;
                } else {
                    nanCounter = 0;
                }
            }

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }
}
