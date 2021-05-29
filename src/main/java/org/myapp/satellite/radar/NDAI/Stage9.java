package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Stage9 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String ndaiFile = outputDir + File.separator + "ndai" + File.separator + "ndai.dim";
            String avgStdFile = outputDir + File.separator + "avgstd" + File.separator + "cohavgstd.dim";
            String avgNDAIDir = outputDir + File.separator + "avgndai";
            String stage9Dir = outputDir + "" + File.separator + "stage9";

            if (Files.exists(Paths.get(avgNDAIDir))) {
                Files.walk(Paths.get(avgNDAIDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(avgNDAIDir).mkdirs();

            if (Files.exists(Paths.get(stage9Dir))) {
                Files.walk(Paths.get(stage9Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage9Dir).mkdirs();

            Product ndaiProduct = ProductIO.readProduct(ndaiFile);
            int width = ndaiProduct.getSceneRasterWidth();
            int height = ndaiProduct.getSceneRasterHeight();
            String[] ndaiBandNames = ndaiProduct.getBandNames();

            int idx;
            int falsePixelsCounter = 0;
            int falsePixelsTreshhold = (int) (ndaiProduct.getSceneRasterWidth() * 0.1);
            int subsetY0 = -1, subsetY1 = -1, subsetX0 = -1, subsetX1 = -1;
            int maxSubsetY0 = 0, minSubsetY1 = height, maxSubsetX0 = 0, minSubsetX1 = width;
            Band[] ndaiBands = ndaiProduct.getBands();
            for (Band band : ndaiBands) {

                falsePixelsCounter = 0;
                subsetY0 = -1;
                subsetY1 = -1;
                subsetX0 = -1;
                subsetX1 = -1;

                band.readRasterDataFully();
                float[] data = ((ProductData.Float) band.getData()).getArray();

                // by width
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        idx = width * y + x;
                        if (data[idx] == 1.0) {
                            falsePixelsCounter += 1;
                        }
                    }
                    if (falsePixelsCounter < falsePixelsTreshhold && subsetY0 == -1) {
                        subsetY0 = y;
                    }
                    if (falsePixelsCounter > falsePixelsTreshhold && subsetY0 != -1) {
                        subsetY1 = y - 1;
                        break;
                    }
                    falsePixelsCounter = 0;
                }
                if (subsetY1 == -1) {
                    subsetY1 = height;
                }
                if (subsetY0 == -1) {
                    subsetY0 = 0;
                }

                // by height
                falsePixelsCounter = 0;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        idx = width * y + x;
                        if (data[idx] == 1.0) {
                            falsePixelsCounter += 1;
                        }
                    }
                    if (falsePixelsCounter < falsePixelsTreshhold && subsetX0 == -1) {
                        subsetX0 = x;
                    }
                    if (falsePixelsCounter > falsePixelsTreshhold && subsetX0 != -1) {
                        subsetX1 = x - 1;
                        break;
                    }
                    falsePixelsCounter = 0;
                }
                if (subsetX1 == -1) {
                    subsetX1 = width;
                }
                if (subsetX0 == -1) {
                    subsetX0 = 0;
                }


                if (subsetY0 > maxSubsetY0) {
                    maxSubsetY0 = subsetY0;
                }
                if (subsetY1 < minSubsetY1) {
                    minSubsetY1 = subsetY1;
                }
                if (subsetX0 > maxSubsetX0) {
                    maxSubsetX0 = subsetX0;
                }
                if (subsetX1 < minSubsetX1) {
                    minSubsetX1 = subsetX1;
                }
            }

            ndaiProduct.closeIO();
            ndaiProduct.dispose();

            String graphFile = "avgndai.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage9Dir + File.separator + "stage9.cmd", "UTF-8");
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(stage9Dir + File.separator + "avgndai.xml"));

            graph.getNode("Read").getConfiguration().getChild("file").setValue(ndaiFile);
            graph.getNode("Write").getConfiguration().getChild("file")
                    .setValue(avgNDAIDir + File.separator + "avgndai");
            graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(avgStdFile);
            graph.getNode("Write(2)").getConfiguration().getChild("file")
                    .setValue(avgNDAIDir + File.separator + "subsetedCohAvgStd.dim");

            graph.getNode("Subset").getConfiguration().getChild("region")
                    .setValue(maxSubsetX0 + "," + maxSubsetY0 + "," + minSubsetX1 + "," + minSubsetY1);
            graph.getNode("Subset(2)").getConfiguration().getChild("region")
                    .setValue(maxSubsetX0 + "," + maxSubsetY0 + "," + minSubsetX1 + "," + minSubsetY1);

            String[] yearList = Arrays.stream(ndaiBandNames).map(bandName -> {
                Matcher m = Pattern.compile("(_)(\\d{2})(\\w{3})(\\d{4})(_)").matcher(bandName);
                m.find();
                return m.group(4);
            }).distinct().toArray(String[]::new);

            int counter = 1;
            for (String year : yearList) {
                String filteredBands = Arrays.stream(ndaiBandNames).filter(bandName -> {
                    if (bandName.contains(year)) {
                        return true;
                    } else {
                        return false;
                    }
                }).collect(Collectors.joining(","));

                graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("expression").setValue(
                        "avg(" + filteredBands + ")");
                graph.getNode("BandMaths(" + String.valueOf(counter) + ")").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name").setValue(
                        "avg_ndai_" + year);
                counter += 1;
            }

            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            cmdWriter.println("gpt " + stage9Dir + File.separator + "avgndai.xml");
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
