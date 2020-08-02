package org.myapp.satellite.radar.sbas;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Stage1 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String filesList = consoleParameters.get("filesList").toString();

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            String networkDir = outputDir + "" + File.separator + "network";
            if (Files.exists(Paths.get(networkDir))) {
                Files.walk(Paths.get(networkDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String stage1Dir = outputDir + "" + File.separator + "stage1";

            new File(outputDir).mkdirs();
            new File(outputDir + File.separator + "network").mkdirs();
            new File(stage1Dir).mkdirs();

            Product[] products = Arrays.stream(files).map(file -> {
                try {
                    return ProductIO.readProduct(file);
                } catch (Exception e) {
                    return null;
                }
            }).toArray(Product[]::new);

            InSARStackOverview.IfgPair[] masterSlavePairs;
            InSARStackOverview.IfgStack[] stackOverview;
            InSARStackOverview.IfgPair masterSlavePair;

            String masterProductName, slaveProductName;
            String masterProductDate = "", slaveProductDate;
            String blList = "", dateToProductName = "";

            String optimalMasterName = InSARStackOverview.findOptimalMasterProduct(products).getName();

            stackOverview = InSARStackOverview.calculateInSAROverview(products);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }
}
