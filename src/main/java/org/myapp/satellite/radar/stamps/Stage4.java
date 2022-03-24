package org.myapp.satellite.radar.stamps;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Stage4 {

    public static void process(String tasksDir, String resultsDir, String username, String taskId) throws Exception {

        String operationTaskDir = Common.getOperationTaskDir(tasksDir, username, taskId, Common.OperationName.STAMPS_STAGE2);
        String[] files = Common.getFiles(operationTaskDir,"_topo.dim");

        Product product = ProductIO.readProduct(files[0]);
        String masterDate = product.getBandAt(0).getName().split("_")[3];
        product.closeIO();
        Locale locale = new Locale("en", "US");
        Locale.setDefault(locale);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMMyyyy");
        Date date = simpleDateFormat.parse(masterDate);
        locale = new Locale("ru", "RU");
        Locale.setDefault(locale);
        simpleDateFormat.applyPattern("yyyyMMdd");
        masterDate = simpleDateFormat.format(date).toString();

        String graphFile = "bandmaths.xml";
        FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
        Graph graph = GraphIO.read(fileReader);
        fileReader.close();

        graph.getNode("Read").getConfiguration().getChild("file").setValue(files[0]);
        graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(files[0]);
        graph.getNode("BandMaths").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name")
                .setValue(masterDate + ".lat");
        graph.getNode("BandMaths(2)").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name")
                .setValue(masterDate + ".lon");
        graph.getNode("Write").getConfiguration().getChild("file").setValue(files[0]);

        String fileName = Paths.get(files[0]).getFileName().toString();
        FileWriter fileWriter = new FileWriter(stage4Dir + File.separator + fileName + ".xml");
        GraphIO.write(graph, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        Common.runGPTScript(stage4Dir + File.separator + fileName + ".xml","Stage4");

    }
}
