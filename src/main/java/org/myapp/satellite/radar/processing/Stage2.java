package org.myapp.satellite.radar.processing;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

public class Stage2 {

    public static void main(String[] args) {

        String inputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\applyorbitfile";
        String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing\\subset";
        String graphsDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\graphs";
        String configDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\config";

        int productPatchLength = 7;

        HashMap parameters = getParameters(configDir);

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

            ArrayList<String> filePatchesList = new ArrayList();

            if (sourceProducts.length >= 20) {

                int productsRemaining = sourceProducts.length;
                int productsProcessed = 0;

                while (productsRemaining > 0) {

                    if (productsRemaining < productPatchLength) {

                        String[] productFilesList = new String[productsRemaining + 1];
                        productFilesList[0] = masterProductFile;
                        System.arraycopy(Arrays.stream(sourceProducts).skip(productsProcessed).toArray(String[]::new), 0,
                                productFilesList, 1, productsRemaining);

                        filePatchesList.add(String.join(",", productFilesList));

                        productsProcessed += productPatchLength;
                        productsRemaining = productsRemaining - productsProcessed;

                    } else {

                        String[] productFilesList = new String[productPatchLength + 1];
                        productFilesList[0] = masterProductFile;
                        System.arraycopy(Arrays.stream(sourceProducts).skip(productsProcessed).limit(productPatchLength).toArray(String[]::new), 0,
                                productFilesList, 1, productPatchLength);

                        filePatchesList.add(String.join(",", productFilesList));

                        productsProcessed += productPatchLength;
                        productsRemaining = sourceProducts.length - productsProcessed;
                    }
                }

            } else {

                System.out.println("The minimum number of files should be 20.");
                return;

            }

            IntStream.range(0, filePatchesList.size()).parallel().forEach(index -> {
                try {

                    Reader fileReader = new FileReader(graphsDir + File.separator + "Subset.xml");
                    Graph graph = GraphIO.read(fileReader);
                    fileReader.close();

                    set region

                    graph.getNode("ProductSet-Reader").getConfiguration().getChild("fileList").setValue(filePatchesList.get(index));
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

    static HashMap getParameters(String configDir) {

        try {

            HashMap<String, HashMap> stageParameters = new HashMap<>();

            // BackGeocoding
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "back_geocoding.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            Iterator it = jsonParameters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                parameters.put(pair.getKey().toString(), ((HashMap) jsonParameters.get(pair.getKey().toString())).get("value"));
            }
            stageParameters.put("BackGeocoding", parameters);

            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

}
