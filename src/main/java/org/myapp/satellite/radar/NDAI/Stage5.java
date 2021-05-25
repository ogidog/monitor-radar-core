package org.myapp.satellite.radar.NDAI;

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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Stage5 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String stage5Dir = outputDir + "" + File.separator + "stage5";
            String stackDir = outputDir + File.separator + "stack";
            String avgCohDir = outputDir + File.separator + "avgcoh";

            String stackFile = stackDir + File.separator + "stack.dim";

            if (Files.exists(Paths.get(avgCohDir))) {
                Files.walk(Paths.get(avgCohDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(avgCohDir).mkdirs();

            if (Files.exists(Paths.get(stage5Dir))) {
                Files.walk(Paths.get(stage5Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage5Dir).mkdirs();

            Product stackProduct = ProductIO.readProduct(stackFile);
            String[] stackBandNames = stackProduct.getBandNames();
            stackProduct.closeIO();
            stackProduct.dispose();

            String[] yearList = Arrays.stream(stackBandNames).map(bandName -> {
                Matcher m = Pattern.compile("(_)(\\d{2})(\\w{3})(\\d{4})(_)").matcher(bandName);
                m.find();
                return m.group(4);
            }).distinct().toArray(String[]::new);

            String graphFile = "avgcohband.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage5Dir + File.separator + "stage5.cmd", "UTF-8");
            for (String year : yearList) {
                String filteredBands = Arrays.stream(stackBandNames).filter(bandName -> {
                    if (bandName.contains(year)) {
                        return true;
                    } else {
                        return false;
                    }
                }).collect(Collectors.joining(","));

                graph.getNode("Read").getConfiguration().getChild("file").setValue(stackFile);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(avgCohDir + File.separator + "avgcohband" + year + ".dim");
                graph.getNode("BandMaths").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                        "avg(" + filteredBands + ")");

                FileWriter fileWriter = new FileWriter(stage5Dir + File.separator + "avgcohband" + year + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage5Dir + File.separator + "avgcohband" + year + ".xml");
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
