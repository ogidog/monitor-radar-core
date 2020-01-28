package org.myapp.satellite.radar.mintpy;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stage0 {

    static Pattern datePattern = Pattern.compile("(\\d\\d\\d\\d\\d\\d\\d\\d)");

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String fileList = consoleParameters.get("filesList").toString();
        String bPerpCrit = consoleParameters.get("bPerpCrit").toString();
        String bTempCrit = consoleParameters.get("bTempCrit").toString();
        String networkModel = consoleParameters.get("networkModel").toString();

        try {
            Files.walk(Paths.get(workingDir + File.separator + "network"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            System.out.println(e);
        }

        switch(networkModel){
            case "1":
                composeIntfPairs(workingDir, fileList, bPerpCrit, bTempCrit);
                break;
            case "2":
                composeIntfPairs(workingDir, fileList);
                break;
            case "3":
                composeIntfPairsExtended(workingDir, fileList, bPerpCrit, bTempCrit);
                break;
            default:
                System.out.println("No model selected.");
                break;
        }

        // 1: by the bPerpCrit, bTempCrit
        //composeIntfPairs(workingDir, fileList, bPerpCrit, bTempCrit);

        // 2: by the by optimalmaster
        // composeIntfPairs(workingDir, fileList);

        // 3: by the bPerpCrit, bTempCrit and two-way direction pairs
        // composeIntfPairsExtended(workingDir, fileList, bPerpCrit, bTempCrit);
    }

    static void composeIntfPairs(String workingDir, String fileList, String bPerpCrit, String bTempCrit) {

        String pairIDStr = "", pairNameStr = "", pairDateStr = "";
        TreeSet<String> productNames = new TreeSet<>();
        String masterProductName, slaveProductName, masterProductDate, slaveProductDate;

        InSARStackOverview.IfgPair[] masterSlavePairs;
        InSARStackOverview.IfgPair masterSlavePair;
        InSARStackOverview.IfgStack[] stackOverview;

        Product[] products = Arrays.stream(fileList.split(",")).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        try {

            stackOverview = InSARStackOverview.calculateInSAROverview(products);

            int counter = 0;

            for (int i = 0; i < stackOverview.length; i++) {
                masterSlavePairs = stackOverview[i].getMasterSlave();
                for (int j = i; j < masterSlavePairs.length; j++) {
                    masterSlavePair = masterSlavePairs[j];

                    double bPerp = masterSlavePair.getPerpendicularBaseline();
                    double bTemp = masterSlavePair.getTemporalBaseline();
                    if (masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString().equals(
                            masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString())) {
                        continue;
                    }

                    if (Math.abs(bTemp) <= Integer.valueOf(bTempCrit) && Math.abs(bPerp) <= Integer.valueOf(bPerpCrit)) {

                        Matcher dateMatcher = datePattern.matcher(masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                        dateMatcher.find();
                        masterProductDate = dateMatcher.group();

                        dateMatcher = datePattern.matcher(masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                        dateMatcher.find();
                        slaveProductDate = dateMatcher.group();

                        pairDateStr = pairDateStr + masterProductDate + "," + slaveProductDate + "," + bPerp + "," + bTemp + ";";

                        masterProductName = masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();
                        slaveProductName = masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();
                        productNames.add(masterProductName);
                        productNames.add(slaveProductName);

                        pairNameStr = pairNameStr + masterProductName + "," + slaveProductName + ";";

                        counter++;

                    }
                }
            }

            System.out.println("Total pairs: " + counter);

            Arrays.stream(products).forEach(product -> {
                try {
                    product.closeIO();
                } catch (IOException e) {
                    System.out.println(e);
                }
            });

            pairNameStr = pairNameStr.substring(0, pairNameStr.length() - 1);
            pairIDStr = pairNameStr;
            String[] productNamesStr = productNames.stream().toArray(String[]::new);

            for (int i = 0; i < productNamesStr.length; i++) {
                pairIDStr = pairIDStr.replace(productNamesStr[i], String.valueOf(i));
            }

            new File(workingDir + File.separator + "network").mkdirs();

            // Write pairs to file
            PrintWriter out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairNames.txt");
            out.println(pairNameStr);
            out.close();

            out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairDate.txt");
            out.println(pairDateStr);
            out.close();

            out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairID.txt");
            out.println(pairIDStr);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static void composeIntfPairsExtended(String workingDir, String fileList, String bPerpCrit, String bTempCrit) {

        String pairIDStr = "", pairNameStr = "", pairDateStr = "";
        TreeSet<String> productNames = new TreeSet<>();
        String masterProductName, slaveProductName, masterProductDate, slaveProductDate;

        InSARStackOverview.IfgPair[] masterSlavePairs;
        InSARStackOverview.IfgPair masterSlavePair;
        InSARStackOverview.IfgStack[] stackOverview;

        Product[] products = Arrays.stream(fileList.split(",")).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        try {

            stackOverview = InSARStackOverview.calculateInSAROverview(products);

            int counter = 0;

            for (int i = 0; i < stackOverview.length; i++) {
                masterSlavePairs = stackOverview[i].getMasterSlave();
                for (int j = 0; j < masterSlavePairs.length; j++) {
                    masterSlavePair = masterSlavePairs[j];

                    double bPerp = masterSlavePair.getPerpendicularBaseline();
                    double bTemp = masterSlavePair.getTemporalBaseline();
                    if (masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString().equals(
                            masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString())) {
                        continue;
                    }

                    if (Math.abs(bTemp) <= Integer.valueOf(bTempCrit) && Math.abs(bPerp) <= Integer.valueOf(bPerpCrit)) {

                        Matcher dateMatcher = datePattern.matcher(masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                        dateMatcher.find();
                        masterProductDate = dateMatcher.group();

                        dateMatcher = datePattern.matcher(masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString());
                        dateMatcher.find();
                        slaveProductDate = dateMatcher.group();

                        pairDateStr = pairDateStr + masterProductDate + "," + slaveProductDate + "," + bPerp + "," + bTemp + ";";

                        masterProductName = masterSlavePair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();
                        slaveProductName = masterSlavePair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString();
                        productNames.add(masterProductName);
                        productNames.add(slaveProductName);

                        pairNameStr = pairNameStr + masterProductName + "," + slaveProductName + ";";

                        counter++;

                    }
                }
            }

            System.out.println("Total pairs: " + counter);

            Arrays.stream(products).forEach(product -> {
                try {
                    product.closeIO();
                } catch (IOException e) {
                    System.out.println(e);
                }
            });

            pairNameStr = pairNameStr.substring(0, pairNameStr.length() - 1);
            pairIDStr = pairNameStr;
            String[] productNamesStr = productNames.stream().toArray(String[]::new);

            for (int i = 0; i < productNamesStr.length; i++) {
                pairIDStr = pairIDStr.replace(productNamesStr[i], String.valueOf(i));
            }

            new File(workingDir + File.separator + "network").mkdirs();

            // Write pairs to file
            PrintWriter out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairNames.txt");
            out.println(pairNameStr);
            out.close();

            out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairDate.txt");
            out.println(pairDateStr);
            out.close();

            out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairID.txt");
            out.println(pairIDStr);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static void composeIntfPairs(String workingDir, String fileList) {

        String masterProductName = "";
        String pairNameStr = "";

        Product[] products = Arrays.stream(fileList.split(",")).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        try {
            masterProductName = InSARStackOverview.findOptimalMasterProduct(products).getName();


            for (int i = 0; i < products.length; i++) {
                Product product = products[i];
                String slaveProductName = product.getName();
                if (!masterProductName.equals(slaveProductName)) {
                    pairNameStr = pairNameStr + masterProductName + "," + slaveProductName + ";";
                }
            }
            pairNameStr = pairNameStr.substring(0, pairNameStr.length() - 1);

            Arrays.stream(products).forEach(product -> {
                try {
                    product.closeIO();
                } catch (IOException e) {
                    System.out.println(e);
                }
            });

            new File(workingDir + File.separator + "network").mkdirs();

            // Write pairs to file
            PrintWriter out = new PrintWriter(workingDir + File.separator + "network" + File.separator + "pairNames.txt");
            out.println(pairNameStr);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}