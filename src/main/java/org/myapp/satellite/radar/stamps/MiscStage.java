package org.myapp.satellite.radar.stamps;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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

public class MiscStage {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String outputDir1 = consoleParameters.get("outputDir1").toString();

            String graphDir = consoleParameters.get("graphDir").toString();
            String stageDir = consoleParameters.get("stageDir").toString();
            String stageDir1 = consoleParameters.get("stageDir1").toString();
            String filesList = consoleParameters.get("filesList").toString();
            String filesList1 = consoleParameters.get("filesList1").toString();

            String[] files, files1;
            files = Files.walk(Paths.get(filesList)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            files1 = Files.walk(Paths.get(filesList1)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String subsetDir = outputDir;
            if (Files.exists(Paths.get(subsetDir))) {
                Files.walk(Paths.get(subsetDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(stageDir))) {
                Files.walk(Paths.get(stageDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(subsetDir).mkdirs();
            new File(stageDir).mkdirs();

            String subsetDir1 = outputDir1;
            if (Files.exists(Paths.get(subsetDir1))) {
                Files.walk(Paths.get(subsetDir1))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(stageDir1))) {
                Files.walk(Paths.get(stageDir1))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(subsetDir1).mkdirs();
            new File(stageDir1).mkdirs();


            Product product = ProductIO.readProduct(files[0]);
            product.getBandAt(0).readRasterDataFully();
            short[] data = ((ProductData.Short) product.getBandAt(0).getData()).getArray();
            product.closeIO();

            int idx;
            int nanCounter = 0;
            int nanTreshhold = (int) (product.getSceneRasterWidth() * 0.1);
            int subsetY0 = -1, subsetY1 = -1, subsetX0 = -1, subsetX1 = -1;

            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();

            // by width
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    idx = width * y + x;
                    if (data[idx] == 0) {
                        nanCounter += 1;
                    }
                }
                if (nanCounter < nanTreshhold && subsetY0 == -1) {
                    subsetY0 = y;
                }
                if (nanCounter > nanTreshhold && subsetY0 != -1) {
                    subsetY1 = y - 1;
                    break;
                }
                nanCounter = 0;
            }

            if (subsetY1 == -1) {
                subsetY1 = height;
            }
            if (subsetY0 == -1) {
                subsetY0 = 0;
            }

            // by height
            nanCounter = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    idx = width * y + x;
                    if (data[idx] == 0) {
                        nanCounter += 1;
                    }
                }
                if (nanCounter < nanTreshhold && subsetX0 == -1) {
                    subsetX0 = x;
                }
                if (nanCounter > nanTreshhold && subsetX0 != -1) {
                    subsetX1 = x - 1;
                    break;
                }
                nanCounter = 0;
            }

            if (subsetX1 == -1) {
                subsetX1 = width;
            }
            if (subsetX0 == -1) {
                subsetX0 = 0;
            }

            PrintWriter cmdWriter = new PrintWriter(stageDir + File.separator + "miscstage.cmd", "UTF-8");
            PrintWriter cmdWriter1 = new PrintWriter(stageDir1 + File.separator + "miscstage1.cmd", "UTF-8");

            String graphFile = "Subset1.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            graph.getNode("Subset").getConfiguration().getChild("region")
                    .setValue(subsetX0 + "," + subsetY0 + "," + subsetX1 + "," + subsetY1);

            for (int i = 0; i < files.length; i++) {
                String fileName = Paths.get(files[i]).getFileName().toString().replace(".dim", "");

                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(subsetDir + File.separator + "Subset_" + fileName + ".dim");
                FileWriter fileWriter = new FileWriter(stageDir + File.separator + fileName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();
                cmdWriter.println("gpt " + stageDir + File.separator + fileName + ".xml");

                fileName = Paths.get(files1[i]).getFileName().toString().replace(".dim", "");
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files1[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(subsetDir1 + File.separator + "Subset_" + fileName + ".dim");
                fileWriter = new FileWriter(stageDir1 + File.separator + fileName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter1.println("gpt " + stageDir1 + File.separator + fileName + ".xml");
            }

            cmdWriter.close();
            cmdWriter1.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }
}
