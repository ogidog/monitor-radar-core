package org.myapp.satellite.radar.mintpy;

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Stage0 {

    static Pattern datePattern = Pattern.compile("(\\d\\d\\d\\d\\d\\d\\d\\d)");

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String resultDir = consoleParameters.get("resultDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();
        String fileList = consoleParameters.get("filesList").toString();
        String bPerpCrit = consoleParameters.get("bPerpCrit").toString();
        String bTempCrit = consoleParameters.get("bTempCrit").toString();
        String proc = consoleParameters.get("proc").toString();

        switch (proc) {
            case "networkModel1":
                // 1: by the bPerpCrit, bTempCrit
                composeIntfPairs(workingDir, fileList, bPerpCrit, bTempCrit);
                break;
            case "networkModel2":
                // 2: by the by optimalmaster
                composeIntfPairs(workingDir, fileList);
                break;
            case "networkModel3":
                // 3: by the bPerpCrit, bTempCrit and two-way direction pairs
                composeIntfPairsExtended(workingDir, fileList, bPerpCrit, bTempCrit);
                break;
            case "networkModel4":
                // 4: by the optimal master get bperp, btemp, doplerdiff
                composeIntfPairsByOptimalMaster(resultDir, fileList);
                break;
            case "prepFiles":
                prepFiles(workingDir, resultDir, snapDir, fileList);
                break;
            default:
                System.out.println("No procedure selected.");
                break;
        }

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

    static void composeIntfPairsByOptimalMaster(String resultDir, String fileList) {

        try {
            Files.walk(Paths.get(resultDir + File.separator + "network"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            System.out.println(e);
        }
        try {
            new File(resultDir + File.separator + "network").mkdirs();
        } catch (Exception e) {
            System.out.println(e);
        }

        try {

            Product[] products = Arrays.stream(fileList.split(",")).map(file -> {
                try {
                    return ProductIO.readProduct(file);
                } catch (Exception e) {
                    e.printStackTrace();
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

            PrintWriter out = new PrintWriter(resultDir + File.separator + "network" + File.separator + "blList.txt");
            out.println(blList);
            out.close();

            out = new PrintWriter(resultDir + File.separator + "network" + File.separator + "date2Name.txt");
            out.println(dateToProductName);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void prepFiles(String workingDir, String resultDir, String snapDir, String fileList) {

        String configDir = resultDir + File.separator + "config";
        String graphDir = resultDir + File.separator + "graphs";

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            System.out.println("Fail to read parameters.");
            return;
        }

        String[] files = fileList.split(",");

        try {
            Files.walk(Paths.get(resultDir + File.separator + "applyorbitfile"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            new File(resultDir + File.separator + "applyorbitfile").mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String ifgListFile = resultDir + File.separator + "network" + File.separator + "ifg_list.txt";
        String date2NameFile = resultDir + File.separator + "network" + File.separator + "date2Name.txt";

        HashMap<String, String> date2NameMap = new HashMap<>();

        try {

            Stream<String> stream = Files.lines(Paths.get(date2NameFile), StandardCharsets.UTF_8);
            stream.forEach(s -> {
                date2NameMap.put(s.trim().split(";")[0], s.trim().split(";")[1]);
            });
            stream.close();

            stream = Files.lines(Paths.get(ifgListFile), StandardCharsets.UTF_8);
            String pairNames = stream.map(s -> {
                if (!s.contains("#")) {
                    String masterDate = s.split(" ")[0].split("-")[0];
                    String slaveDate = s.split(" ")[0].split("-")[1];
                    return date2NameMap.get(masterDate) + "," + date2NameMap.get(slaveDate);
                } else {
                    return "";
                }
            }).filter(s -> !s.equals("")).collect(Collectors.joining(";"));

            PrintWriter out = new PrintWriter(resultDir + File.separator + "network" + File.separator + "pairNames.txt");
            out.println(pairNames);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();

        Product targetProduct;
        Graph graph = null;
        try {
            FileReader fileReader = new FileReader(graphDir + File.separator + "applyorbitfile.xml");
            graph = GraphIO.read(fileReader);
            fileReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        graph.getNode("TOPSAR-Split").getConfiguration().getChild("selectedPolarisations").setValue(((HashMap) parameters.get("TOPSARSplit")).get("selectedPolarisations").toString());
        graph.getNode("Apply-Orbit-File").getConfiguration().getChild("orbitType").setValue(((HashMap) parameters.get("ApplyOrbitFile")).get("orbitType").toString());
        graph.getNode("Apply-Orbit-File").getConfiguration().getChild("polyDegree").setValue(((HashMap) parameters.get("ApplyOrbitFile")).get("polyDegree").toString());
        graph.getNode("Apply-Orbit-File").getConfiguration().getChild("continueOnFail").setValue(((HashMap) parameters.get("ApplyOrbitFile")).get("continueOnFail").toString());

        for (int i = 0; i < files.length; i++) {
            try {

                targetProduct = topsarSplitOpEnv.getTargetProduct(files[i], parameters);

                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topsarSplitOpEnv.getSubSwath());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topsarSplitOpEnv.getFirstBurstIndex());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topsarSplitOpEnv.getLastBurstIndex());
                graph.getNode("Write").getConfiguration().getChild("file").setValue(workingDir + File.separator + "applyorbitfile" + File.separator + topsarSplitOpEnv.getSourceProductName() + ".dim");

                FileWriter fileWriter = new FileWriter(resultDir + File.separator + "applyorbitfile" + File.separator + topsarSplitOpEnv.getSourceProductName() + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                targetProduct.closeIO();

            } catch (Exception e) {
                System.out.println(e);
            }

            topsarSplitOpEnv.Dispose();
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // TOPSARSplit
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "s1_tops_split.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("TOPSARSplit",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            fileReader.close();

            // ApplyOrbitFile
            fileReader = new FileReader(configDir + File.separator + "apply_orbit_file.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            parameters.put("polyDegree", Integer.valueOf(((HashMap) jsonParameters.get("polyDegree")).get("value").toString()));
            parameters.put("continueOnFail", Boolean.valueOf(((HashMap) jsonParameters.get("continueOnFail")).get("value").toString()));
            parameters.put("orbitType", ((HashMap) jsonParameters.get("orbitType")).get("value"));
            stageParameters.put("ApplyOrbitFile", parameters);

            fileReader.close();

            // Subset
            fileReader = new FileReader(configDir + File.separator + "subset.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            parameters = new HashMap();
            parameters.put("geoRegionCoordinates", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}
