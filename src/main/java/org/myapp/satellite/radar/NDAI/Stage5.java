package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            String avgStdDir = outputDir + File.separator + "avgstd";

            String stackFile = stackDir + File.separator + "stack.dim";

            if (Files.exists(Paths.get(avgStdDir))) {
                Files.walk(Paths.get(avgStdDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(avgStdDir).mkdirs();

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

            String graphFile = "cohavgstd.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage5Dir + File.separator + "stage5.cmd", "UTF-8");
            FileWriter fileWriter = new FileWriter(stage5Dir + File.separator + "cohavgstd.xml");

            graph.getNode("Read").getConfiguration().getChild("file").setValue(stackFile);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(avgStdDir + File.separator + "cohavgstd.dim");
            int counter = 1;
            for (String year : yearList) {
                String filteredBands = Arrays.stream(stackBandNames).filter(bandName -> {
                    if (bandName.contains(year)) {
                        return true;
                    } else {
                        return false;
                    }
                }).collect(Collectors.joining(","));

                graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                        "avg(" + filteredBands + ")");
                graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name").setValue(
                        "avg_coh_" + year);
                graph.getNode("BandMaths(" + String.valueOf(counter + 3) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                        "stddev(" + filteredBands + ")");
                graph.getNode("BandMaths(" + String.valueOf(counter + 3) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name").setValue(
                        "std_coh_" + year);
                counter += 1;
            }

            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            cmdWriter.println("gpt " + stage5Dir + File.separator + "cohavgstd.xml");
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void process(String outputDir, String graphDir, String taskId) throws Exception {

        String taskDir = outputDir + "" + File.separator + taskId;
        String stage5Dir = taskDir + "" + File.separator + "stage5";
        String stackDir = taskDir + File.separator + "stack";
        String avgStdDir = taskDir + File.separator + "avgstd";

        String stackFile = stackDir + File.separator + "stack.dim";

        if (Files.exists(Paths.get(avgStdDir))) {
            Routines.deleteDir(new File(avgStdDir));
        }
        new File(avgStdDir).mkdirs();

        if (Files.exists(Paths.get(stage5Dir))) {
            Routines.deleteDir(new File(stage5Dir));
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

        String graphFile = "cohavgstd.xml";
        FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        graph.getNode("Read").getConfiguration().getChild("file").setValue(stackFile);
        graph.getNode("Write").getConfiguration().getChild("file")
                .setValue(avgStdDir + File.separator + "cohavgstd.dim");
        int counter = 1;
        for (String year : yearList) {
            String filteredBands = Arrays.stream(stackBandNames).filter(bandName -> {
                if (bandName.contains(year)) {
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.joining(","));

            graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                    "avg(" + filteredBands + ")");
            graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name").setValue(
                    "avg_coh_" + year);
            graph.getNode("BandMaths(" + String.valueOf(counter + 3) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                    "stddev(" + filteredBands + ")");
            graph.getNode("BandMaths(" + String.valueOf(counter + 3) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name").setValue(
                    "std_coh_" + year);
            counter += 1;
        }
        FileWriter fileWriter = new FileWriter(stage5Dir + File.separator + "cohavgstd.xml");
        GraphIO.write(graph, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        Routines.runGPTScript(  stage5Dir + File.separator + "cohavgstd.xml", "Stage5");
    }
}
