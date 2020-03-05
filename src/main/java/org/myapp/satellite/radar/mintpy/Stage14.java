package org.myapp.satellite.radar.mintpy;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.runtime.Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Stage14 {

    /*
    workingDir="D:\\mnt\\fast\\dockers\\monitor-radar-core\\monitor_radar_usr\\processing\\1580805641883"
    resultDir="D:\\mnt\\hdfs\\user\\monitor_radar_usr\\monitor-radar-core\\results\\1580805641883"
    snapDir="D:\\mnt\\hdfs\\user\\monitor_radar_usr\\monitor-radar-core\\.snap"
    filesList=""
    bPerpCrit=120
    bTempCrit=40
    proc=""
     */

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String resultDir = consoleParameters.get("resultDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();

        Config.instance().preferences().put("snap.userdir", snapDir);

        String configDir = resultDir + File.separator + "config";
        HashMap<String, HashMap> smallBaselineParameters = getParameters(configDir);
        Object useTroposphericDelayCorrection = ((HashMap) smallBaselineParameters.get("MintPy")).get("useTroposphericDelayCorrection");

        try {

            String graphDir = resultDir + File.separator + "graphs";

            Product[] products = null;

            if (Files.exists(Paths.get(workingDir + File.separator + "prep_resize"))) {
                Files.walk(Paths.get(workingDir + File.separator + "prep_resize"))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            if (!Boolean.valueOf(useTroposphericDelayCorrection.toString())) {
                new File(workingDir + File.separator + "prep").renameTo(new File(workingDir + File.separator + "prep_resize"));
                return;
            }

            products = Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_cc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);

            int minWidth = products[0].getSceneRasterWidth(), minHeight = products[0].getSceneRasterHeight();
            for (Product product : products) {
                minWidth = product.getSceneRasterWidth() < minWidth ? product.getSceneRasterWidth() : minWidth;
                minHeight = product.getSceneRasterHeight() < minHeight ? product.getSceneRasterHeight() : minHeight;
            }
            for (Product product : products) {
                product.closeIO();
            }

            String[] productFiles = Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_cc.dim"))
                    .map(file -> file.toAbsolutePath().toString()).toArray(String[]::new);
            subset(graphDir, productFiles, minWidth, minHeight, "cc");

            productFiles = Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_unw_tc.dim"))
                    .map(file -> file.toAbsolutePath().toString()).toArray(String[]::new);
            subset(graphDir, productFiles, minWidth, minHeight, "unw");

            productFiles = Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_coh_tc.dim"))
                    .map(file -> file.toAbsolutePath().toString()).toArray(String[]::new);
            subset(graphDir, productFiles, minWidth, minHeight, "coh");

            productFiles = Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_filt_int_sub_tc.dim"))
                    .map(file -> file.toAbsolutePath().toString()).toArray(String[]::new);
            subset(graphDir, productFiles, minWidth, minHeight, "intf");

            productFiles = Files.walk(Paths.get(workingDir + File.separator + "prep"))
                    .filter(file -> file.toAbsolutePath().toString().contains("dem_tc.dim"))
                    .map(file -> file.toAbsolutePath().toString()).toArray(String[]::new);
            subset(graphDir, productFiles, minWidth, minHeight, "dem");

            if (Files.exists(Paths.get(workingDir + File.separator + "prep"))) {
                Files.walk(Paths.get(workingDir + File.separator + "prep"))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void subset(String graphDir, String[] productFiles, int minWidth, int minHeight, String productPrefix) {

        try {

            new File(productFiles[0].replace("prep", "prep_resize")).getParentFile().mkdirs();

            String subsetCmd = "#!/bin/bash\n";

            for (int i = 0; i < productFiles.length; i++) {

                String sourceProductFile = productFiles[i];
                String targetProductFile = productFiles[i].replace(File.separator + "prep", File.separator + "prep_resize");
                String newGraphFile = productFiles[i].replace(".dim", ".xml");

                FileReader fileReader = new FileReader(graphDir + File.separator + "subset.xml");
                Graph graph = GraphIO.read(fileReader);
                fileReader.close();

                graph.getNode("Read").getConfiguration().getChild("file").setValue(sourceProductFile);
                graph.getNode("Subset").getConfiguration().getChild("region").setValue("0,0," + minWidth + "," + minHeight);
                graph.getNode("Write").getConfiguration().getChild("file").setValue(targetProductFile);

                FileWriter fileWriter = new FileWriter(newGraphFile);
                GraphIO.write(graph, fileWriter);
                fileWriter.flush();
                fileWriter.close();

                subsetCmd = subsetCmd + "gpt " + newGraphFile + "\n";

            }

            PrintWriter out;
            if (productPrefix.equals("dem")) {
                out = new PrintWriter(new File(productFiles[0]).getParentFile().toString() + File.separator + "subset_" + productPrefix + ".cmd");
            } else {
                out = new PrintWriter(new File(productFiles[0]).getParentFile().getParentFile().toString() + File.separator + "subset_" + productPrefix + ".cmd");
            }
            out.println(subsetCmd);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // DataSet
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "smallbaselineApp.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("MintPy",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }
}
