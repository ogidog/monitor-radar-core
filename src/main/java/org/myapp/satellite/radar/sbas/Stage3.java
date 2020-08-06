package org.myapp.satellite.radar.sbas;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Stage3 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String filesList = consoleParameters.get("filesList").toString();

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

            String stage3Dir = outputDir + "" + File.separator + "stage3";
            String intfDir = outputDir + File.separator + "intf";
            String applyorbitfileDir = outputDir + File.separator + "applyorbitfile";

            if (Files.exists(Paths.get(intfDir))) {
                Files.walk(Paths.get(intfDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(intfDir).mkdirs();
            new File(stage3Dir).mkdirs();

            String date2NameFile = outputDir + "" + File.separator + "network" + File.separator + "date2Name.txt";
            String ifgListFile = outputDir + "" + File.separator + "network" + File.separator + "ifg_list.txt";

            HashMap<String, String> date2Name = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(date2NameFile));
            for (String line; (line = br.readLine()) != null; ) {
                date2Name.put(line.split(";")[0], line.split(";")[1]);
            }

            String graphFile = "filt_intf.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            br = new BufferedReader(new FileReader(ifgListFile));
            String[] lines = br.lines().skip(2).toArray(String[]::new);

            PrintWriter cmdWriter = new PrintWriter(stage3Dir + File.separator + "stage3.cmd", "UTF-8");
            String masterProductDate, slaveProductDate, masterProductName, slaveProductName;
            for (int i = 0; i < lines.length; i++) {
                masterProductDate = lines[i].split(" ")[0].split("-")[0];
                slaveProductDate = lines[i].split(" ")[0].split("-")[1];
                masterProductName = date2Name.get(masterProductDate) + "_Orb.dim";
                slaveProductName = date2Name.get(slaveProductDate) + "_Orb.dim";

                graph.getNode("Read").getConfiguration().getChild("file").setValue(applyorbitfileDir + File.separator + masterProductName);
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(applyorbitfileDir + File.separator + slaveProductName);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(intfDir + File.separator + masterProductDate + "_" + slaveProductDate + "_intf.dim");
                graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                        .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

                FileWriter fileWriter = new FileWriter(stage3Dir + File.separator
                        + masterProductDate + "_" + slaveProductDate + "_intf.xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage3Dir + File.separator + masterProductDate + "_" + slaveProductDate + "_intf.xml");
            }
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}
