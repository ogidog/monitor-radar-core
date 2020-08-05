package org.myapp.satellite.radar.sbas;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
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

            String intfDir = outputDir + File.separator + "intf";
            if (Files.exists(Paths.get(intfDir))) {
                Files.walk(Paths.get(intfDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String stage3Dir = outputDir + "" + File.separator + "stage3";
            String date2NameFile = outputDir + "" + File.separator + "network" + File.separator + "date2Name.txt";
            String ifgListFile = outputDir + "" + File.separator + "network" + File.separator + "ifg_list.txt";

            HashMap<String,String> date2Name = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(date2NameFile));
            for(String line; (line = br.readLine()) != null; ) {
                date2Name.put(line.split(";")[0], line.split(";")[1]);
            }

            new File(intfDir).mkdirs();
            new File(stage3Dir).mkdirs();

            String graphFile = "filt_intf.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage3Dir + File.separator + "stage2.cmd", "UTF-8");

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
            parameters.put("geoRegionCoordinates", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}
