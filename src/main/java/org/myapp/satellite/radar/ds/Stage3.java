package org.myapp.satellite.radar.ds;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Stage3 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();

            String esdDir = outputDir + File.separator + "esd";

            String stage3Dir = outputDir + File.separator + "Stage3";
            if (Files.exists(Paths.get(stage3Dir))) {
                Files.walk(Paths.get(stage3Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage3Dir).mkdirs();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            List<double[]> subsetRegionGeoCoord = Arrays.stream(((HashMap) parameters.get("Subset")).get("geoRegion").toString().split(","))
                    .limit(4)
                    .map(tuple -> {
                        double lon = Double.valueOf(tuple.split(" ")[0]);
                        double lat = Double.valueOf(tuple.split(" ")[1]);
                        return new double[]{lon, lat};
                    }).collect(Collectors.toList());

            String file = Files.walk(Paths.get(esdDir)).filter(path -> {
                if (path.toString().endsWith(".dim")) {
                    return true;
                } else {
                    return false;
                }

            }).findFirst().map(path -> path.toAbsolutePath().toString()).get();

            Product product = ProductIO.readProduct(file);
            Band band = product.getBandAt(0);
            band.readRasterDataFully();

            List<int[]> subsetRegionPixelCoord = subsetRegionGeoCoord.stream()
                    .map(tuple -> {
                        GeoPos geoPos = new GeoPos(tuple[1], tuple[0]);
                        PixelPos pixelPos = new PixelPos();
                        band.getGeoCoding().getPixelPos(geoPos, pixelPos);
                        return new int[]{(int) pixelPos.y, (int) pixelPos.x};
                    }).collect(Collectors.toList());

            band.dispose();
            product.closeIO();
            product.dispose();

            int minY = Integer.MAX_VALUE;
            int minX = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxX = Integer.MIN_VALUE;

            for (int i = 0; i < subsetRegionPixelCoord.size(); i++) {
                if (subsetRegionPixelCoord.get(i)[0] < minY) {
                    minY = subsetRegionPixelCoord.get(i)[0];
                }
                if (subsetRegionPixelCoord.get(i)[1] < minX) {
                    minX = subsetRegionPixelCoord.get(i)[1];
                }
                if (subsetRegionPixelCoord.get(i)[0] > maxY) {
                    maxY = subsetRegionPixelCoord.get(i)[0];
                }
                if (subsetRegionPixelCoord.get(i)[1] > maxX) {
                    maxX = subsetRegionPixelCoord.get(i)[1];
                }
            }

            PrintWriter subsetRegion = new PrintWriter(stage3Dir + File.separator + "subsetRegion.txt", "UTF-8");
            subsetRegion.print(minY + " " + minX + ";" + maxY + " " + maxX);
            subsetRegion.close();

            return;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            JSONParser parser = new JSONParser();
            stageParameters = new HashMap<>();

            // Subset
            FileReader fileReader = new FileReader(configDir + File.separator + "subset.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");
            String geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString();
            HashMap parameters = new HashMap();
            parameters.put("geoRegion", geoRegionCoordinates);
            stageParameters.put("Subset", parameters);
            fileReader.close();

            return stageParameters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
