package org.myapp.satellite.radar.NDAI;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.util.HashMap;


public class Stage10 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);

            String outputDir = consoleParameters.get("outputDir").toString();
            String filteredAvgNDAIFile = outputDir + File.separator + "avgndai" + File.separator + "filteredavgndai.dim";
            String avgNDAIFile = outputDir + File.separator + "avgndai" + File.separator + "avgndai.dim";
            String subsetedCohAvgStdFile = outputDir + File.separator + "avgndai" + File.separator + "subsetedCohAvgStd.dim";

            Product avgNDAIProduct = ProductIO.readProduct(avgNDAIFile);
            Band[] avgNDAIBands = avgNDAIProduct.getBands();
            avgNDAIBands[0].readRasterDataFully();
            avgNDAIBands[1].readRasterDataFully();
            avgNDAIBands[2].readRasterDataFully();

            Product subsetedCohAvgStdProduct = ProductIO.readProduct(subsetedCohAvgStdFile);
            Band[] cohAvgStdBands = subsetedCohAvgStdProduct.getBands();
            cohAvgStdBands[0].readRasterDataFully();
            cohAvgStdBands[1].readRasterDataFully();
            cohAvgStdBands[2].readRasterDataFully();

            int width = avgNDAIProduct.getSceneRasterWidth();
            int height = avgNDAIProduct.getSceneRasterHeight();
            for (int i = 0; i < width * height; i++) {
                if (cohAvgStdBands[0].getData().getElemFloatAt(i) < 0.55 &&
                        cohAvgStdBands[1].getData().getElemFloatAt(i) < 0.55 &&
                        cohAvgStdBands[2].getData().getElemFloatAt(i) < 0.55) {
                    avgNDAIProduct.getBandAt(0).getRasterData().setElemFloatAt(i, Float.NaN);
                    avgNDAIProduct.getBandAt(1).getRasterData().setElemFloatAt(i, Float.NaN);
                    avgNDAIProduct.getBandAt(2).getRasterData().setElemFloatAt(i, Float.NaN);
                }
            }
            File file = new File(filteredAvgNDAIFile);
            ProductIO.writeProduct(avgNDAIProduct,
                    file,
                    "BEAM-DIMAP",
                    false,
                    ProgressMonitor.NULL);

            avgNDAIProduct.closeIO();
            subsetedCohAvgStdProduct.closeIO();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;

        String filteredAvgNDAIFile = taskDir + File.separator + "avgndai" + File.separator + "filteredavgndai.dim";
        String avgNDAIFile = taskDir + File.separator + "avgndai" + File.separator + "avgndai.dim";
        String subsetedCohAvgStdFile = taskDir + File.separator + "avgndai" + File.separator + "subsetedCohAvgStd.dim";

        Product avgNDAIProduct = ProductIO.readProduct(avgNDAIFile);
        Band[] avgNDAIBands = avgNDAIProduct.getBands();
        avgNDAIBands[0].readRasterDataFully();
        avgNDAIBands[1].readRasterDataFully();
        avgNDAIBands[2].readRasterDataFully();

        Product subsetedCohAvgStdProduct = ProductIO.readProduct(subsetedCohAvgStdFile);
        Band[] cohAvgStdBands = subsetedCohAvgStdProduct.getBands();
        cohAvgStdBands[0].readRasterDataFully();
        cohAvgStdBands[1].readRasterDataFully();
        cohAvgStdBands[2].readRasterDataFully();

        int width = avgNDAIProduct.getSceneRasterWidth();
        int height = avgNDAIProduct.getSceneRasterHeight();
        for (int i = 0; i < width * height; i++) {
            if (cohAvgStdBands[0].getData().getElemFloatAt(i) < 0.55 &&
                    cohAvgStdBands[1].getData().getElemFloatAt(i) < 0.55 &&
                    cohAvgStdBands[2].getData().getElemFloatAt(i) < 0.55) {
                avgNDAIProduct.getBandAt(0).getRasterData().setElemFloatAt(i, Float.NaN);
                avgNDAIProduct.getBandAt(1).getRasterData().setElemFloatAt(i, Float.NaN);
                avgNDAIProduct.getBandAt(2).getRasterData().setElemFloatAt(i, Float.NaN);
            }
        }
        File file = new File(filteredAvgNDAIFile);
        ProductIO.writeProduct(avgNDAIProduct,
                file,
                "BEAM-DIMAP",
                false,
                ProgressMonitor.NULL);

        avgNDAIProduct.closeIO();
        subsetedCohAvgStdProduct.closeIO();

    }
}
