package org.myapp.satellite.radar.processing;

import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.runtime.Config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ApplyOrbitFileOpEnv {

    Product targetProduct;

    OperatorSpi spi;
    ApplyOrbitFileOp op;

    public ApplyOrbitFileOpEnv(String snapDir) {
        Config.instance().preferences().put("snap.userdir", snapDir);
    }

    public Product getTargetProduct(Product sourceProduct, HashMap stageParameters) {

        HashMap applyOrbitParameters = (HashMap) stageParameters.get("ApplyOrbitFile");

        spi = new ApplyOrbitFileOp.Spi();
        op = (ApplyOrbitFileOp) spi.createOperator();

        op.setSourceProduct(sourceProduct);

        Iterator it = applyOrbitParameters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            op.setParameter(pair.getKey().toString(), pair.getValue());
        }

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
