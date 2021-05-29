package org.myapp.satellite.radar.NDAI;

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
            String avgNDAIFile = outputDir + File.separator + "avgndai" + File.separator + "avgndai.tif";
            String cohAvgStd = outputDir + File.separator + "avgstd" + File.separator + "cohavgstd.dim";

            Product avgNDAIProduct = ProductIO.readProduct(avgNDAIFile);
            Product cohAvgStdProduct = ProductIO.readProduct(cohAvgStd);

            Band[] avgNDAIBand = avgNDAIProduct.getBands();

            //product.getBandAt(0).getRasterData().setElemFloatAt(j, Float.NaN);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
