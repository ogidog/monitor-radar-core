package org.myapp.satellite.radar.sbas;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Stage7 {
    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String filesList1 = consoleParameters.get("filesList1").toString();
            String filesList2 = consoleParameters.get("filesList2").toString();
            String graphDir = consoleParameters.get("graphDir").toString();

            String[] files1;
            if (!filesList1.contains(",")) {
                files1 = Files.walk(Paths.get(filesList1)).filter(file -> file.toString().endsWith(".dim"))
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files1 = filesList1.split(",");
            }

            String[] files2;
            if (!filesList1.contains(",")) {
                files2 = Files.walk(Paths.get(filesList2)).filter(file -> file.toString().endsWith(".dim"))
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files2 = filesList1.split(",");
            }

            String prepDir = outputDir + File.separator + "prep";
            String stage7Dir = outputDir + "" + File.separator + "stage7";
            if (Files.exists(Paths.get(stage7Dir))) {
                Files.walk(Paths.get(stage7Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (Files.exists(Paths.get(prepDir))) {
                Files.walk(Paths.get(prepDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage7Dir).mkdirs();
            new File(prepDir).mkdirs();

            String graphFile = "tc.xml";
            FileReader fileReader = new FileReader(graphDir + File.separator + graphFile);
            Graph graph = GraphIO.read(fileReader);
            fileReader.close();

            PrintWriter cmdWriter = new PrintWriter(stage7Dir + File.separator + "stage7.cmd", "UTF-8");
            for (int i = 0; i < files1.length; i++) {
                Product product = ProductIO.readProduct(files1[i]);
                String[] bandNames = product.getBandNames();
                String[] iqBands = Arrays.stream(bandNames).filter(name -> name.contains("i_") || name.contains("q_")).toArray(String[]::new);


                graph.getNode("Read").getConfiguration().getChild("file").setValue(files1[i]);
                graph.getNode("BandMaths").getConfiguration().getChild("expression").setValue(
                        "");
            }


        } catch (Exception e) {

        }
    }
}
