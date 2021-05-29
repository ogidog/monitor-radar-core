package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Stage9 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String ndaiDir = outputDir + File.separator + "ndai";
            String ndaiFile = ndaiDir + File.separator + "ndai.dim";
            String avgNDAIDir = outputDir + File.separator + "avgndai";

            if (Files.exists(Paths.get(avgNDAIDir))) {
                Files.walk(Paths.get(avgNDAIDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(avgNDAIDir).mkdirs();

            Product ndaiProduct = ProductIO.readProduct(ndaiFile);
            int width = ndaiProduct.getSceneRasterWidth();
            int height = ndaiProduct.getSceneRasterHeight();

            int idx;
            int falsePixelsCounter = 0;
            int falsePixelsTreshhold = (int) (ndaiProduct.getSceneRasterWidth() * 0.1);
            int subsetY0 = -1, subsetY1 = -1, subsetX0 = -1, subsetX1 = -1;
            int maxSubsetY0 = 0, minSubsetY1 = height, maxSubsetX0 = 0, minSubsetX1 = width;

            Band[] ndaiBands = ndaiProduct.getBands();
            for (Band band : ndaiBands) {

                falsePixelsCounter = 0;
                subsetY0 = -1;
                subsetY1 = -1;
                subsetX0 = -1;
                subsetX1 = -1;

                band.readRasterDataFully();
                float[] data = ((ProductData.Float) band.getData()).getArray();

                // by width
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        idx = width * y + x;
                        if (data[idx] == 1.0) {
                            falsePixelsCounter += 1;
                        }
                    }
                    if (falsePixelsCounter < falsePixelsTreshhold && subsetY0 == -1) {
                        subsetY0 = y;
                    }
                    if (falsePixelsCounter > falsePixelsTreshhold && subsetY0 != -1) {
                        subsetY1 = y - 1;
                        break;
                    }
                    falsePixelsCounter = 0;
                }
                if (subsetY1 == -1) {
                    subsetY1 = height;
                }
                if (subsetY0 == -1) {
                    subsetY0 = 0;
                }

                // by height
                falsePixelsCounter = 0;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        idx = width * y + x;
                        if (data[idx] == 1.0) {
                            falsePixelsCounter += 1;
                        }
                    }
                    if (falsePixelsCounter < falsePixelsTreshhold && subsetX0 == -1) {
                        subsetX0 = x;
                    }
                    if (falsePixelsCounter > falsePixelsTreshhold && subsetX0 != -1) {
                        subsetX1 = x - 1;
                        break;
                    }
                    falsePixelsCounter = 0;
                }
                if (subsetX1 == -1) {
                    subsetX1 = width;
                }
                if (subsetX0 == -1) {
                    subsetX0 = 0;
                }


                if (subsetY0 > maxSubsetY0) {
                    maxSubsetY0 = subsetY0;
                }
                if (subsetY1 < minSubsetY1) {
                    minSubsetY1 = subsetY1;
                }
                if (subsetX0 > maxSubsetX0) {
                    maxSubsetX0 = subsetX0;
                }
                if (subsetX1 < minSubsetX1) {
                    minSubsetX1 = subsetX1;
                }
            }

            ndaiProduct.closeIO();
            ndaiProduct.dispose();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
