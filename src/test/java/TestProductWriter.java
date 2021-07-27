import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class TestProductWriter {

    public static void main(String[] args) {
        try {
            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            // TODO: убрать, записывать в туже папку, переписывать файлы
            String testOutputDir = "D:\\Temp\\";

            String snaphuimportDir = outputDir + File.separator + "snaphuimport";

            String[] files = Files.walk(Paths.get(snaphuimportDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            Product[] products = Arrays.stream(files).map(file -> {
                try {
                    Product product = ProductIO.readProduct(file);
                    return product;
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

            int minWidth = 999999999, minHeight = 999999999;
            for (int i = 0; i < products.length; i++) {
                if (products[i].getSceneRasterWidth() < minWidth) {
                    minWidth = products[i].getSceneRasterWidth();
                }
                if (products[i].getSceneRasterHeight() < minHeight) {
                    minHeight = products[i].getSceneRasterHeight();
                }
            }

            boolean[] mask = new boolean[minHeight * minWidth];
            Arrays.fill(mask, true);

            ProductData pd = ProductData.createInstance(30, minHeight * minWidth);
            for (int i = 0; i < products.length; i++) {
                products[i].getBandAt(cohBandIndex).readRasterData(0, 0, minWidth - 1, minHeight - 1, pd);
                for (int j = 0; j < pd.getNumElems(); j++) {
                    if (pd.getElemFloatAt(j) > 0.5) {
                        if (!mask[j]) {
                            mask[j] = false;
                        } else {
                            mask[j] = true;
                        }
                    } else {
                        mask[j] = false;
                    }
                }
            }

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

            return;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
