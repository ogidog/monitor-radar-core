package org.myapp.satellite.radar.NDAI;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                /*
                else {
                    //LLH (band3, red)
                    if ((band2 / band3) * 100 < 30f && (band1 / band3) * 100 < 30f) {
                        avgNDAIProduct.getBandAt(2).getRasterData().setElemFloatAt(i, 1.0f);
                    }
                    //LHL (band2, green)
                    if ((band3 / band2) * 100 < 30f && (band1 / band2) * 100 < 30f) {
                        avgNDAIProduct.getBandAt(1).getRasterData().setElemFloatAt(i, 2.0f);
                    }
                    //HLL (band1, blue)
                    if ((band3 / band1) * 100 < 30f && (band2 / band1) * 100 < 30f) {
                        avgNDAIProduct.getBandAt(0).getRasterData().setElemFloatAt(i, 3.0f);
                    }
                    //HHL (band1, band2, cyan)
                    if ((band3 / band1) * 100 < 30f && (band3 / band2) * 100 < 30f && Math.abs((band1 / band2) * 100 - 100) < 30f) {
                        avgNDAIProduct.getBandAt(2).getRasterData().setElemFloatAt(i, 4.0f);
                    }
                    //HLH (band1, band3, magenta)
                    if ((band2 / band1) * 100 < 30f && (band2 / band3) * 100 < 30f && Math.abs((band1 / band3) * 100 - 100) < 30f) {
                        avgNDAIProduct.getBandAt(1).getRasterData().setElemFloatAt(i, 5.0f);
                    }
                    //LHH (band2, band3, yellow)
                    if ((band1 / band2) * 100 < 0.3 && (band1 / band3) * 100 < 30f && Math.abs((band2 / band3) * 100 - 100) < 30f) {
                        avgNDAIProduct.getBandAt(0).getRasterData().setElemFloatAt(i, 6.0f);
                    }
                    //LLL (band1, band2, band3, white)
                    if (band1  < 0.003 && band2 < 0.003 && band3 < 0.003) {
                        avgNDAIProduct.getBandAt(2).getRasterData().setElemFloatAt(i, 7.0f);
                    }
                    //HHH (band1, band2, band3, black)
                    if (band1  > 0.3 && band2 > 0.3 && band3 > 0.3) {
                        avgNDAIProduct.getBandAt(1).getRasterData().setElemFloatAt(i, 8.0f);
                    }
                }*/
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
}
