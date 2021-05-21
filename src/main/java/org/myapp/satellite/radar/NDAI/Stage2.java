package org.myapp.satellite.radar.NDAI;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
            String tcDir = outputDir + File.separator + "tc";
            String applyorbitfileDir = outputDir + File.separator + "applyorbitfile";

            String[] files = Files.walk(Paths.get(applyorbitfileDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            if (Files.exists(Paths.get(tcDir))) {
                Files.walk(Paths.get(tcDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(tcDir).mkdirs();

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
                graphFile = "tc.xml";
            } else {
                graphFile = "tc_without_esd.xml";
            }

            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            // Subset
            graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                    .setValue("POLYGON((" + ((HashMap) parameters.get("Subset")).get("geoRegion").toString() + "))");

            // BackGeocoding
            ((HashMap) parameters.get("BackGeocoding")).forEach((key, value) -> {
                graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            // Interferogram
            ((HashMap) parameters.get("Interferogram")).forEach((key, value) -> {
                graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                        .setValue(value.toString());
            });

            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");
            String masterProductDate, slaveProductDate, masterProductName, slaveProductName;

            for (String[] pair : pairs) {
                masterProductDate = Paths.get(pair[0]).getFileName().toString();
                slaveProductDate = Paths.get(pair[1]).getFileName().toString();
                masterProductDate = masterProductDate.split("T")[0].split("_")[5];
                slaveProductDate = slaveProductDate.split("T")[0].split("_")[5];

                graph.getNode("Read").getConfiguration().getChild("file").setValue(pair[0] + ".dim");
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(pair[1] + ".dim");
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(tcDir + File.separator + masterProductDate + "_" + slaveProductDate + ".dim");

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
            return;
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

            // BackGeocoding
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "back_geocoding.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("BackGeocoding", parameters);
            fileReader.close();

            // Interferogram Formation
            parser = new JSONParser();
            fileReader = new FileReader(configDir + File.separator + "interferogram_formation.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            it = jsonParameters.entrySet().iterator();
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
