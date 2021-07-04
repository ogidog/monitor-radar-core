package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.CustomErrorHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Stage4 {

    public static void main(String[] args) {

        if(CustomErrorHandler.checkPreviousErrors()){
            return;
        }

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String stage4Dir = outputDir + "" + File.separator + "stage4";
            String subsetDir = outputDir + File.separator + "subset";
            String stackDir = outputDir + File.separator + "stack";

            String[] files = Files.walk(Paths.get(subsetDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).sorted((file1,file2)->{
                String file1Name = file1.getFileName().toString().replace(".dim","").split("_")[1];
                String file2Name = file2.getFileName().toString().replace(".dim","").split("_")[1];
                return file1Name.compareTo(file2Name);
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            if (Files.exists(Paths.get(stackDir))) {
                Files.walk(Paths.get(stackDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stackDir).mkdirs();

            if (Files.exists(Paths.get(stage4Dir))) {
                Files.walk(Paths.get(stage4Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage4Dir).mkdirs();

            String graphFile = "createstack.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            String fileList = Arrays.stream(files).collect(Collectors.joining(","));
            graph.getNode("ProductSet-Reader").getConfiguration().getChild("fileList").setValue(fileList);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(stackDir + File.separator + "stack.dim");

            FileWriter fileWriter = new FileWriter(stage4Dir + File.separator + "stack.xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            PrintWriter cmdWriter = new PrintWriter(stage4Dir + File.separator + "stage4.cmd", "UTF-8");
            cmdWriter.println("gpt " + stage4Dir + File.separator + "stack.xml");
            cmdWriter.close();

        } catch (Exception e) {
            CustomErrorHandler.writeErrorToFile(e.getMessage(), "/mnt/task" + File.separator + "ERROR");
            e.printStackTrace();
        }
    }
}
