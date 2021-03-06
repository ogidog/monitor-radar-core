package org.myapp.satellite.radar.ds;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

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

public class Stage6 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String topophaseremovalDir = outputDir + File.separator + "topophaseremoval";
            String[] files = Files.walk(Paths.get(topophaseremovalDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String stage5Dir = outputDir + File.separator + "stage5";
            if (Files.exists(Paths.get(stage5Dir))) {
                Files.walk(Paths.get(stage5Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage5Dir).mkdirs();

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

            PrintWriter cmdWriter = new PrintWriter(stage5Dir + File.separator + "stage5.cmd", "UTF-8");

            graph.getNode("Read").getConfiguration().getChild("file").setValue(files[0]);
            graph.getNode("Read(2)").getConfiguration().getChild("file").setValue(files[0]);
            graph.getNode("BandMaths").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name")
                    .setValue(masterDate + ".lat");
            graph.getNode("BandMaths(2)").getConfiguration().getChild("targetBands").getChild("targetBand").getChild("name")
                    .setValue(masterDate + ".lon");
            graph.getNode("Write").getConfiguration().getChild("file").setValue(files[0]);

            String fileName = Paths.get(files[0]).getFileName().toString();
            FileWriter fileWriter = new FileWriter(stage5Dir + File.separator + fileName + ".xml");
            GraphIO.write(graph, fileWriter);
            fileWriter.flush();
            fileWriter.close();

            cmdWriter.println("gpt " + stage5Dir + File.separator + fileName + ".xml");
            cmdWriter.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void process(String outputDir, String graphDir, String taskId) throws Exception {

        String taskDir = outputDir + File.separator + taskId;

        String topophaseremovalDir = taskDir + File.separator + "topophaseremoval";
        String[] files = Files.walk(Paths.get(topophaseremovalDir)).filter(path -> {
            if (path.toString().endsWith(".dim")) {
                return true;
            } else {
                return false;
            }
        }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

        String stage6Dir = taskDir + File.separator + "stage6";
        if (Files.exists(Paths.get(stage6Dir))) {
            Routines.deleteDir(new File(stage6Dir));
        }
        new File(stage6Dir).mkdirs();

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
        FileWriter fileWriter = new FileWriter(stage6Dir + File.separator + fileName + ".xml");
        GraphIO.write(graph, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        Routines.runGPTScript(stage6Dir + File.separator + fileName + ".xml", "Stage6");

    }

}
