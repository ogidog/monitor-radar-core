package org.myapp.satellite.radar.msbas;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import scala.util.Try;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Coherence {

    public boolean[] getCohMask(String cohFilesDir) {

        boolean[] mask = null;

        try {
            String[] files = Files.walk(Paths.get(cohFilesDir)).filter(path -> {
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
            mask = new boolean[height * width];
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

            return mask;

        } catch (Exception ex) {
            ex.printStackTrace();

            return mask;
        }
    }
}
