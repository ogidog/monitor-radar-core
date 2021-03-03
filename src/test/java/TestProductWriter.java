import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.nio.file.Path;

public class TestProductWriter {

    public static void main(String[] args) {
        try {
            Product product = ProductIO.readProduct("D:\\Temp\\maiskoe2\\snaphuimport\\20200524_VV_20200605_VV.dim");
            ProductData pd =
            product.getBandAt(4).get.readRasterData(0,0,1,1,);
            product.getBandAt(9).readRasterDataFully();
            product.getBandAt(9).getRasterData().setElemFloatAt(0, Float.NaN);
            final File file = new File("D:\\Temp\\20200524_VV_20200605_VV.dim");
            ProductIO.writeProduct(product,
                    file,
                    DimapProductConstants.DIMAP_FORMAT_NAME,
                    false,
                    ProgressMonitor.NULL);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
