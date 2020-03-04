package org.myapp.satellite.radar.mintpy;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.runtime.Config;
import org.myapp.utils.ConsoleArgsReader;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Stage13 {

    /*
    workingDir="D:\\mnt\\fast\\dockers\\monitor-radar-core\\monitor_radar_usr\\processing\\1580805641883"
    resultDir="D:\\mnt\\hdfs\\user\\monitor_radar_usr\\monitor-radar-core\\results\\1580805641883"
    snapDir="D:\\mnt\\hdfs\\user\\monitor_radar_usr\\monitor-radar-core\\.snap"
    filesList=""
    bPerpCrit=120
    bTempCrit=40
    proc=""
     */

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();

        Config.instance().preferences().put("snap.userdir", snapDir);

        try {
            Product[] products = null;

            if (Files.exists(Paths.get(workingDir + File.separator + "tc_resize"))) {
                Files.walk(Paths.get(workingDir + File.separator + "tc_resize"))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            products = Files.walk(Paths.get(workingDir + File.separator + "tc"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_cc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);

            int minWidth = products[0].getSceneRasterWidth(), minHeight = products[0].getSceneRasterHeight();
            for (Product product : products) {
                minWidth = product.getSceneRasterWidth() < minWidth ? product.getSceneRasterWidth() : minWidth;
                minHeight = product.getSceneRasterHeight() < minHeight ? product.getSceneRasterHeight() : minHeight;
            }

            subset(products, minWidth, minHeight);
            for (Product product : products) {
                product.closeIO();
            }

            products = Files.walk(Paths.get(workingDir + File.separator + "tc"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_unw_tc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);
            subset(products, minWidth, minHeight);
            for (Product product : products) {
                product.closeIO();
            }

            products = Files.walk(Paths.get(workingDir + File.separator + "tc"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_coh_tc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);
            subset(products, minWidth, minHeight);
            for (Product product : products) {
                product.closeIO();
            }

            products = Files.walk(Paths.get(workingDir + File.separator + "tc"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_filt_int_sub_tc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);
            subset(products, minWidth, minHeight);
            for (Product product : products) {
                product.closeIO();
            }

            products = Files.walk(Paths.get(workingDir + File.separator + "tc"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("dem_tc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);
            subset(products, minWidth, minHeight);
            for (Product product : products) {
                product.closeIO();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void subset(Product[] products, int minWidth, int minHeight) {

        new File(products[0].getFileLocation().toString().replace("tc", "tc_resize")).getParentFile().mkdirs();

        for (int i = 0; i < products.length; i++) {

            File newProductFile = new File(products[i].getFileLocation().toString().replace(File.separator + "tc", File.separator + "tc_resize"));

            OperatorSpi operatorSpiSubset = new SubsetOp.Spi();
            SubsetOp subsetOp = (SubsetOp) operatorSpiSubset.createOperator();

            subsetOp.setSourceProduct(products[i]);
            subsetOp.setRegion(new Rectangle(0, 0, minWidth, minHeight));
            Product targetProduct = subsetOp.getTargetProduct();

            WriteOp writeOp = new WriteOp(targetProduct, newProductFile, "BEAM-DIMAP");
            writeOp.writeProduct(ProgressMonitor.NULL);
        }

    }
}
