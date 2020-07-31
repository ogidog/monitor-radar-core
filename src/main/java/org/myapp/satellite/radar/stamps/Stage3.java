
package org.myapp.satellite.radar.stamps;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;


public class Stage3 {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphDir = consoleParameters.get("graphDir").toString();
        String filesList1 = consoleParameters.get("filesList1").toString();
        String filesList2 = consoleParameters.get("filesList2").toString();


        String[] files1;
        String[] files2;
        try {
            files1 = Files.walk(Paths.get(filesList1)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            files2 = Files.walk(Paths.get(filesList2)).filter(path -> {
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

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String stampsexportDir = outputDir + "" + File.separator + "stampsexport";
        if (Files.exists(Paths.get(stampsexportDir))) {
            try {
                Files.walk(Paths.get(stampsexportDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }


        String stage3Dir = outputDir + "" + File.separator + "stage3";
        if (Files.exists(Paths.get(stage3Dir))) {
            try {
                Files.walk(Paths.get(stage3Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        new File(stampsexportDir).mkdirs();
        new File(stage3Dir).mkdirs();


        try {

            String graphFile = "stampsexport.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage3Dir + File.separator + "stage3.cmd", "UTF-8");

            for (int i = 0; i < files1.length; i++) {
                String fileName = Paths.get(files1[i]).getFileName().toString();
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files1[i]);
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(files2[i]);
                graph.getNode("StampsExport").getConfiguration().getChild("targetFolder")
                        .setValue(stampsexportDir);

                FileWriter fileWriter = new FileWriter(stage3Dir + File.separator + fileName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage3Dir + File.separator + fileName + ".xml");
            }


            cmdWriter.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


    }

}
