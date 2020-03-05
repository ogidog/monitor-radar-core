package org.myapp.satellite.radar.mintpy.snaphu;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectedComponents {

    public static void main(String[] args) {

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String workingDir = consoleParameters.get("workingDir").toString();
        String resultDir = consoleParameters.get("resultDir").toString();

        String configDir = resultDir + File.separator + "config";
        HashMap<String, HashMap> snaphuParameters = getParameters(configDir);
        Object generateConnectedComponentsFile = ((HashMap) snaphuParameters.get("Snaphu")).get("generateConnectedComponentsFile");

        try {
            Files.copy(Paths.get(configDir + File.separator + "smallbaselineApp.cfg"), Paths.get(workingDir + File.separator + "prep_resize" + File.separator + "smallbaselineApp.cfg"), StandardCopyOption.REPLACE_EXISTING);
            if (!Boolean.valueOf(generateConnectedComponentsFile.toString())) {
                return;
            }

            Product[] ccProducts = Files.walk(Paths.get(workingDir + File.separator + "prep_resize"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_cc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);

            int maxWidth = ccProducts[0].getSceneRasterWidth(), maxHeight = ccProducts[0].getSceneRasterHeight();
            for (Product product : ccProducts) {
                maxWidth = product.getSceneRasterWidth() > maxWidth ? product.getSceneRasterWidth() : maxWidth;
                maxHeight = product.getSceneRasterHeight() > maxHeight ? product.getSceneRasterHeight() : maxHeight;
            }

            int[][] intersectData = new int[maxHeight][maxWidth];
            for (int y = 0; y < maxHeight; y++) {
                for (int x = 0; x < maxWidth; x++) {
                    intersectData[y][x] = 1;
                }
            }

            boolean isAllNull = false;
            for (Product product : ccProducts) {
                product.getBandAt(0).readRasterDataFully();
                float[] data = ((ProductData.Float) product.getBandAt(0).getData()).getArray();
                isAllNull = false;
                for (int y = 0; y < product.getSceneRasterHeight(); y++) {
                    for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                        if (data[y * product.getSceneRasterWidth() + x] > 0.0 && intersectData[y][x] == 1) {
                            intersectData[y][x] = 1;
                            isAllNull = true;
                        } else {
                            intersectData[y][x] = 0;
                        }
                    }
                }
                if (!isAllNull) {

                    // TODO: убрать
                    System.out.println(product.getName());

                    break;
                }
            }

            for (Product product : ccProducts) {
                product.closeIO();
            }

            if (!isAllNull) {
                return;
            }


            Product[] cohProducts = Files.walk(Paths.get(workingDir + File.separator + "prep_resize"))
                    .filter(file -> file.toAbsolutePath().toString().endsWith("_coh_tc.dim"))
                    .map(file -> file.toFile())
                    .map(file -> {
                        try {
                            return ProductIO.readProduct(file);
                        } catch (Exception e) {
                            return null;
                        }
                    }).toArray(Product[]::new);

            float[][] aveCoh = new float[maxHeight][maxWidth];
            for (Product product : cohProducts) {
                product.getBandAt(0).readRasterDataFully();
                float[] data = ((ProductData.Float) product.getBandAt(0).getData()).getArray();
                for (int y = 0; y < product.getSceneRasterHeight(); y++) {
                    for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                        aveCoh[y][x] = aveCoh[y][x] + data[y * product.getSceneRasterWidth() + x];
                    }
                }
                product.closeIO();
            }
            for (int y = 0; y < maxHeight; y++) {
                for (int x = 0; x < maxWidth; x++) {
                    aveCoh[y][x] = aveCoh[y][x] / (float) cohProducts.length;
                }
            }


            float maxAveCoh = 0.0f;
            int maxCohY = 0, maxCohX = 0;
            for (int y = 0; y < intersectData.length; y++) {
                for (int x = 0; x < intersectData[0].length; x++) {
                    if (intersectData[y][x] == 1) {
                        if (aveCoh[y][x] > maxAveCoh) {
                            maxCohY = y;
                            maxCohX = x;
                            maxAveCoh = aveCoh[y][x];
                        }
                    }
                }
            }

            if (maxCohY == 0 && maxCohX == 0 && maxAveCoh == 0.0) {
                // TODO: убрать
                System.out.println("(" + maxCohY + ", " + maxCohX + ") = " + maxAveCoh);
                //

                return;
            }

            modifyMintPyConfigFile(workingDir, configDir, maxCohY, maxCohX);

            // TODO: убрать
            System.out.println("(" + maxCohY + ", " + maxCohX + ") = " + maxAveCoh);
            //

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void modifyMintPyConfigFile(String workingDir, String configDir, double y, double x) {
        try {
            // TODO: данные и другие параметры из smallbaseline.cfg менять из файла smallbaseline.json, когда будет добавлен раздел настройки MintPy в monitor-radar-frontend

            String smallBaseLineAppConfig = Files.lines(Paths.get(configDir + File.separator + "smallbaselineApp.cfg")).map(line -> {
                if (line.contains("mintpy.load.connCompFile")) {
                    return "mintpy.load.connCompFile = /home/work/*_*/*_cc*.data/Unw*.img";
                }
                if (line.contains("mintpy.network.tempBaseMax")) {
                    return "mintpy.network.tempBaseMax = 250";
                }
                if (line.contains("mintpy.network.perpBaseMax")) {
                    return "mintpy.network.perpBaseMax = 250";
                }
                if (line.contains("mintpy.reference.yx")) {
                    return "mintpy.reference.yx = " + (int) y + "," + (int) x;
                }
                if (line.contains("mintpy.unwrapError.method")) {
                    return "mintpy.unwrapError.method = bridging";
                }
                if (line.contains("mintpy.unwrapError.ramp")) {
                    return "mintpy.unwrapError.ramp = linear";
                }
                return line;
            }).collect(Collectors.joining("\n"));

            // Write ifgs12_excluded.txt to file
            PrintWriter out = new PrintWriter(workingDir + File.separator + "prep_resize" + File.separator + "smallbaselineApp.cfg");
            out.println(smallBaseLineAppConfig);
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
            FileReader fileReader = new FileReader(configDir + File.separator + "snaphu.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters1 = (HashMap) jsonObject.get("parameters");

            stageParameters.put("Snaphu",
                    (HashMap) jsonParameters1.entrySet().stream
                            ().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get("value")))
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }
}