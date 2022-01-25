
package org.myapp.satellite.radar.ds;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Stage4 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String configDir = consoleParameters.get("configDir").toString();

            String esdDir = outputDir + File.separator + "esd";
            String[] files = Files.walk(Paths.get(esdDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);


            String topophaseremovalDir = outputDir + "" + File.separator + "topophaseremoval";
            if (Files.exists(Paths.get(topophaseremovalDir))) {
                Files.walk(Paths.get(topophaseremovalDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String esdModifyDir = outputDir + "" + File.separator + "esdmodify";
            if (Files.exists(Paths.get(esdModifyDir))) {
                Files.walk(Paths.get(esdModifyDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String stage3Dir = outputDir + "" + File.separator + "stage3";
            if (Files.exists(Paths.get(stage3Dir))) {
                Files.walk(Paths.get(stage3Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            new File(esdModifyDir).mkdirs();
            new File(topophaseremovalDir).mkdirs();
            new File(stage3Dir).mkdirs();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            FileReader fileReader = new FileReader(graphDir + File.separator + "topophaseremoval.xml");
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");
            graph.getNode("Subset(2)").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

            // Interferogram
            ((HashMap) parameters.get("Interferogram")).forEach((key, value) -> {
                graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            // TopoPhaseRemoval
            ((HashMap) parameters.get("TopoPhaseRemoval")).forEach((key, value) -> {
                graph.getNode("TopoPhaseRemoval").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            PrintWriter cmdWriter = new PrintWriter(stage3Dir + File.separator + "stage3.cmd", "UTF-8");
            for (int i = 0; i < files.length; i++) {
                String fileName = Paths.get(files[i]).getFileName().toString().replace(".dim", "");
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(esdModifyDir + File.separator + fileName + "_Stack_Deb.dim");
                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(topophaseremovalDir + File.separator + fileName + "_Stack_Ifg_Deb_DInSAR.dim");

                FileWriter fileWriter = new FileWriter(stage3Dir + File.separator + fileName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage3Dir + File.separator + fileName + ".xml");
            }

            cmdWriter.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static void process(String outputDir, String configDir, String graphDir, String taskId) throws Exception {

        String taskDir = outputDir + File.separator + taskId;

        String esdDir = taskDir + File.separator + "esd";
        String[] files = Files.walk(Paths.get(esdDir)).filter(path -> {
            if (path.toString().endsWith(".dim")) {
                return true;
            } else {
                return false;
            }

        }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

        String topophaseremovalDir = taskDir + "" + File.separator + "topophaseremoval";
        if (Files.exists(Paths.get(topophaseremovalDir))) {
            Common.deleteDir(new File(topophaseremovalDir));
        }
        new File(topophaseremovalDir).mkdirs();

        String esdModifyDir = taskDir + "" + File.separator + "esdmodify";
        if (Files.exists(Paths.get(esdModifyDir))) {
            Common.deleteDir(new File(esdModifyDir));
        }
        new File(esdModifyDir).mkdirs();

        String stage4Dir = taskDir + "" + File.separator + "stage4";
        if (Files.exists(Paths.get(stage4Dir))) {
            Common.deleteDir(new File(stage4Dir));
        }
        new File(stage4Dir).mkdirs();

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            throw new Exception("Stage3: Fail to read parameters.");
        }

        FileReader fileReader = new FileReader(graphDir + File.separator + "topophaseremoval.xml");
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        // Subset
        graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");
        graph.getNode("Subset(2)").getConfiguration().getChild("geoRegion")
                .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

        // Interferogram
        ((HashMap) parameters.get("Interferogram")).forEach((key, value) -> {
            graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        // TopoPhaseRemoval
        ((HashMap) parameters.get("TopoPhaseRemoval")).forEach((key, value) -> {
            graph.getNode("TopoPhaseRemoval").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        for (int i = 0; i < files.length; i++) {
            String fileName = Paths.get(files[i]).getFileName().toString().replace(".dim", "");
            graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(esdModifyDir + File.separator + fileName + "_Stack_Deb.dim");
            graph.getNode("Write(2)").getConfiguration().getChild("file")
                    .setValue(topophaseremovalDir + File.separator + fileName + "_Stack_Ifg_Deb_DInSAR.dim");

            FileWriter fileWriter = new FileWriter(stage4Dir + File.separator + fileName + ".xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            Common.runGPTScript(stage4Dir + File.separator + fileName + ".xml","Stage4");

        }

    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            JSONParser parser = new JSONParser();
            stageParameters = new HashMap<>();

            // Subset
            FileReader fileReader = new FileReader(configDir + File.separator + "subset.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
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

            // Topo Phase Removal
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "topo_phase_removal.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("TopoPhaseRemoval", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
