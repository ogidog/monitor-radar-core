package org.myapp.satellite.radar.processing;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.IntStream;

public class Stage3 {

    public static void main(String[] args) {

        String inputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\data\\subset";
        String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\data\\topophaseremoval";
        String graphsDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\data\\graphs";

        try {

            String[] sourceProductsDirs = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                return Character.isDigit(path.toString().charAt(path.toString().length() - 1));
            }).map(path -> path.toString()).toArray(String[]::new);

            IntStream.range(0, sourceProductsDirs.length).parallel().forEach(index -> {
                new File(outputDir + File.separator + (index + 1)).mkdirs();

                try {

                    Reader fileReader = new FileReader(graphsDir + File.separator + "TopoPhaseRemoval.xml");
                    Graph graph = GraphIO.read(fileReader);
                    fileReader.close();

                    graph.getNode("Read").getConfiguration().getChild("file").setValue(sourceProductsDirs[index] + File.separator + "subset_master_Stack_Deb.dim");
                    graph.getNode("Write").getConfiguration().getChild("file").setValue(outputDir + File.separator + (index + 1) + File.separator + "subset_master_Stack_Deb_ifg_dinsar.dim");

                    FileWriter fileWriter = new FileWriter(graphsDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml");
                    GraphIO.write(graph, fileWriter);
                    fileWriter.flush();
                    fileWriter.close();

                    ProcessBuilder processBuilder =
                            new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt.exe ",
                                    graphsDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml").inheritIO();
                    Process p = processBuilder.start();
                    p.waitFor();
                    p.destroy();

                    Files.deleteIfExists(Paths.get(graphsDir + File.separator + "TopoPhaseRemoval" + (index + 1) + ".xml"));

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });

            return;

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
