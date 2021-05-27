package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class Stage7 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String stage7Dir = outputDir + "" + File.separator + "stage7";
            String stablePointDir = outputDir + File.separator + "stablepoints";

            Product stablePointsProduct = ProductIO.readProduct(stablePointDir + File.separator + "stablepoints.dim");
            String[] stablePointsBandNames = stablePointsProduct.getBandNames();
            String[] yearList = Arrays.stream(stablePointsBandNames).map(bandName -> {
                return bandName.split("_")[2];
            }).distinct().toArray(String[]::new);

            for (String bandName: stablePointsBandNames){
                Band stablePointsBand = stablePointsProduct.getBand(bandName);
                stablePointsBand.readRasterDataFully();
                ProductData pd = stablePointsBand.getData();

                return;
            }

            return;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
