package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

public class Stage7 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String stablePointDir = outputDir + File.separator + "stablepoints";
            String stablePointIndexesDir = outputDir + File.separator + "stablepointindexes";

            if (Files.exists(Paths.get(stablePointIndexesDir))) {
                Files.walk(Paths.get(stablePointIndexesDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stablePointIndexesDir).mkdirs();

            Product stablePointsProduct = ProductIO.readProduct(stablePointDir + File.separator + "stablepoints.dim");
            String[] stablePointsBandNames = stablePointsProduct.getBandNames();

            for (String bandName : stablePointsBandNames) {
                Band stablePointsBand = stablePointsProduct.getBand(bandName);
                stablePointsBand.readRasterDataFully();
                float[] stablePointFlags = ((ProductData.Float) stablePointsBand.getData()).getArray();
                ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(stablePointIndexesDir + File.separator + bandName + ".dat"));
                for (int i = 0; i < stablePointFlags.length; i++) {
                    if (stablePointFlags[i] == 1.0f) {
                        ous.writeInt(i);
                    }
                }
                ous.flush();
                ous.close();
            }

            stablePointsProduct.closeIO();
            stablePointsProduct.dispose();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
