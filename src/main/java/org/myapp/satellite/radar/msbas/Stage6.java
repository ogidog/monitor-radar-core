package org.myapp.satellite.radar.msbas;

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
            String graphDir = consoleParameters.get("graphDir").toString();

            String snaphuexportDir = outputDir + File.separator + "snaphuexport";
            String intfDir = outputDir + File.separator + "intf";

            String[] files1 = Files.walk(Paths.get(intfDir)).filter(file -> file.toString().endsWith(".dim"))
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String[] files2 = Files.walk(Paths.get(snaphuexportDir)).filter(file -> file.toString().endsWith(".hdr") && file.toString().contains("UnwPhase"))
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String[][] pairs = new String[files1.length][2];
            for (int i = 0; i < files1.length; i++) {
                String date = Paths.get(files1[i]).getFileName().toString().replace(".dim", "");
                date = date.substring(0, date.length() - 4);
                for (int j = 0; j < files2.length; j++) {
                    if (files2[j].contains(date)) {
                        pairs[i][0] = files1[i];
                        pairs[i][1] = files2[j];
                        break;
                    }
                }
            }

            String snaphuimportDir = outputDir + File.separator + "snaphuimport";
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
                graph.getNode("Read").getConfiguration().getChild("file").setValue(pairs[i][0]);
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(pairs[i][1]);
                graph.getNode("Write").getConfiguration().getChild("file").setValue(snaphuimportDir
                        + File.separator + Paths.get(files1[i]).getFileName().toString().replace(".dim", ".dim"));

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