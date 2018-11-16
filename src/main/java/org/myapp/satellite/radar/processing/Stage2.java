package org.myapp.satellite.radar.processing;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

public class Stage2 {

    public static void main(String[] args) {

        String inputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\data\\applyorbitfile";
        String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\data\\subset";
        String graphsDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\data\\graphs";

        int productPatchLength = 7;

        HashMap parameters = getParameters();

        try {

            String[] sourceProducts = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toString()).toArray(String[]::new);
            String masterProductFile = sourceProducts[0];
            sourceProducts = Arrays.stream(sourceProducts).skip(1).toArray(String[]::new);

            IntStream.range(0, sourceProducts.length % productPatchLength != 0 ? (sourceProducts.length / productPatchLength) + 1 : sourceProducts.length / productPatchLength)
                    .forEach(patchNum -> {
                        new File(outputDir + File.separator + (patchNum + 1)).mkdirs();
                    });

            ArrayList<String> filesLists = new ArrayList();

            if (sourceProducts.length >= 20) {

                int productsRemaining = sourceProducts.length;
                int productsProcessed = 0;

                while (productsRemaining > 0) {

                    if (productsRemaining < productPatchLength) {

                        String[] productFilesList = new String[productsRemaining + 1];
                        productFilesList[0] = masterProductFile;
                        System.arraycopy(Arrays.stream(sourceProducts).skip(productsProcessed).toArray(String[]::new), 0,
                                productFilesList, 1, productsRemaining);

                        filesLists.add(String.join(",", productFilesList));

                        productsProcessed += productPatchLength;
                        productsRemaining = productsRemaining - productsProcessed;

                    } else {

                        String[] productFilesList = new String[productPatchLength + 1];
                        productFilesList[0] = masterProductFile;
                        System.arraycopy(Arrays.stream(sourceProducts).skip(productsProcessed).limit(productPatchLength).toArray(String[]::new), 0,
                                productFilesList, 1, productPatchLength);

                        filesLists.add(String.join(",", productFilesList));

                        productsProcessed += productPatchLength;
                        productsRemaining = sourceProducts.length - productsProcessed;
                    }
                }

            } else {

                System.out.println("The minimum number of files should be 20.");
                return;

            }

            IntStream.range(0, filesLists.size()).parallel().forEach(index -> {
                try {

                    Reader fileReader = new FileReader(graphsDir + File.separator + "Subset.xml");
                    Graph graph = GraphIO.read(fileReader);
                    fileReader.close();

                    graph.getNode("ProductSet-Reader").getConfiguration().getChild("fileList").setValue(filesLists.get(index));
                    graph.getNode("Write").getConfiguration().getChild("file").setValue(outputDir + File.separator + (index + 1) + File.separator + "subset_master_Stack_Deb.dim");

                    FileWriter fileWriter = new FileWriter(graphsDir + File.separator + "Subset" + (index + 1) + ".xml");
                    GraphIO.write(graph, fileWriter);
                    fileWriter.flush();
                    fileWriter.close();

                    ProcessBuilder processBuilder = new ProcessBuilder(System.getenv("SNAP_HOME") + File.separator + "bin" + File.separator + "gpt.exe ",
                            graphsDir + File.separator + "Subset" + (index + 1) + ".xml").inheritIO();
                    Process p = processBuilder.start();
                    p.waitFor();
                    p.destroy();

                    Files.deleteIfExists(Paths.get(graphsDir + File.separator + "Subset" + (index + 1) + ".xml"));

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static HashMap getParameters() {

        // TODO: Загружать параметры из json-файла

        HashMap parameters = new HashMap();

        // Subset
        parameters.put("topLeftLat", 55.60507332069096);
        parameters.put("topLeftLon", 86.1867704184598);
        parameters.put("topRightLat", 55.6487070962106);
        parameters.put("topRightLon", 86.18718760125022);
        parameters.put("bottomLeftLat", 55.64874125567167);
        parameters.put("bottomLeftLon", 86.08502696051652);
        parameters.put("bottomRightLat", 55.60510658714328);
        parameters.put("bottomRightLon", 86.08502696051652);
        parameters.put("topLeftLat1", 55.60507332069096);
        parameters.put("topLeftLon1", 86.1867704184598);


        return parameters;
    }

}
