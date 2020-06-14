package org.myapp.satellite.radar.processing;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.runtime.Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

public class Stage2 {

    public static void main(String[] args) {

        /*inputDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\applyorbitfile"
        outputDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\subset"
        graphsDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\graphs"
        configDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\config"
        snapDir="F:\intellij-idea-workspace\monitor-radar-core-v3\.snap" masterName="S1A_IW_SLC__1SDV_20161229T002757_20161229T002826_014587_017B50_58CD"*/

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String inputDir = consoleParameters.get("inputDir").toString();
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphsDir = consoleParameters.get("graphsDir").toString();
        String configDir = consoleParameters.get("configDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();

        String masterName = consoleParameters.get("masterName").toString();
        int numOfProcesses = 2;

        Config.instance().preferences().put("snap.userdir", snapDir);

        try {

            String[] sourceProducts = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toString()).toArray(String[]::new);
            String masterProductFile = Arrays.stream(sourceProducts).filter(name -> name.contains(masterName)).findFirst().get();

            sourceProducts = Arrays.stream(sourceProducts).skip(1).toArray(String[]::new);

            IntStream.range(0, sourceProducts.length)
                    .forEach(pairNum -> {
                        new File(outputDir + File.separator + pairNum).mkdirs();
                    });

            String tmpDir = new File("").getAbsolutePath();

            HashMap stageParameters = getParameters(configDir);

            Reader fileReader = new FileReader(graphsDir + File.separator + "Subset.xml");
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            HashMap backGeocodingParameters = (HashMap) stageParameters.get("BackGeocoding");
            Iterator it = backGeocodingParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                graph.getNode("Back-Geocoding").getConfiguration().getChild(pair.getKey().toString()).setValue(pair.getValue().toString());
            }
            HashMap subsetParameters = (HashMap) stageParameters.get("Subset");
            graph.getNode("Subset").getConfiguration().getChild("geoRegion").setValue("POLYGON ((" + subsetParameters.get("geoRegion").toString() + "))");

            FileWriter fileWriter = new FileWriter(tmpDir + File.separator + "Subset.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            for (int index = 0; index < sourceProducts.length; index += numOfProcesses) {
                String[] slaveProductFiles = Arrays.stream(sourceProducts).skip(index).limit(numOfProcesses).toArray(String[]::new);
                runGraphParallel(masterProductFile, slaveProductFiles, tmpDir, outputDir, snapDir, index);
            }

            Files.deleteIfExists(Paths.get(tmpDir + File.separator + "Subset.xml"));

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static void runGraphParallel(String masterProductFile, String[] slaveProductFiles, String tmpDir, String outputDir, String snapDir, int sourceProductIndex) {

        IntStream.range(0, slaveProductFiles.length).parallel().forEach(index -> {
            try {

                FileReader fileReader1 = new FileReader(tmpDir + File.separator + "Subset.xml");
                Graph graph1 = GraphIO.read(fileReader1);
                fileReader1.close();

                graph1.getNode("ProductSet-Reader").getConfiguration().getChild("fileList").setValue(masterProductFile + ',' + slaveProductFiles[index]);
                graph1.getNode("Write").getConfiguration().getChild("file").setValue(outputDir + File.separator + (sourceProductIndex + index) + File.separator + "subset_master_Stack_Deb.dim");

                FileWriter fileWriter1 = new FileWriter(tmpDir + File.separator + "Subset" + (index) + ".xml");
                GraphIO.write(graph1, fileWriter1);
                fileWriter1.flush();
                fileWriter1.close();

                ProcessBuilder processBuilder = new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt" +
                        (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""),
                        tmpDir + File.separator + "Subset" + (index) + ".xml"
                ).inheritIO();
                Process p = processBuilder.start();
                p.waitFor();
                p.destroy();

                Files.deleteIfExists(Paths.get(tmpDir + File.separator + "Subset" + (index) + ".xml"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void _main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String inputDir = consoleParameters.get("inputDir").toString();
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphsDir = consoleParameters.get("graphsDir").toString();
        String configDir = consoleParameters.get("configDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();

        int filePatchesLength = Integer.valueOf(consoleParameters.get("filePatchesLength").toString());

        int numOfProcesses = 2;

        Config.instance().preferences().put("snap.userdir", snapDir);

        try {

            String[] sourceProducts = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toString()).sorted().toArray(String[]::new);
            String masterProductFile = sourceProducts[0];
            sourceProducts = Arrays.stream(sourceProducts).skip(1).toArray(String[]::new);

            IntStream.range(0, sourceProducts.length % filePatchesLength != 0 ? (sourceProducts.length / filePatchesLength) + 1 : sourceProducts.length / filePatchesLength)
                    .forEach(patchNum -> {
                        new File(outputDir + File.separator + (patchNum + 1)).mkdirs();
                    });

            ArrayList<String> filePatchesList = new ArrayList();
            filePatchesList.add(masterProductFile + "," + sourceProducts[0]);
            sourceProducts = Arrays.stream(sourceProducts).skip(1).sorted().toArray(String[]::new);

            int productsRemaining = sourceProducts.length;
            int productsProcessed = 0;

            while (productsRemaining > 0) {

                if (productsRemaining < filePatchesLength) {

                    String[] productFilesList = new String[productsRemaining + 1];
                    productFilesList[0] = masterProductFile;
                    System.arraycopy(Arrays.stream(sourceProducts).skip(productsProcessed).toArray(String[]::new), 0,
                            productFilesList, 1, productsRemaining);

                    filePatchesList.add(String.join(",", productFilesList));

                    productsProcessed += filePatchesLength;
                    productsRemaining = productsRemaining - productsProcessed;

                } else {

                    String[] productFilesList = new String[filePatchesLength + 1];
                    productFilesList[0] = masterProductFile;
                    System.arraycopy(Arrays.stream(sourceProducts).skip(productsProcessed).limit(filePatchesLength).toArray(String[]::new), 0,
                            productFilesList, 1, filePatchesLength);

                    filePatchesList.add(String.join(",", productFilesList));

                    productsProcessed += filePatchesLength;
                    productsRemaining = sourceProducts.length - productsProcessed;
                }
            }

            String tmpDir = new File("").getAbsolutePath();

            HashMap stageParameters = getParameters(configDir);

            Reader fileReader = new FileReader(graphsDir + File.separator + "Subset.xml");
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            HashMap backGeocodingParameters = (HashMap) stageParameters.get("BackGeocoding");
            Iterator it = backGeocodingParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                graph.getNode("Back-Geocoding").getConfiguration().getChild(pair.getKey().toString()).setValue(pair.getValue().toString());
            }
            HashMap subsetParameters = (HashMap) stageParameters.get("Subset");
            graph.getNode("Subset").getConfiguration().getChild("geoRegion").setValue("POLYGON ((" + subsetParameters.get("geoRegion").toString() + "))");

            FileWriter fileWriter = new FileWriter(tmpDir + File.separator + "Subset.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            IntStream.range(0, 1).forEach(index -> {
                try {
                    FileReader fileReader1 = new FileReader(tmpDir + File.separator + "Subset.xml");
                    Graph graph1 = GraphIO.read(fileReader1);
                    fileReader1.close();

                    graph1.getNode("ProductSet-Reader").getConfiguration().getChild("fileList").setValue(filePatchesList.get(index));
                    graph1.getNode("Write").getConfiguration().getChild("file").setValue(outputDir + File.separator + (index + 1) + File.separator + "subset_master_Stack_Deb.dim");

                    FileWriter fileWriter1 = new FileWriter(tmpDir + File.separator + "Subset" + (index + 1) + ".xml");
                    GraphIO.write(graph1, fileWriter1);
                    fileWriter1.flush();
                    fileWriter1.close();

                    ProcessBuilder processBuilder = new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt" +
                            (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""),
                            tmpDir + File.separator + "Subset" + (index + 1) + ".xml", "-Dsnap.userdir=" + snapDir
                    ).inheritIO();
                    Process p = processBuilder.start();
                    p.waitFor();
                    p.destroy();

                    Files.deleteIfExists(Paths.get(tmpDir + File.separator + "Subset" + (index + 1) + ".xml"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            IntStream.range(1, filePatchesList.size()).forEach(index -> {
                try {
                    FileReader fileReader1 = new FileReader(tmpDir + File.separator + "Subset.xml");
                    Graph graph1 = GraphIO.read(fileReader1);
                    fileReader1.close();

                    graph1.getNode("ProductSet-Reader").getConfiguration().getChild("fileList").setValue(filePatchesList.get(index));
                    graph1.getNode("Write").getConfiguration().getChild("file").setValue(outputDir + File.separator + (index + 1) + File.separator + "subset_master_Stack_Deb.dim");

                    FileWriter fileWriter1 = new FileWriter(tmpDir + File.separator + "Subset" + (index + 1) + ".xml");
                    GraphIO.write(graph1, fileWriter1);
                    fileWriter1.flush();
                    fileWriter1.close();

                    ProcessBuilder processBuilder = new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt" +
                            (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""),
                            tmpDir + File.separator + "Subset" + (index + 1) + ".xml", "-Dsnap.userdir=" + snapDir
                    ).inheritIO();
                    Process p = processBuilder.start();
                    p.waitFor();
                    p.destroy();

                    Files.deleteIfExists(Paths.get(tmpDir + File.separator + "Subset" + (index + 1) + ".xml"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Files.deleteIfExists(Paths.get(tmpDir + File.separator + "Subset.xml"));

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static HashMap getParameters(String configDir) {

        try {

            HashMap<String, HashMap> stageParameters = new HashMap<>();

            // BackGeocoding
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "back_geocoding.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("BackGeocoding", parameters);
            fileReader.close();

            // Subset
            parameters = new HashMap();
            fileReader = new FileReader(configDir + File.separator + "subset.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");
            parameters.put("geoRegion", ((HashMap) jsonParameters.get("geoRegion")).get("value").toString());
            stageParameters.put("Subset", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
