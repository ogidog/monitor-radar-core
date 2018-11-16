package org.myapp.satellite.radar.processing;

import org.esa.s1tbx.sentinel1.gpf.TOPSARDeburstOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;

import java.util.HashMap;

public class DeburstEnv {

    public Product getTargetProduct(Product sourceProduct, HashMap parameters) {

        OperatorSpi spi = new TOPSARDeburstOp.Spi();
        TOPSARDeburstOp op = (TOPSARDeburstOp) spi.createOperator();
        op.setSourceProduct(sourceProduct);

        return op.getTargetProduct();
    }
}
