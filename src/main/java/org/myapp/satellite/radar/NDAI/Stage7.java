package org.myapp.satellite.radar.NDAI;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.dataio.ProductIO;
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

public class Stage7 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String stage7Dir = outputDir + "" + File.separator + "stage7";
            String tcDir = outputDir + File.separator + "tc";
            String subsetDir = outputDir + File.separator + "subset";

            String[] files = Files.walk(Paths.get(subsetDir)).filter(path -> {
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

            if (Files.exists(Paths.get(stage7Dir))) {
                Files.walk(Paths.get(stage7Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage7Dir).mkdirs();

            String graphFile = "tc.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage7Dir + File.separator + "stage4.cmd", "UTF-8");

            for (String file : files) {
                String fileName = Paths.get(file).getFileName().toString().replace(".dim", "");
                graph.getNode("Read").getConfiguration().getChild("file").setValue(file);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(tcDir + File.separator + fileName + ".dim");

                FileWriter fileWriter = new FileWriter(stage7Dir + File.separator
                        + fileName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage7Dir + File.separator + fileName + ".xml");
            }
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
