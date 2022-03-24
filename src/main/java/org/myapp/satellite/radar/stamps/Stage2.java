
package org.myapp.satellite.radar.stamps;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.Common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Stage2 {

    public static void process(String tasksDir, String resultsDir, String username, String taskId) throws Exception {

        HashMap parameters = Common.getParameters(Common.getConfigDir(resultsDir, username, taskId), new String[]{
                Common.OperationName.TOPO_PHASE_REMOVAL,
                Common.OperationName.INTERFEROGRAM, Common.OperationName.BACK_GEOCODING, Common.OperationName.SUBSET
        });

        String operationTaskDir = Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE2);
        if (Files.exists(Paths.get(operationTaskDir))) {
            Common.deleteDir(new File(operationTaskDir));
        }
        new File(operationTaskDir).mkdirs();

        String[] files = Common.getFiles(Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE1), ".dim");
        Product[] products = Arrays.stream(files).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        // Set graph
        Graph graph;
        Sentinel1Utils s1u = new Sentinel1Utils(products[0]);
        int numOfBurst = s1u.getNumOfBursts(s1u.getSubSwath()[0].subSwathName);
        if (numOfBurst > 1) {
            graph = Common.readGraphFile(Common.getGraphFile(resultsDir, username, taskId, Common.OperationName.STAMPS_PREP));
        } else {
            graph = Common.readGraphFile(Common.getGraphFile(resultsDir, username, taskId, Common.OperationName.STAMPS_PREP_WITHOUT_ESD));
        }

        // Subset
        graph.getNode("Subset").getConfiguration().getChild("geoRegion")
                .setValue("POLYGON((" + ((HashMap) parameters.get(Common.OperationName.SUBSET)).get("geoRegion").toString() + "))");
        graph.getNode("Subset(2)").getConfiguration().getChild("geoRegion")
                .setValue("POLYGON((" + ((HashMap) parameters.get(Common.OperationName.SUBSET)).get("geoRegion").toString() + "))");

        // BackGeocoding
        ((HashMap) parameters.get(Common.OperationName.BACK_GEOCODING)).forEach((key, value) -> {
            graph.getNode("Back-Geocoding").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        // Interferogram
        ((HashMap) parameters.get(Common.OperationName.INTERFEROGRAM)).forEach((key, value) -> {
            graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        // TopoPhaseRemoval
        ((HashMap) parameters.get(Common.OperationName.TOPO_PHASE_REMOVAL)).forEach((key, value) -> {
            graph.getNode("TopoPhaseRemoval").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        Pattern p = Pattern.compile("\\d{8}");
        Product masterProduct = InSARStackOverview.findOptimalMasterProduct(products);
        Matcher m = p.matcher(masterProduct.getName());
        m.find();

        String masterProductDate = m.group();

        graph.getNode("Read").getConfiguration().getChild("file").setValue(masterProduct.getFileLocation().toString());
        for (int i = 0; i < products.length; i++) {
            m = p.matcher(products[i].getName());
            m.find();
            String slaveProductDate = m.group();

            if (!masterProductDate.equals(slaveProductDate)) {

                String productDate = masterProductDate + "_" + slaveProductDate;

                String targetFile1 = operationTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING + ".dim";
                String targetFile2 = operationTaskDir + File.separator + productDate + Common.OperationPrefix.TOPO_PHASE_REMOVAL + ".dim";
                String targetGraphFile = operationTaskDir + File.separator + productDate + Common.OperationPrefix.BACK_GEOCODING + ".xml";

                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(products[i].getFileLocation().toString());
                graph.getNode("Write").getConfiguration().getChild("file").setValue(targetFile1);
                graph.getNode("Write(2)").getConfiguration().getChild("file").setValue(targetFile2);

                FileWriter fileWriter = new FileWriter(targetGraphFile);
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                Common.runGPTScript(targetGraphFile, Common.OperationName.STAMPS_STAGE2);

            }
        }

        // Close products
        Arrays.stream(products).forEach(product -> {
            try {
                product.closeIO();
            } catch (IOException e) {
                System.out.println(e);
            }
        });
    }
}
