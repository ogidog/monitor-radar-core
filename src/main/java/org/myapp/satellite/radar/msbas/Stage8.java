package org.myapp.satellite.radar.msbas;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class Stage8 {
    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String geotiffDir = outputDir + File.separator + "geotiff";


            String[] dscFiles, ascFiles;
            if (Files.exists(Paths.get(geotiffDir + File.separator + "dsc"))) {
                dscFiles = Files.walk(Paths.get(geotiffDir + File.separator + "dsc")).filter(path -> {
                    if (path.toString().endsWith(".tif")) {
                        return true;
                    } else {
                        return false;
                    }
                }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

                PrintWriter cmdWriter = new PrintWriter(geotiffDir + File.separator + "dsc.txt", "UTF-8");
                for (int i = 0; i < dscFiles.length; i++) {
                    Product product = ProductIO.readProduct(dscFiles[i]);
                    String bperp = product.getMetadataRoot().getElement("Abstracted_Metadata").getElement("Baselines").getElementAt(0).getElementAt(1)
                            .getAttribute("Perp Baseline").getData().toString();
                    product.closeIO();
                    cmdWriter.println(dscFiles[i] + " " + bperp + " " + product.getName().split("_")[0] + " " + product.getName().split("_")[2]);
                }

                cmdWriter.close();
            }
            if (Files.exists(Paths.get(geotiffDir + File.separator + "asc"))) {
                ascFiles = Files.walk(Paths.get(geotiffDir + File.separator + "asc")).filter(path -> {
                    if (path.toString().endsWith(".tif")) {
                        return true;
                    } else {
                        return false;
                    }
                }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

                PrintWriter cmdWriter = new PrintWriter(geotiffDir + File.separator + "asc.txt", "UTF-8");
                for (int i = 0; i < ascFiles.length; i++) {
                    Product product = ProductIO.readProduct(ascFiles[i]);
                    String bperp = product.getMetadataRoot().getElement("Abstracted_Metadata").getElement("Baselines").getElementAt(0).getElementAt(1)
                            .getAttribute("Perp Baseline").getData().toString();
                    product.closeIO();
                    cmdWriter.println(ascFiles[i] + " " + bperp + " " + product.getName().split("_")[0] + " " + product.getName().split("_")[2]);
                }

                cmdWriter.close();
            }

            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
