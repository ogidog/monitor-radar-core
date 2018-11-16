package org.myapp.satellite.radar.processing;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.sentinel1.gpf.BackGeocodingOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class BackGeocodingEnv {

    public Product getTargetProduct(String inputDir, HashMap parameters) {

        try {

            Product[] sourceProducts = Files.find(Paths.get(inputDir), 1, (path, attr) -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> {
                try {
                    return ProductIO.readProduct(new File(path.toString()));
                } catch (Exception e) {
                    return null;
                }
            }).filter(product -> !product.equals(null)).toArray(Product[]::new);

            OperatorSpi spi1 = new BackGeocodingOp.Spi();
            BackGeocodingOp backGeocodingOp = (BackGeocodingOp) spi1.createOperator();
            backGeocodingOp.setSourceProducts(sourceProducts);

            return backGeocodingOp.getTargetProduct();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void executeGraph() {
        Graph graph = null;
        try (Reader fileReader = new FileReader("F:\\intellij-idea-workspace\\monitor-radar-core-v2\\data\\graphs\\BackGeocoding.xml")) {
            graph = GraphIO.read(fileReader);

            // GraphIO.write(graph, new FileWriter("F:\\intellij-idea-workspace\\monitor-radar-core-v2\\data\\graphs\\BackGeocoding.xml"));

            final GraphProcessor processor = new GraphProcessor();
            // setIO(graph, srcFiles, outputFile, "BEAM-DIMAP");

            processor.executeGraph(graph, ProgressMonitor.NULL);
        } catch (Exception e) {

        }
    }
}
