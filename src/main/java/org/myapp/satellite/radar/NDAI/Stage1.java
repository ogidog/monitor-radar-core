package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.satellite.radar.shared.TOPSARSplitOpEnv;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Stage1 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String filesList = consoleParameters.get("filesList").toString();
            String taskid = consoleParameters.get("taskid").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            String stage1Dir = outputDir + "" + File.separator + "stage1";
            if (Files.exists(Paths.get(outputDir))) {
                Files.walk(Paths.get(outputDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            new File(outputDir).mkdirs();
            new File(outputDir + File.separator + "applyorbitfile").mkdirs();
            new File(stage1Dir).mkdirs();

            TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
            String graphFile = "applyorbitfile.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage1Dir + File.separator + "stage1.cmd", "UTF-8");

            for (int i = 0; i < files.length; i++) {

                topsarSplitOpEnv.getSplitParameters(files[i], parameters);

                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(outputDir + File.separator + "applyorbitfile" + File.separator
                                + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + "_Orb.dim");
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topsarSplitOpEnv.getSubSwath());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topsarSplitOpEnv.getFirstBurstIndex());
                graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topsarSplitOpEnv.getLastBurstIndex());

                FileWriter fileWriter = new FileWriter(stage1Dir + File.separator
                        + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage1Dir + File.separator
                        + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");

            }

            topsarSplitOpEnv.Dispose();
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String configDir, String graphDir, String filesList, String taskid) throws Exception {

        HashMap parameters = getParameters(configDir);
        if (parameters == null) {
            System.out.println("Fail to read parameters.");
            return;
        }

        String[] files;
        if (!filesList.contains(",")) {
            files = Files.walk(Paths.get(filesList)).skip(1)
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
        } else {
            files = filesList.split(",");
        }

        String taskDir = outputDir + File.separator + taskid;
        if (Files.exists(Paths.get(taskDir))) {
            Routines.deleteDir(new File(taskDir));
        }
        new File(taskDir).mkdir();
        String stage1Dir = taskDir + "" + File.separator + "stage1";
        new File(stage1Dir).mkdir();
        new File(taskDir + File.separator + "applyorbitfile").mkdir();

        TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
        String graphFile = "applyorbitfile.xml";
        FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        PrintWriter cmdWriter = new PrintWriter(stage1Dir + File.separator + "stage1.cmd", "UTF-8");

        for (int i = 0; i < files.length; i++) {

            topsarSplitOpEnv.getSplitParameters(files[i], parameters);

            graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(taskDir + File.separator + "applyorbitfile" + File.separator
                            + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + "_Orb.dim");
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("subswath").setValue(topsarSplitOpEnv.getSubSwath());
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("firstBurstIndex").setValue(topsarSplitOpEnv.getFirstBurstIndex());
            graph.getNode("TOPSAR-Split").getConfiguration().getChild("lastBurstIndex").setValue(topsarSplitOpEnv.getLastBurstIndex());

            FileWriter fileWriter = new FileWriter(stage1Dir + File.separator
                    + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            cmdWriter.println("gpt " + stage1Dir + File.separator
                    + Paths.get(files[i]).getFileName().toString().replace(".zip", "") + ".xml");

        }

        topsarSplitOpEnv.Dispose();
        cmdWriter.close();

    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // DataSet
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "dataset.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("DataSet",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

            // TOPSARSplit
            fileReader = new FileReader(configDir + File.separator + "s1_tops_split.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            jsonParameters1 = (HashMap) jsonObject.get("parameters");

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
            parameters.put("geoRegion", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }
}
