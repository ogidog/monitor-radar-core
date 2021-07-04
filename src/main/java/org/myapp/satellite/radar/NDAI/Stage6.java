package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.CustomErrorHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Stage6 {

    public static void main(String[] args) {

        if(CustomErrorHandler.checkPreviousErrors()){
            return;
        }

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String stage6Dir = outputDir + "" + File.separator + "stage6";
            String avgStdDir = outputDir + File.separator + "avgstd";
            String stablePointDir = outputDir + File.separator + "stablepoints";

            String avgStdFile = avgStdDir + File.separator + "cohavgstd.dim";

            if (Files.exists(Paths.get(stablePointDir))) {
                Files.walk(Paths.get(stablePointDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stablePointDir).mkdirs();

            if (Files.exists(Paths.get(stage6Dir))) {
                Files.walk(Paths.get(stage6Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage6Dir).mkdirs();

            Product avgStdProduct = ProductIO.readProduct(avgStdFile);
            String[] avgStdBandNames = avgStdProduct.getBandNames();
            avgStdProduct.closeIO();
            avgStdProduct.dispose();

            String[] yearList = Arrays.stream(avgStdBandNames).map(bandName -> {
                return bandName.split("_")[2];
            }).distinct().sorted(Comparator.reverseOrder()).toArray(String[]::new);

            String graphFile = "stablepoints.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage6Dir + File.separator + "stage6.cmd", "UTF-8");
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(stage6Dir + File.separator + "stablepoints.xml"));

            graph.getNode("Read").getConfiguration().getChild("file").setValue(avgStdFile);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(stablePointDir + File.separator + "stablepoints.dim");
            int counter = 1;
            for (String year : yearList) {
                String[] filteredBands = Arrays.stream(avgStdBandNames).filter(bandName -> {
                    if (bandName.contains(year)) {
                        return true;
                    } else {
                        return false;
                    }
                }).toArray(String[]::new);

                graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                        filteredBands[0] + " > 0.8 and " + filteredBands[1] + " < 0.2");
                graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name").setValue(
                        "stable_points_" + year);
                counter += 1;
            }
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            cmdWriter.println("gpt " + stage6Dir + File.separator + "stablepoints.xml");
            cmdWriter.close();

        } catch (Exception e) {
            CustomErrorHandler.writeErrorToFile(e.getMessage(), "/mnt/task" + File.separator + "ERROR");
            e.printStackTrace();
        }
    }
}
