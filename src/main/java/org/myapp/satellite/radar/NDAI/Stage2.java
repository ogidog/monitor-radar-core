package org.myapp.satellite.radar.NDAI;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Stage2 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String stage2Dir = outputDir + "" + File.separator + "stage2";
            String esdDir = outputDir + File.separator + "esd";
            String applyorbitfileDir = outputDir + File.separator + "applyorbitfile";

            String[] files = Files.walk(Paths.get(applyorbitfileDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString())
                    .sorted((file1, file2) -> {
                        String file1Name = Paths.get(file1).getFileName().toString().split("_")[5].split("T")[0];
                        String file2Name = Paths.get(file2).getFileName().toString().split("_")[5].split("T")[0];
                        return file1Name.compareTo(file2Name);
                    }).toArray(String[]::new);

            if (Files.exists(Paths.get(esdDir))) {
                Files.walk(Paths.get(esdDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(esdDir).mkdirs();

            if (Files.exists(Paths.get(stage2Dir))) {
                Files.walk(Paths.get(stage2Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage2Dir).mkdirs();

            ArrayList<String[]> pairs = new ArrayList<>();
            for (int i = 0; i < files.length - 2; i++) {
                for (int j = i + 1; j < i + 2; j++) {
                    pairs.add(new String[]{files[i], files[j]});
                }
            }

            Sentinel1Utils s1u = new Sentinel1Utils(ProductIO.readProduct(files[0]));
            int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);
            String graphFile;
            if (numOfBurst > 1) {
                graphFile = "esd.xml";
            } else {
                graphFile = "backgeocoding.xml";
            }

            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // BackGeocoding
            ((HashMap) parameters.get("BackGeocoding")).forEach((key, value) -> {
                graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");
            String masterProductDate, slaveProductDate;

            for (String[] pair : pairs) {
                masterProductDate = Paths.get(pair[0]).getFileName().toString();
                slaveProductDate = Paths.get(pair[1]).getFileName().toString();
                masterProductDate = masterProductDate.split("T")[0].split("_")[5];
                slaveProductDate = slaveProductDate.split("T")[0].split("_")[5];
                String masterProductYear = masterProductDate.substring(0, 4);
                String slaveProductYear = slaveProductDate.substring(0, 4);
                if (!masterProductYear.equals(slaveProductYear)) {
                    continue;
                }

                graph.getNode("Read").getConfiguration().getChild("file").setValue(pair[0]);
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(pair[1]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(esdDir + File.separator + masterProductDate + "_" + slaveProductDate + ".dim");

                FileWriter fileWriter = new FileWriter(stage2Dir + File.separator
                        + masterProductDate + "_" + slaveProductDate + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage2Dir + File.separator + masterProductDate + "_" + slaveProductDate + ".xml");
            }
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String configDir, String graphDir, String taskId) throws Exception {

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            throw new Exception("Stage2: Fail to read parameters.");
        }

        String taskDir = outputDir + File.separator + taskId;
        String stage2Dir = taskDir + "" + File.separator + "stage2";
        String esdDir = taskDir + File.separator + "esd";
        String applyorbitfileDir = taskDir + File.separator + "applyorbitfile";

        String[] files = Files.walk(Paths.get(applyorbitfileDir)).filter(path -> {
            if (path.toString().endsWith(".dim")) {
                return true;
            } else {
                return false;
            }
        }).map(path -> path.toAbsolutePath().toString())
                .sorted((file1, file2) -> {
                    String file1Name = Paths.get(file1).getFileName().toString().split("_")[5].split("T")[0];
                    String file2Name = Paths.get(file2).getFileName().toString().split("_")[5].split("T")[0];
                    return file1Name.compareTo(file2Name);
                }).toArray(String[]::new);

        if (Files.exists(Paths.get(esdDir))) {
            Routines.deleteDir(new File(esdDir));
        }
        new File(esdDir).mkdirs();

        if (Files.exists(Paths.get(stage2Dir))) {
            Routines.deleteDir(new File(stage2Dir));
        }
        new File(stage2Dir).mkdirs();

        ArrayList<String[]> pairs = new ArrayList<>();
        for (int i = 0; i < files.length - 2; i++) {
            for (int j = i + 1; j < i + 2; j++) {
                pairs.add(new String[]{files[i], files[j]});
            }
        }

        Sentinel1Utils s1u = new Sentinel1Utils(ProductIO.readProduct(files[0]));
        int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);
        String graphFile;
        if (numOfBurst > 1) {
            graphFile = "esd.xml";
        } else {
            graphFile = "backgeocoding.xml";
        }

        FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        // BackGeocoding
        ((HashMap) parameters.get("BackGeocoding")).forEach((key, value) -> {
            graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        String masterProductDate, slaveProductDate;
        for (String[] pair : pairs) {
            masterProductDate = Paths.get(pair[0]).getFileName().toString();
            slaveProductDate = Paths.get(pair[1]).getFileName().toString();
            masterProductDate = masterProductDate.split("T")[0].split("_")[5];
            slaveProductDate = slaveProductDate.split("T")[0].split("_")[5];
            String masterProductYear = masterProductDate.substring(0, 4);
            String slaveProductYear = slaveProductDate.substring(0, 4);
            if (!masterProductYear.equals(slaveProductYear)) {
                continue;
            }

            graph.getNode("Read").getConfiguration().getChild("file").setValue(pair[0]);
            graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(pair[1]);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(esdDir + File.separator + masterProductDate + "_" + slaveProductDate + ".dim");

            FileWriter fileWriter = new FileWriter(stage2Dir + File.separator
                    + masterProductDate + "_" + slaveProductDate + ".xml");

            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            Routines.runGPTScript(stage2Dir + File.separator + masterProductDate + "_" + slaveProductDate + ".xml", "Stage2");

        }

    }

    static HashMap getParameters(String configDir) {

        try {
            HashMap<String, HashMap> stageParameters = new HashMap<>();


            // BackGeocoding
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "back_geocoding.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("BackGeocoding", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
