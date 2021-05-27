package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
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

public class Stage8 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String ndaiDir = outputDir + File.separator + "ndai";
            String stablePointIndexesDir = outputDir + File.separator + "stablepointindexes";
            String stackFile = outputDir + File.separator + "stack" + File.separator + "stack.dim";

            if (Files.exists(Paths.get(ndaiDir))) {
                Files.walk(Paths.get(ndaiDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(ndaiDir).mkdirs();

            String[] stablePointIndexFiles = Files.walk(Paths.get(stablePointIndexesDir))
                    .filter(path -> path.toString().endsWith(".dat"))
                    .map(path -> path.getFileName().toString()).toArray(String[]::new);

            Product stackProduct = ProductIO.readProduct(stackFile);
            for (String stablePointIndexFile : stablePointIndexFiles) {
                String year = stablePointIndexFile.replace(".dat", "").split("_")[2];
                String[] stackBandNames = Arrays.stream(stackProduct.getBandNames())
                        .filter(name->name.contains(year)).toArray(String[]::new);

                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
