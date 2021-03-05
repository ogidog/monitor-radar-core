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


            Product[] products = Arrays.stream(files).map(file -> {
                try {
                    return ProductIO.readProduct(file);
                } catch (Exception ex) {
                    return null;
                }
            }).toArray(Product[]::new);

            int cohBandIndex = -1, unwBandIndex = -1;
            for (int i = 0; i < products[0].getBands().length; i++) {
                if (products[0].getBandAt(i).getName().toLowerCase().contains("coh")) {
                    cohBandIndex = i;
                }
                if (products[0].getBandAt(i).getName().toLowerCase().contains("unw")) {
                    unwBandIndex = i;
                }
            }

            int height = products[0].getSceneRasterWidth();
            int width = products[0].getSceneRasterWidth();
            boolean[] mask = new boolean[height * width];
            Arrays.fill(mask, false);
            ProductData pd = ProductData.createInstance(30, height * width);
            for (int i = 0; i < products.length; i++) {
                products[i].getBandAt(cohBandIndex).readRasterData(0, 0, width, height, pd);
                for (int j = 0; j < pd.getNumElems(); j++) {
                    if (pd.getElemFloatAt(j) > 0.5) {
                        mask[j] = true;
                    }
                }
            }

            for (int i = 0; i < products.length; i++) {
                products[i].closeIO();
            }

            products = Arrays.stream(files).map(file -> {
                try {
                    return ProductIO.readProduct(file);
                } catch (Exception ex) {
                    return null;
                }
            }).toArray(Product[]::new);

            for (int i = 0; i < products.length; i++) {
                products[i].getBandAt(unwBandIndex).readRasterDataFully();
                for (int j = 0; j < mask.length; j++) {
                    if (!mask[j]) {
                        products[i].getBandAt(unwBandIndex).getRasterData().setElemFloatAt(j, Float.NaN);
                    }
                }
                File file = new File(snaphuimportDir + File.separator + products[i].getName() + ".dim");
                ProductIO.writeProduct(products[i],
                        file,
                        DimapProductConstants.DIMAP_FORMAT_NAME,
                        false,
                        ProgressMonitor.NULL);
                products[i].closeIO();
            }

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
