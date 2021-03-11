package org.myapp.satellite.radar.msbas;

import com.bc.ceres.core.ProgressMonitor;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Stage9 {

    public static void main(String[] args) {

        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String sbasDscDir = outputDir + File.separator + "geotiff" + File.separator + "sbas_dsc";
            String sbasAscDir = outputDir + File.separator + "geotiff" + File.separator + "sbas_asc";
            String geodimapDir = outputDir + File.separator + "geodimap" + File.separator + "dsc";

            String[] files = Files.walk(Paths.get(geodimapDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);


            Product[] products = Arrays.stream(files).map(file -> {
                try {
                    return ProductIO.readProduct(file);
                } catch (Exception ex) {
                    return null;
                }
            }).toArray(Product[]::new);

            int height = products[0].getSceneRasterHeight();
            int width = products[0].getSceneRasterWidth();
            boolean[] mask = new boolean[height * width];
            Arrays.fill(mask, false);
            ProductData pd = ProductData.createInstance(30, height * width);
            for (int i = 0; i < products.length; i++) {
                products[i].getBandAt(2).readRasterData(0, 0, width, height, pd);
                for (int j = 0; j < pd.getNumElems(); j++) {
                    if (pd.getElemFloatAt(j) > 0.5) {
                        mask[j] = true;
                    }
                }
            }

            for (int i = 0; i < products.length; i++) {
                products[i].closeIO();
            }

            if (Files.exists(Paths.get(sbasDscDir))) {
                Product product = ProductIO.readProduct(sbasDscDir + File.separator + "MSBAS_LINEAR_RATE_LOS.tif");

                product.getBandAt(0).readRasterDataFully();
                for (int j = 0; j < mask.length; j++) {
                    if (!mask[j]) {
                        product.getBandAt(0).getRasterData().setElemFloatAt(j, Float.NaN);
                    }
                }
                File file = new File(sbasDscDir + File.separator + "MSBAS_LINEAR_RATE_LOS_coh_filt.tif");
                ProductIO.writeProduct(product,
                        file,
                        "GeoTiff",
                        false,
                        ProgressMonitor.NULL);
                product.closeIO();
            }

            if (Files.exists(Paths.get(sbasAscDir))) {
                Product product = ProductIO.readProduct(sbasAscDir + File.separator + "MSBAS_LINEAR_RATE_LOS.tif");

                product.getBandAt(0).readRasterDataFully();
                for (int j = 0; j < mask.length; j++) {
                    if (!mask[j]) {
                        product.getBandAt(0).getRasterData().setElemFloatAt(j, Float.NaN);
                    }
                }
                File file = new File(sbasAscDir + File.separator + "MSBAS_LINEAR_RATE_LOS_coh_filt.tif");
                ProductIO.writeProduct(product,
                        file,
                        "GeoTiff",
                        false,
                        ProgressMonitor.NULL);
                product.closeIO();
            }

            return;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
