package org.myapp.satellite.radar.processing;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.IntStream;

public class Stage4 {

    public static void main(String[] args) {

        // String inputDir1 = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\subset";
        // String inputDir2 = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\topophaseremoval";
        // String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\stampsexport";
        // String graphsDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\graphs";

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String inputDir1 = consoleParameters.get("inputDir1").toString();
        String inputDir2 = consoleParameters.get("inputDir2").toString();
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphsDir = consoleParameters.get("graphsDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();

        try {

            String[] sourceProductsDirs1 = Files.find(Paths.get(inputDir1), 1, (path, attr) -> {
                return Character.isDigit(path.toString().charAt(path.toString().length() - 1));
            }).map(path -> path.toString()).sorted().toArray(String[]::new);

            String[] sourceProductsDirs2 = Files.find(Paths.get(inputDir2), 1, (path, attr) -> {
                return Character.isDigit(path.toString().charAt(path.toString().length() - 1));
            }).map(path -> path.toString()).sorted().toArray(String[]::new);

            String tmpDir = new File("").getAbsolutePath();

            IntStream.range(0, sourceProductsDirs1.length).forEach(index -> {

                try {

                    Reader fileReader = new FileReader(graphsDir + File.separator + "PSExport.xml");
                    Graph graph = GraphIO.read(fileReader);
                    fileReader.close();

                    graph.getNode("Read").getConfiguration().getChild("file").setValue(sourceProductsDirs1[index] + File.separator + "subset_master_Stack_Deb.dim");
                    graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(sourceProductsDirs2[index] + File.separator + "subset_master_Stack_Deb_ifg_dinsar.dim");
                    graph.getNode("StampsExport").getConfiguration().getChild("targetFolder").setValue(outputDir);

                    FileWriter fileWriter = new FileWriter(tmpDir + File.separator + "PSExport.xml");
                    GraphIO.write(graph, fileWriter);
                    fileWriter.flush();
                    fileWriter.close();

                    ProcessBuilder processBuilder =
                            new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt" +
                                    (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""),
                                    tmpDir + File.separator + "PSExport.xml", "-Dsnap.userdir=" + snapDir
                            ).inheritIO();
                    Process p = processBuilder.start();
                    p.waitFor();
                    p.destroy();

                    Files.deleteIfExists(Paths.get(tmpDir + File.separator + "PSExport.xml"));

                } catch (Exception e) {
                    e.printStackTrace();
                }

            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
