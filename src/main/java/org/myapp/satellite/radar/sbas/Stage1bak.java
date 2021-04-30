package org.myapp.satellite.radar.sbas;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stage1bak {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String filesList = consoleParameters.get("filesList").toString();

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            if (Files.exists(Paths.get(outputDir))) {
                Files.walk(Paths.get(outputDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
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
            }).sorted((e1, e2) -> e1.getStartTime().getAsDate().compareTo(e2.getStartTime().getAsDate())).toArray(Product[]::new);

            InSARStackOverview.IfgPair[] masterSlavePairs;
            InSARStackOverview.IfgStack[] stackOverview;
            InSARStackOverview.IfgPair masterSlavePair;

            String optimalMasterName = products[0].getName();
            String masterProductName, slaveProductName;
            String masterProductDate = "", slaveProductDate;
            String blList = "", dateToProductName = "";


            String content = new String(Files.readAllBytes(Paths.get(configDir + File.separator + "selectNetwork.template")));
            if (content.contains("sequential")) {
                optimalMasterName = products[0].getName();
            }
            if (content.contains("delaunay")) {
                optimalMasterName = InSARStackOverview.findOptimalMasterProduct(products).getName();
                stackOverview = InSARStackOverview.calculateInSAROverview(products);

                Pattern datePattern = Pattern.compile("(\\d\\d\\d\\d\\d\\d\\d\\d)");

                for (int i = 0; i < stackOverview.length; i++) {
                    masterSlavePairs = stackOverview[i].getMasterSlave();
                    for (int j = 0; j < masterSlavePairs.length; j++) {
                        masterSlavePair = masterSlavePairs[j];

                        masterProductName = masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();
                        slaveProductName = masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();

                        if (optimalMasterName.equals(masterProductName) && !optimalMasterName.equals(slaveProductName)) {

                            double bPerp = masterSlavePair.getPerpendicularBaseline();
                            double dopplerDiff = masterSlavePair.getDopplerDifference();

                            Matcher dateMatcher = datePattern.matcher(masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                            dateMatcher.find();
                            masterProductDate = dateMatcher.group().substring(2);

                            dateMatcher = datePattern.matcher(masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                            dateMatcher.find();
                            slaveProductDate = dateMatcher.group().substring(2);

                            blList = blList + slaveProductDate + " " + bPerp + " " + dopplerDiff + "\n";

                            dateToProductName = dateToProductName + slaveProductDate + ";" + slaveProductName + "\n";
                        }
                    }
                }
                blList = masterProductDate + " 0.0 0.0\n" + blList;
                blList = blList.trim();

                dateToProductName = masterProductDate + ";" + optimalMasterName + "\n" + dateToProductName;
                dateToProductName = dateToProductName.trim();

                PrintWriter out = new PrintWriter(outputDir + File.separator + "network" + File.separator + "blList.txt");
                out.println(blList);
                out.close();

                out = new PrintWriter(outputDir + File.separator + "network" + File.separator + "date2Name.txt");
                out.println(dateToProductName);
                out.close();

                Files.copy(Paths.get(configDir + File.separator + "selectNetwork.template"),
                        Paths.get(outputDir + File.separator + "network" + File.separator + "selectNetwork.template"));
            }


        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }
}
