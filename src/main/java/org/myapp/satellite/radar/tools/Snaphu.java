package org.myapp.satellite.radar.tools;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.Common;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Snaphu {

    public static void main(String[] args) {

        String outputDir, filesList, taskId, resultDir = "";

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            outputDir = consoleParameters.get("outputDir").toString();
            resultDir = consoleParameters.get("resultDir").toString();
            filesList = consoleParameters.get("filesList").toString();
            taskId = consoleParameters.get("taskId").toString();

            String configDir = resultDir + File.separator + taskId + File.separator + "config";
            String graphDir = resultDir + File.separator + taskId + File.separator + "graphs";

            String taskDir = outputDir + File.separator + taskId;

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            String snaphuexportTaskDir = taskDir + "" + File.separator + "snaphuexport";
            if (Files.exists(Paths.get(snaphuexportTaskDir))) {
                Common.deleteDir(new File(snaphuexportTaskDir));
            }
            new File(snaphuexportTaskDir).mkdirs();

            String snaphuimportTaskDir = taskDir + "" + File.separator + "snaphuimport";
            if (Files.exists(Paths.get(snaphuimportTaskDir))) {
                Common.deleteDir(new File(snaphuimportTaskDir));
            }
            new File(snaphuimportTaskDir).mkdirs();

            String snaphuimportResultDir = resultDir + File.separator + taskId + File.separator + "public" + File.separator + "snaphuimport";
            if (Files.exists(Paths.get(snaphuimportResultDir))) {
                Common.deleteDir(new File(snaphuimportResultDir));
            }
            new File(snaphuimportResultDir).mkdirs();

            String graphFile = "snaphu_export.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph1 = GraphIO.read(fileReader);
            fileReader.close();

            graphFile = "snaphu_import.xml";
            fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph2 = GraphIO.read(fileReader);
            fileReader.close();


            for (int i = 0; i < files.length; i++) {
                Path path = Paths.get(files[i]);
                Charset charset = StandardCharsets.UTF_8;
                String content = new String(Files.readAllBytes(path), charset);
                content = content.replaceAll("<NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>",
                        "<NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
                Files.write(path, content.getBytes(charset));
            }

        } catch (Exception ex) {

            //TODO: delete
            ex.printStackTrace();
        }

    }
}
