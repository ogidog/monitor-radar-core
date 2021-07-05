package org.myapp.satellite.radar.NDAI;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.CustomErrorHandler;
import org.myapp.utils.Routines;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Stage3 {

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

            String stage3Dir = outputDir + "" + File.separator + "stage3";
            String subsetDir = outputDir + File.separator + "subset";
            String esdDir = outputDir + File.separator + "esd";

            String[] files = Files.walk(Paths.get(esdDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            if (Files.exists(Paths.get(subsetDir))) {
                Files.walk(Paths.get(subsetDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(subsetDir).mkdirs();

            if (Files.exists(Paths.get(stage3Dir))) {
                Files.walk(Paths.get(stage3Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage3Dir).mkdirs();

            String graphFile = "subset.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

            // Interferogram
            ((HashMap) parameters.get("Interferogram")).forEach((key, value) -> {
                graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            PrintWriter cmdWriter = new PrintWriter(stage3Dir + File.separator + "stage3.cmd", "UTF-8");
            String masterProductDate, slaveProductDate;

            for (String file : files) {
                String fileName = Paths.get(file).getFileName().toString().replace(".dim", "");
                graph.getNode("Read").getConfiguration().getChild("file").setValue(file);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(subsetDir + File.separator + fileName + ".dim");

                FileWriter fileWriter = new FileWriter(stage3Dir + File.separator
                        + fileName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage3Dir + File.separator + fileName + ".xml");
            }
            cmdWriter.close();

        } catch (Exception e) {
            CustomErrorHandler.writeErrorToFile(e.getMessage(), "/mnt/task" + File.separator + "ERROR");
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String configDir, String graphDir, String taskId) throws Exception {

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            System.out.println("Fail to read parameters.");
            return;
        }

        String taskDir = outputDir + File.separator + taskId;
        String stage3Dir = taskDir + "" + File.separator + "stage3";
        String subsetDir = taskDir + File.separator + "subset";
        String esdDir = taskDir + File.separator + "esd";

        String[] files = Files.walk(Paths.get(esdDir)).filter(path -> {
            if (path.toString().endsWith(".dim")) {
                return true;
            } else {
                return false;
            }
        }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

        if (Files.exists(Paths.get(subsetDir))) {
            Routines.deleteDir(new File(subsetDir));
        }
        new File(subsetDir).mkdirs();

        if (Files.exists(Paths.get(stage3Dir))) {
            Routines.deleteDir(new File(stage3Dir));
        }
        new File(stage3Dir).mkdirs();

        String graphFile = "subset.xml";
        FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        // Subset
        graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

        // Interferogram
        ((HashMap) parameters.get("Interferogram")).forEach((key, value) -> {
            graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        PrintWriter cmdWriter = new PrintWriter(stage3Dir + File.separator + "stage3.cmd", "UTF-8");
        String masterProductDate, slaveProductDate;

        for (String file : files) {
            String fileName = Paths.get(file).getFileName().toString().replace(".dim", "");
            graph.getNode("Read").getConfiguration().getChild("file").setValue(file);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(subsetDir + File.separator + fileName + ".dim");

            FileWriter fileWriter = new FileWriter(stage3Dir + File.separator
                    + fileName + ".xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            ProcessBuilder pb = new ProcessBuilder(Routines.getGPTScriptName(), stage3Dir + File.separator
                    + fileName + ".xml");
            pb.inheritIO();
            Process process = pb.start();
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                // check for errors
                new BufferedInputStream(process.getErrorStream());
                throw new RuntimeException("execution of script failed!");
            }
        }
    }

    static HashMap getParameters(String configDir) {

        try {
            HashMap<String, HashMap> stageParameters = new HashMap<>();

            // Subset
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "subset.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters = (HashMap) jsonObject.get("parameters");

            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            HashMap parameters = new HashMap();
            parameters.put("geoRegion", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

            // Interferogram Formation
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "interferogram_formation.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("Interferogram", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
