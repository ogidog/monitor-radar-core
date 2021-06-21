
package org.myapp.satellite.radar.ds;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;


public class Stage4 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String esdModifyDir = outputDir + File.separator + "esdmodify";
            String topophaseremovalDir = outputDir + File.separator + "topophaseremoval";

            String[] files1 = Files.walk(Paths.get(esdModifyDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String[] files2 = Files.walk(Paths.get(topophaseremovalDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            for (int i = 0; i < files1.length; i++) {
                Path path = Paths.get(files1[i]);
                Charset charset = StandardCharsets.UTF_8;
                String content = new String(Files.readAllBytes(path), charset);
                content = content.replaceAll("<NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>",
                        "<NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
                Files.write(path, content.getBytes(charset));

                path = Paths.get(files2[i]);
                content = new String(Files.readAllBytes(path), charset);
                content = content.replaceAll("<NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>",
                        "<NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
                Files.write(path, content.getBytes(charset));
            }

            String stampsexportDir = outputDir + "" + File.separator + "stampsexport";
            if (Files.exists(Paths.get(stampsexportDir))) {
                Files.walk(Paths.get(stampsexportDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }


            String stage4Dir = outputDir + "" + File.separator + "stage4";
            if (Files.exists(Paths.get(stage4Dir))) {
                Files.walk(Paths.get(stage4Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            new File(stampsexportDir).mkdirs();
            new File(stage4Dir).mkdirs();


            String graphFile = "stampsexport.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage4Dir + File.separator + "stage4.cmd", "UTF-8");

            for (int i = 0; i < files1.length; i++) {
                String fileName = Paths.get(files1[i]).getFileName().toString();
                String date = fileName.split("_")[5].split("T")[0];
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files1[i]);
                for (int j = 0; j < files2.length; j++) {
                    if (files2[j].contains(date)) {
                        graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(files2[j]);
                    }
                }
                graph.getNode("StampsExport").getConfiguration().getChild("targetFolder")
                        .setValue(stampsexportDir);

                FileWriter fileWriter = new FileWriter(stage4Dir + File.separator + fileName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage4Dir + File.separator + fileName + ".xml");
            }

            cmdWriter.close();

            return;

        } catch (
                Exception e) {
            e.printStackTrace();
            return;
        }


    }

}
