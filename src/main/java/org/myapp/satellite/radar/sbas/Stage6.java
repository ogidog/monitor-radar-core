package org.myapp.satellite.radar.sbas;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

public class Stage6 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String filesList1 = consoleParameters.get("filesList1").toString();
            String filesList2 = consoleParameters.get("filesList2").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String[] files1;
            if (!filesList1.contains(",")) {
                files1 = Files.walk(Paths.get(filesList1)).filter(file -> file.toString().endsWith(".dim"))
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files1 = filesList1.split(",");
            }

            String[] files2;
            if (!filesList1.contains(",")) {
                files2 = Files.walk(Paths.get(filesList2)).filter(file -> file.toString().endsWith(".hdr") && file.toString().contains("UnwPhase"))
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files2 = filesList1.split(",");
            }


            String snaphuimportDir = outputDir + File.separator + "snaphu_import";
            String stage6Dir = outputDir + "" + File.separator + "stage6";
            if (Files.exists(Paths.get(stage6Dir))) {
                Files.walk(Paths.get(stage6Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(snaphuimportDir))) {
                Files.walk(Paths.get(snaphuimportDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage6Dir).mkdirs();
            new File(snaphuimportDir).mkdirs();

            String graphFile = "snaphu_import.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage6Dir + File.separator + "stage6.cmd", "UTF-8");
            for (int i = 0; i < files1.length; i++) {
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files1[i]);
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(files2[i]);
                graph.getNode("Write").getConfiguration().getChild("file").setValue(snaphuimportDir
                        + File.separator + Paths.get(files1[i]).getFileName().toString().replace(".dim", "_unw.dim"));

                FileWriter fileWriter = new FileWriter(stage6Dir + File.separator
                        + Paths.get(files1[i]).getFileName().toString().replace(".dim", ".xml"));
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage6Dir + File.separator + Paths.get(files1[i]).getFileName().toString().replace(".dim", ".xml"));
            }

            cmdWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

}
