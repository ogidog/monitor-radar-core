package org.myapp.satellite.radar.msbas;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Stage7 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String stage7Dir = outputDir + "" + File.separator + "stage7";
            String snaphuimportDir = outputDir + File.separator + "snaphuimport";
            String geotiffDir = outputDir + File.separator + "geotiff";
            String geodimapDir = outputDir + File.separator + "geodimap";

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
                geodimapDir = geodimapDir + File.separator + "dsc";
            } else {
                geotiffDir = geotiffDir + File.separator + "asc";
                geodimapDir = geodimapDir + File.separator + "asc";
            }

            if (Files.exists(Paths.get(geotiffDir))) {
                Files.walk(Paths.get(geotiffDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(geodimapDir))) {
                Files.walk(Paths.get(geodimapDir))
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
            new File(geodimapDir).mkdirs();
            new File(stage7Dir).mkdirs();

            String graphFile = "createtiff.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage7Dir + File.separator + "stage7.cmd", "UTF-8");
            for (int i = 0; i < files.length; i++) {
                String productName = Paths.get(files[i]).getFileName().toString();
                graph.getNode("Read").getConfiguration().getChild("file").setValue(files[i]);
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(geotiffDir + File.separator + productName.replace(".dim", ".disp.geo.tif"));
                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(geodimapDir + File.separator + productName.replace(".dim", ".disp.geo.dim"));

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
