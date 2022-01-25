package org.myapp.satellite.radar.sbas;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Stage7 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String configDir = consoleParameters.get("configDir").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String intfDir = outputDir + File.separator + "intf";
            String snaphuimportDir = outputDir + File.separator + "snaphuimport";

            String[] files1 = Files.walk(Paths.get(intfDir)).filter(file -> file.toString().endsWith(".dim"))
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String[] files2 = Files.walk(Paths.get(snaphuimportDir)).filter(file -> file.toString().endsWith(".dim"))
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String[][] pairs = new String[files1.length][2];
            for (int i = 0; i < files1.length; i++) {
                String date = Paths.get(files1[i]).getFileName().toString();
                date = date.substring(0, date.length() - 4);
                for (int j = 0; j < files2.length; j++) {
                    if (files2[j].contains(date)) {
                        pairs[i][0] = files1[i];
                        pairs[i][1] = files2[j];
                        break;
                    }
                }
            }

            String prepDir = outputDir + File.separator + "prep";
            String stage7Dir = outputDir + "" + File.separator + "stage7";
            if (Files.exists(Paths.get(stage7Dir))) {
                Files.walk(Paths.get(stage7Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(prepDir))) {
                Files.walk(Paths.get(prepDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage7Dir).mkdirs();
            new File(prepDir).mkdirs();

            FileReader fileReader = new FileReader(graphDir + File.separator + "tc.xml");
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Terrain Correction
            ((HashMap) parameters.get("TerrainCorrection")).forEach((key, value) -> {
                graph.getNode("Terrain-Correction").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
                graph.getNode("Terrain-Correction(2)").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
                graph.getNode("Terrain-Correction(4)").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            PrintWriter cmdWriter = new PrintWriter(stage7Dir + File.separator + "stage7.cmd", "UTF-8");
            Pattern p = Pattern.compile("[\\d]{1,}");
            for (int i = 0; i < files1.length; i++) {
                Product product = ProductIO.readProduct(files1[i]);
                String[] bandNames = product.getBandNames();
                product.closeIO();
                String[] iqBandNames = Arrays.stream(bandNames).filter(name -> name.contains("i_") || name.contains("q_"))
                        .toArray(String[]::new);
                String[] fileNameSplitted = Paths.get(files1[i]).getFileName().toString().replace(".dim", "").split("_");
                String datePair = Arrays.stream(fileNameSplitted).filter(str -> {
                    Matcher m = p.matcher(str);
                    if (m.matches()) {
                        return (true);
                    } else {
                        return (false);
                    }
                }).collect(Collectors.joining("_"));
                String pairDir = prepDir + File.separator + datePair;
                new File(pairDir).mkdirs();
                graph.getNode("Read").getConfiguration().getChild("file").setValue(pairs[i][0]);
                graph.getNode("BandMaths").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                        "atan2(" + iqBandNames[1] + "," + iqBandNames[0] + ")");
                graph.getNode("Write").getConfiguration().getChild("file").setValue(pairDir + File.separator + datePair + "_filt_int_sub_tc.dim");
                graph.getNode("Write(2)").getConfiguration().getChild("file").setValue(pairDir + File.separator + datePair + "_coh_tc.dim");
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(pairs[i][1]);
                graph.getNode("Write(4)").getConfiguration().getChild("file").setValue(pairDir + File.separator + datePair + "_unw_tc.dim");

                FileWriter fileWriter = new FileWriter(stage7Dir + File.separator
                        + datePair + "_tc.xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage7Dir + File.separator + datePair + "_tc.xml");
            }

            fileReader = new FileReader(graphDir + File.separator + "tc_dem.xml");
            Graph graph1 = GraphIO.read(fileReader);
            fileReader.close();

            graph1.getNode("Read").getConfiguration().getChild("file").setValue(files1[0]);
            graph1.getNode("Write").getConfiguration().getChild("file").setValue(prepDir + File.separator + "dem_tc.dim");
            FileWriter fileWriter = new FileWriter(stage7Dir + File.separator
                    + "dem_tc.xml");
            GraphIO.write(graph1, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            cmdWriter.println("gpt " + stage7Dir + File.separator + "dem_tc.xml");

            cmdWriter.close();

            Files.copy(Paths.get(configDir + File.separator + "smallbaselineApp.cfg"), Paths.get(prepDir + File.separator + "smallbaselineApp.cfg"));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static void process(String outputDir, String configDir, String graphDir, String taskId) throws Exception {

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            throw new Exception("Stage7: Fail to read parameters.");
        }

        String taskDir = outputDir + File.separator + taskId;
        String subsetDir = taskDir + File.separator + "subset";
        String snaphuimportDir = taskDir + File.separator + "snaphuimport";

        String[] files1 = Files.walk(Paths.get(subsetDir)).filter(file -> file.toString().endsWith(".dim"))
                .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

        String[] files2 = Files.walk(Paths.get(snaphuimportDir)).filter(file -> file.toString().endsWith(".dim"))
                .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

        String[][] pairs = new String[files1.length][2];
        for (int i = 0; i < files1.length; i++) {
            String date = Paths.get(files1[i]).getFileName().toString().replace(".dim","");
            for (int j = 0; j < files2.length; j++) {
                if (files2[j].contains(date)) {
                    pairs[i][0] = files1[i];
                    pairs[i][1] = files2[j];
                    break;
                }
            }
        }

        String prepDir = taskDir + File.separator + "prep";
        String stage7Dir = taskDir + "" + File.separator + "stage7";

        if (Files.exists(Paths.get(stage7Dir))) {
            Common.deleteDir(new File(stage7Dir));
        }
        new File(stage7Dir).mkdirs();

        if (Files.exists(Paths.get(prepDir))) {
            Common.deleteDir(new File(prepDir));
        }
        new File(prepDir).mkdirs();

        FileReader fileReader = new FileReader(graphDir + File.separator + "tc.xml");
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        // Terrain Correction
        ((HashMap) parameters.get("TerrainCorrection")).forEach((key, value) -> {
            graph.getNode("Terrain-Correction").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
            graph.getNode("Terrain-Correction(2)").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
            graph.getNode("Terrain-Correction(4)").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        Pattern p = Pattern.compile("[\\d]{1,}");
        for (int i = 0; i < files1.length; i++) {
            Product product = ProductIO.readProduct(files1[i]);
            String[] bandNames = product.getBandNames();
            product.closeIO();
            String[] iqBandNames = Arrays.stream(bandNames).filter(name -> name.contains("i_") || name.contains("q_"))
                    .toArray(String[]::new);
            String[] fileNameSplitted = Paths.get(files1[i]).getFileName().toString().replace(".dim", "").split("_");
            String datePair = Arrays.stream(fileNameSplitted).filter(str -> {
                Matcher m = p.matcher(str);
                if (m.matches()) {
                    return (true);
                } else {
                    return (false);
                }
            }).collect(Collectors.joining("_"));
            String pairDir = prepDir + File.separator + datePair;
            new File(pairDir).mkdirs();
            graph.getNode("Read").getConfiguration().getChild("file").setValue(pairs[i][0]);
            graph.getNode("BandMaths").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                    "atan2(" + iqBandNames[1] + "," + iqBandNames[0] + ")");
            graph.getNode("Write").getConfiguration().getChild("file").setValue(pairDir + File.separator + datePair + "_filt_int_sub_tc.dim");
            graph.getNode("Write(2)").getConfiguration().getChild("file").setValue(pairDir + File.separator + datePair + "_coh_tc.dim");
            graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(pairs[i][1]);
            graph.getNode("Write(4)").getConfiguration().getChild("file").setValue(pairDir + File.separator + datePair + "_unw_tc.dim");

            FileWriter fileWriter = new FileWriter(stage7Dir + File.separator
                    + datePair + "_tc.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            Common.runGPTScript(stage7Dir + File.separator + datePair + "_tc.xml", "Stage7");
        }

        fileReader = new FileReader(graphDir + File.separator + "tc_dem.xml");
        Graph graph1 = GraphIO.read(fileReader);
        fileReader.close();

        graph1.getNode("Read").getConfiguration().getChild("file").setValue(files1[0]);
        graph1.getNode("Write").getConfiguration().getChild("file").setValue(prepDir + File.separator + "dem_tc.dim");
        FileWriter fileWriter = new FileWriter(stage7Dir + File.separator
                + "dem_tc.xml");
        GraphIO.write(graph1, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        Common.runGPTScript(stage7Dir + File.separator + "dem_tc.xml", "Stage7");

        Files.copy(Paths.get(configDir + File.separator + "smallbaselineApp.cfg"), Paths.get(prepDir + File.separator + "smallbaselineApp.cfg"));

    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            JSONParser parser = new JSONParser();
            stageParameters = new HashMap<>();

            // TerrainCorrection
            parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "terrain_correction.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("TerrainCorrection", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
