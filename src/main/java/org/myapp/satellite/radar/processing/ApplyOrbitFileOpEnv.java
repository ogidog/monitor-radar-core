package org.myapp.satellite.radar.processing;

import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.runtime.Config;

import java.util.HashMap;

public class ApplyOrbitFileOpEnv {

    Product targetProduct;

    OperatorSpi spi;
    ApplyOrbitFileOp op;

    public ApplyOrbitFileOpEnv(HashMap parameters) {
        Config.instance().preferences().put("snap.userdir", (String) parameters.get("snap.userdir"));
    }

    public Product getTargetProduct(Product sourceProduct, HashMap parameters) {

        spi = new ApplyOrbitFileOp.Spi();
        op = (ApplyOrbitFileOp) spi.createOperator();

        op.setSourceProduct(sourceProduct);
        op.setParameter("selectedPolarisations", parameters.get("polyDegree"));
        op.setParameter("continueOnFail", parameters.get("continueOnFail"));

        targetProduct = op.getTargetProduct();
        return targetProduct;
    }

    public void Dispose() {
        try {

            this.targetProduct.closeIO();
            targetProduct = null;

            op.dispose();
            spi = null;

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
