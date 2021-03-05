package org.myapp.satellite.radar.msbas;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
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

public class Stage7bak {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String stage7Dir = outputDir + "" + File.separator + "stage7";
            String snaphuimportDir = outputDir + File.separator + "snaphuimport";
            String geotiffDir = outputDir + File.separator + "geotiff";

            String[] files;
            files = Files.walk(Paths.get(snaphuimportDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            Product product = ProductIO.readProduct(files[0]);
            String pass = product.getMetadataRoot().getElement("Abstracted_Metadata").getAttribute("PASS").getData().toString();
            product.closeIO();
            if (pass.equals("DESCENDING")) {
                geotiffDir = geotiffDir + File.separator + "dsc";
            } else {
                geotiffDir = geotiffDir + File.separator + "asc";
            }

            if (Files.exists(Paths.get(geotiffDir))) {
                Files.walk(Paths.get(geotiffDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(stage7Dir))) {
                Files.walk(Paths.get(stage7Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(geotiffDir).mkdirs();
            new File(stage7Dir).mkdirs();

            //TODO: добавить сюда код из TestProductWriter
            // из createtiff.xml убрать BandMaths

            String graphFile = "createtiff.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage7Dir + File.separator + "stage7.cmd", "UTF-8");
            for (int i = 0; i < files.length; i++) {
                //TODO: убрать
                //########################################
                product = ProductIO.readProduct(files[i]);
                Band[] bands = product.getBands();
                String cohBand = "", unwBand = "";
                for (Band band : bands) {
                    if (band.getName().toString().contains("coh_")) {
                        cohBand = band.getName().toString();
                    }
                    if (band.getName().toString().contains("Unw_")) {
                        unwBand = band.getName().toString();
                    }
                }
                product.closeIO();
                String expression = "if " + cohBand + ">0.5 then " + unwBand + " else NaN";
                graph.getNode("BandMaths").getConfiguration().getChild("targetBands").getChild("targetBand")
                        .getChild("expression").setValue(expression);
                //####################################################

                String productName = Paths.get(files[i]).getFileName().toString();
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(geotiffDir + File.separator + productName.replace(".dim", ".disp.geo.tif"));

                FileWriter fileWriter = new FileWriter(stage7Dir + File.separator + productName.replace(".dim", ".disp.geo.xml"));
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                cmdWriter.println("gpt " + stage7Dir + File.separator + productName.replace(".dim", ".disp.geo.xml"));
            }

            cmdWriter.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
