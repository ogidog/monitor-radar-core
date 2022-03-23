
package org.myapp.satellite.radar.stamps;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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

        /*String esdDir = taskDir + "" + File.separator + "esd";
        if (Files.exists(Paths.get(esdDir))) {
            Common.deleteDir(new File(esdDir));
        }
        new File(esdDir).mkdirs();

        String topophaseremovalDir = taskDir + "" + File.separator + "topophaseremoval";
        if (Files.exists(Paths.get(topophaseremovalDir))) {
            Common.deleteDir(new File(topophaseremovalDir));
        }
        new File(topophaseremovalDir).mkdirs();

        String stage2Dir = taskDir + "" + File.separator + "stage2";
        if (Files.exists(Paths.get(stage2Dir))) {
            Common.deleteDir(new File(stage2Dir));
        }
        new File(stage2Dir).mkdirs();*/

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
        ((HashMap) parameters.get("Interferogram")).forEach((key, value) -> {
            graph.getNode("Interferogram").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        // TopoPhaseRemoval
        ((HashMap) parameters.get("TopoPhaseRemoval")).forEach((key, value) -> {
            graph.getNode("TopoPhaseRemoval").getConfiguration().getChild(key.toString())
                    .setValue(value.toString());
        });

        Pattern p = Pattern.compile("\\d{8}");
        Matcher m = p.matcher(InSARStackOverview.findOptimalMasterProduct(products).getName());
        m.find();
        String masterProductDate = m.group();

        /*m = p.matcher(Paths.get(files[2 * i + 1]).getFileName().toString());
        m.find();
        String slaveProductDate = m.group();
        String masterProductPath = applyorbitfileDir;

        graph.getNode("Read").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + masterProductName + ".dim");
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            String slaveProductName = product.getName();
            if (!masterProductName.equals(slaveProductName)) {
                graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(masterProductPath + File.separator + slaveProductName + ".dim");
                graph.getNode("Write").getConfiguration().getChild("file")
                        .setValue(esdDir + File.separator + slaveProductName + "_Stack_Deb.dim");
                graph.getNode("Write(2)").getConfiguration().getChild("file")
                        .setValue(topophaseremovalDir + File.separator + slaveProductName + "_Stack_Ifg_Deb_DInSAR.dim");

                FileWriter fileWriter = new FileWriter(stage2Dir + File.separator + slaveProductName + ".xml");
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                Common.runGPTScript(stage2Dir + File.separator + slaveProductName + ".xml", "Stage2");

            }
        }*/

        Arrays.stream(products).forEach(product -> {
            try {
                product.closeIO();
            } catch (IOException e) {
                System.out.println(e);
            }
        });

    }

}
