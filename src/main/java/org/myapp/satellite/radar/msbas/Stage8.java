package org.myapp.satellite.radar.msbas;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

public class Stage8 {
    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String geotiffDir = outputDir + File.separator + "geotiff";

            String cmd = "";
            String[] dscFiles = new String[]{}, ascFiles = new String[]{};
            if (Files.exists(Paths.get(geotiffDir + File.separator + "dsc"))) {
                dscFiles = Files.walk(Paths.get(geotiffDir + File.separator + "dsc")).filter(path -> {
                    if (path.toString().endsWith(".tif")) {
                        return true;
                    } else {
                        return false;
                    }
                }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

                PrintWriter cmdWriter = new PrintWriter(geotiffDir + File.separator + "dsc.txt");
                for (int i = 0; i < dscFiles.length; i++) {
                    Product product = ProductIO.readProduct(dscFiles[i]);
                    String bperp = product.getMetadataRoot().getElement("Abstracted_Metadata").getElement("Baselines").getElementAt(0).getElementAt(1)
                            .getAttribute("Perp Baseline").getData().toString();
                    product.closeIO();
                    cmd = cmd + "../dsc/" + Paths.get(dscFiles[i]).getFileName() + " " + bperp + " " + product.getName().split("_")[0] + " " + product.getName().split("_")[2] + "\n";
                }
                cmdWriter.print(cmd.trim());
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

                PrintWriter cmdWriter = new PrintWriter(geotiffDir + File.separator + "asc.txt");
                for (int i = 0; i < ascFiles.length; i++) {
                    Product product = ProductIO.readProduct(ascFiles[i]);
                    String bperp = product.getMetadataRoot().getElement("Abstracted_Metadata").getElement("Baselines").getElementAt(0).getElementAt(1)
                            .getAttribute("Perp Baseline").getData().toString();
                    product.closeIO();
                    cmd = cmd + "../asc/" + Paths.get(dscFiles[i]).getFileName() + " " + bperp + " " + product.getName().split("_")[0] + " " + product.getName().split("_")[2] + "\n";
                }
                cmdWriter.print(cmd.trim());
                cmdWriter.close();
            }

            if (Files.exists(Paths.get(geotiffDir + File.separator + "out"))) {
                Files.walk(Paths.get(geotiffDir + File.separator + "out"))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(geotiffDir + File.separator + "out").mkdirs();

            int dscWidth = 999999999, dscHeight = 999999999, ascWidth = 999999999, ascHeight = 999999999, minWidth = 999999999, minHeight = 999999999;
            for (int i = 0; i < dscFiles.length; i++) {
                Product product = ProductIO.readProduct(dscFiles[i]);
                if (product.getSceneRasterWidth() < dscWidth) {
                    dscWidth = product.getSceneRasterWidth();
                }
                if (product.getSceneRasterHeight() < dscHeight) {
                    dscHeight = product.getSceneRasterHeight();
                }
                product.closeIO();
            }
            for (int i = 0; i < ascFiles.length; i++) {
                Product product = ProductIO.readProduct(ascFiles[i]);
                if (product.getSceneRasterWidth() < ascWidth) {
                    ascWidth = product.getSceneRasterWidth();
                }
                if (product.getSceneRasterHeight() < ascHeight) {
                    ascHeight = product.getSceneRasterHeight();
                }
                product.closeIO();
            }

            minWidth = Math.min(dscWidth, ascWidth) - 1;
            minHeight = Math.min(dscHeight, ascHeight) - 1;

            String[] header = Files.readAllLines(Paths.get(geotiffDir + File.separator + "header.txt")).stream().toArray(String[]::new);
            PrintWriter headerWriter = new PrintWriter(geotiffDir + File.separator + "header.txt");
            cmd = "";
            for (int i = 0; i < header.length; i++) {
                if (header[i].contains("FILE_SIZE")) {
                    header[i] = "FILE_SIZE=" + String.valueOf(minWidth) + ", " + String.valueOf(minHeight);
                }
                if (header[i].contains("WINDOW_SIZE")) {
                    header[i] = "WINDOW_SIZE=0, " + String.valueOf(minWidth - 1) + ", 0, " + String.valueOf(minHeight - 1);
                }
                cmd = cmd + header[i] + "\n";
            }
            headerWriter.print(cmd.trim());
            headerWriter.close();

            return;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
