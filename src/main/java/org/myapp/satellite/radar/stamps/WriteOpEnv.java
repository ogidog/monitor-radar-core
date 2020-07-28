package org.myapp.satellite.radar.stamps;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.WriteOp;

import java.io.File;

public class WriteOpEnv {

    WriteOp writeOp;

    boolean write(String outputDir, Product targetProduct) {

        try {
            File ouptutFile = new File(outputDir, targetProduct.getName());
            writeOp = new WriteOp(targetProduct, ouptutFile, "BEAM-DIMAP");
            writeOp.writeProduct(ProgressMonitor.NULL);

            targetProduct.closeIO();
            writeOp.dispose();
            writeOp = null;

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
