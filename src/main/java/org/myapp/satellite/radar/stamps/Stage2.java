
package org.myapp.satellite.radar.stamps;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;


public class Stage2 {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphDir = consoleParameters.get("graphDir").toString();
        String filesList = consoleParameters.get("filesList").toString();

        String[] files;
        try {
            files = Files.walk(Paths.get(filesList)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String esdDir = outputDir + "" + File.separator + "esd";
        if (Files.exists(Paths.get(esdDir))) {
            try {
                Files.walk(Paths.get(esdDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        String topophaseremovalDir = outputDir + "" + File.separator + "topophaseremoval";
        if (Files.exists(Paths.get(topophaseremovalDir))) {
            try {
                Files.walk(Paths.get(topophaseremovalDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        String stage2Dir = outputDir + "" + File.separator + "stage2";
        if (Files.exists(Paths.get(stage2Dir))) {
            try {
                Files.walk(Paths.get(stage2Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        new File(esdDir).mkdirs();
        new File(topophaseremovalDir).mkdirs();
        new File(stage2Dir).mkdirs();

        Product[] products = Arrays.stream(files).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        try {
            Sentinel1Utils s1u = new Sentinel1Utils(products[0]);
            int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);

            String graphFile;
            if (numOfBurst > 1) {
                graphFile = "stamps_prep.xml";
            } else {
                graphFile = "stamps_prep_without_esd.xml";
            }
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            String masterProductName = InSARStackOverview.findOptimalMasterProduct(products).getName();
            String masterProductPath = filesList;

            PrintWriter cmdWriter = new PrintWriter(stage2Dir + File.separator + "stage2.cmd", "UTF-8");

            graph.getNode("Read").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + masterProductName + ".dim");
            for (int i = 0; i < products.length; i++) {
                Product product = products[i];
                String slaveProductName = product.getName();
                if (!masterProductName.equals(slaveProductName)) {
                    graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + slaveProductName + ".dim");
                    graph.getNode("Write").getConfiguration().getChild("file")
                            .setValue(esdDir + File.separator + slaveProductName + "_Stack_Deb.dim");
                    graph.getNode("Write(2)").getConfiguration().getChild("file")
                            .setValue(topophaseremovalDir + File.separator + slaveProductName + "_Stack_Ifg_Deb_DInSAR.dim");

                    FileWriter fileWriter = new FileWriter(stage2Dir + File.separator + slaveProductName + ".xml");
                    GraphIO.write(graph, fileWriter);
                    fileWriter.flush();
                    fileWriter.close();

                    cmdWriter.println("gpt " + stage2Dir + File.separator + slaveProductName + ".xml");
                }
            }

            Arrays.stream(products).forEach(product -> {
                try {
                    product.closeIO();
                } catch (IOException e) {
                    System.out.println(e);
                }
            });

            cmdWriter.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


    }

}
