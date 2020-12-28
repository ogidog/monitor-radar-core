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
            int subsetY0 = -1, subsetY1 = -1, subsetX0 = -1, subsetX1 = -1;

            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();

            // by width
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    idx = width * y + x;
                    if (data[idx] == 0) {
                        nanCounter += 1;
                    }
                }
                if (nanCounter < nanTreshhold && subsetY0 == -1) {
                    subsetY0 = y;
                }
                if (nanCounter > nanTreshhold && subsetY0 != -1) {
                    subsetY1 = y - 1;
                    break;
                }
                nanCounter = 0;
            }

            if (subsetY1 == -1) {
                subsetY1 = height;
            }

            // by height
            nanCounter = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    idx = width * y + x;
                    if (data[idx] == 0) {
                        nanCounter += 1;
                    }
                }
                if (nanCounter < nanTreshhold && subsetX0 == -1) {
                    subsetX0 = x;
                }
                if (nanCounter > nanTreshhold && subsetX0 != -1) {
                    subsetX1 = x - 1;
                    break;
                }
                nanCounter = 0;
            }

            if (subsetX1 == -1) {
                subsetX1 = width;
            }

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }
}
