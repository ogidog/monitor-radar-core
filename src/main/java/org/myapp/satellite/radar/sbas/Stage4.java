package org.myapp.satellite.radar.sbas;

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
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String stage4Dir = outputDir + "" + File.separator + "stage4";
            String snaphuexportDir = outputDir + File.separator + "snaphuexport";
            String intfDir = outputDir + File.separator + "intf";

            String[] files = Files.walk(Paths.get(intfDir)).filter(file -> file.toString().endsWith(".dim"))
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            for (int i = 0; i < files.length; i++) {
                Path path = Paths.get(files[i]);
                Charset charset = StandardCharsets.UTF_8;
                String content = new String(Files.readAllBytes(path), charset);
                content = content.replaceAll("<NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>",
                        "<NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
                Files.write(path, content.getBytes(charset));
            }

            if (Files.exists(Paths.get(snaphuexportDir))) {
                Files.walk(Paths.get(snaphuexportDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(stage4Dir))) {
                Files.walk(Paths.get(stage4Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(snaphuexportDir).mkdirs();
            new File(stage4Dir).mkdirs();

            String graphFile = "snaphu_export.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage4Dir + File.separator + "stage4.cmd", "UTF-8");
            for (int i = 0; i < files.length; i++) {
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("SnaphuExport").getConfiguration().getChild("targetFolder")
                        .setValue(snaphuexportDir);
                FileWriter fileWriter = new FileWriter(stage4Dir + File.separator
                        + Paths.get(files[i]).getFileName().toString().replace(".dim", ".xml"));
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage4Dir + File.separator + Paths.get(files[i]).getFileName().toString().replace(".dim", ".xml"));
            }
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}
