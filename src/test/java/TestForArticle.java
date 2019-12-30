import org.esa.s1tbx.sentinel1.gpf.TOPSARSplitOp;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;

import java.io.File;

public class TestForArticle {

    public static void main(String args[]) {
        String s1aImg1Path = "/mnt/hdfs/s1adata/" +
                "S1B_IW_SLC__1SDV_20191225T003559_20191225T003626_019514_024DF4_9F74.zip";

        try {
            Product sourceProduct1 = ProductIO.readProduct(new File(s1aImg1Path));

            Operator op1 = (TOPSARSplitOp) (new TOPSARSplitOp.Spi().createOperator());
            // ... настройка параметров оператора
            {
                op1.setParameter("selectedPolarisations", "VV");
                op1.setParameter("subswath", "IW");
                op1.setParameter("firstBurstIndex", 1);
                op1.setParameter("lastBurstIndex", 2);
                op1.setSourceProduct(sourceProduct1);
            }
            Product targetProduct1 = op1.getTargetProduct();
            Operator op11 = (ApplyOrbitFileOp) (new ApplyOrbitFileOp.Spi().createOperator());
            op11.setSourceProduct(targetProduct1);
            Product targetProduct11 = op1.getTargetProduct();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/*
            Product sourceProduct2 = ProductIO.readProduct(new File(s1aImg1Path));
                  // master
        String s1aImg1Path = "/mnt/hdfs/s1adata/S1B_IW_SLC__1SDV_20191225T003559_20191225T003626_019514_024DF4_9F74.zip";
        // slave
        String s1aImg2Path = "/mnt/hdfs/s1adata/S1B_IW_SLC__1SDV_20191222T001206_20191222T001233_019470_024C76_8911.zip";

            Operator op2 = (TOPSARSplitOp)(new TOPSARSplitOp.Spi().createOperator());
            // ... настройка параметров оператора
            op2.setSourceProduct(sourceProduct2);
            Product targetProduct2 = op2.getTargetProduct();
*/
