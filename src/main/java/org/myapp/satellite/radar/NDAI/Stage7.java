package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.CustomErrorHandler;
import org.myapp.utils.Routines;

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

            //boolean isStablePointsExist = false;
            for (String bandName : stablePointsBandNames) {
                //isStablePointsExist = false;
                Band stablePointsBand = stablePointsProduct.getBand(bandName);
                stablePointsBand.readRasterDataFully();
                float[] stablePointFlags = ((ProductData.Float) stablePointsBand.getData()).getArray();
                ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(stablePointIndexesDir + File.separator + bandName + ".dat"));
                for (int i = 0; i < stablePointFlags.length; i++) {
                    if (stablePointFlags[i] == 1.0f) {
                        ous.writeInt(i);
                        //isStablePointsExist = true;
                    }
                }

                ous.flush();
                ous.close();

                /*if (!isStablePointsExist) {
                    throw new Exception("No stable points.");
                }*/
            }

            stablePointsProduct.closeIO();
            stablePointsProduct.dispose();

        } catch (Exception e) {
            CustomErrorHandler.writeErrorToFile(e.getMessage(), "/mnt/task" + File.separator + "ERROR");
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        String stablePointDir = taskDir + File.separator + "stablepoints";
        String stablePointIndexesDir = taskDir + File.separator + "stablepointindexes";

        if (Files.exists(Paths.get(stablePointIndexesDir))) {
            Routines.deleteDir(new File(stablePointIndexesDir));
        }
        new File(stablePointIndexesDir).mkdirs();

        Product stablePointsProduct = ProductIO.readProduct(stablePointDir + File.separator + "stablepoints.dim");
        String[] stablePointsBandNames = stablePointsProduct.getBandNames();

        boolean isStablePointsExist = false;
        for (String bandName : stablePointsBandNames) {
            isStablePointsExist = false;
            Band stablePointsBand = stablePointsProduct.getBand(bandName);
            stablePointsBand.readRasterDataFully();
            float[] stablePointFlags = ((ProductData.Float) stablePointsBand.getData()).getArray();
            ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(stablePointIndexesDir + File.separator + bandName + ".dat"));
            for (int i = 0; i < stablePointFlags.length; i++) {
                if (stablePointFlags[i] == 1.0f) {
                    ous.writeInt(i);
                    isStablePointsExist = true;
                }
            }

            ous.flush();
            ous.close();

            if (!isStablePointsExist) {
                stablePointsProduct.closeIO();
                stablePointsProduct.dispose();

                throw new Exception("Stage7: No stable points");
            }
        }
        stablePointsProduct.closeIO();
        stablePointsProduct.dispose();
    }
}
