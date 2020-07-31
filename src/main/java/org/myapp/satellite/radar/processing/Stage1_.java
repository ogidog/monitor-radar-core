
package org.myapp.satellite.radar.processing;

import org.esa.snap.core.datamodel.Product;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;


public class Stage1_ {

    public static void main(String[] args) {

        /* outputDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing"
snapDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\.snap"
configDir="F:\\intellij-idea-workspace\\monitor-radar-core-v3\\config"
filesList="Y:\\Satellites\\Sentinel-1A\\S1A_IW_SLC__1SDV_20170122T002755_20170122T002824_014937_018613_A687.zip,Y:\\Satellites\\Sentinel-1A\\S1A_IW_SLC__1SDV_20170215T002754_20170215T002824_015287_0190E5_24DE.zip"*/

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String outputDir = consoleParameters.get("outputDir").toString();
        String snapDir = consoleParameters.get("snapDir").toString();
        String configDir = consoleParameters.get("configDir").toString();
        String filesList = consoleParameters.get("filesList").toString();

        HashMap parameters = getParameters(configDir);
        if (parameters == null){
            System.out.println("Fail to read parameters.");
            return;
        };

        String[] files = filesList.split(",");

        TOPSARSplitOpEnv_ topsarSplitOpEnv = new TOPSARSplitOpEnv_();
        ApplyOrbitFileOpEnv applyOrbitFileOpEnv = new ApplyOrbitFileOpEnv(snapDir);
        WriteOpEnv writeOpEnv = new WriteOpEnv();

        Product targetProduct;

        for (int i = 0; i < files.length; i++) {
            try {

                targetProduct = topsarSplitOpEnv.getTargetProduct(files[i], parameters);
                targetProduct = applyOrbitFileOpEnv.getTargetProduct(targetProduct, parameters);

                if (targetProduct != null) {
                    writeOpEnv.write(outputDir + File.separator + "applyorbitfile", targetProduct);
                }

                targetProduct.closeIO();

            } catch (Exception e) {
                e.printStackTrace();
            }

            applyOrbitFileOpEnv.Dispose();
            topsarSplitOpEnv.Dispose();
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

            // TOPSARSplit
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(configDir + File.separator + "s1_tops_split.json");
            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap jsonParameters = (HashMap) jsonObject.get("parameters");

            HashMap parameters = new HashMap();
            parameters.put("selectedPolarisations", ((HashMap) jsonParameters.get("selectedPolarisations")).get("value"));
            stageParameters.put("TOPSARSplit", parameters);

            fileReader.close();

            // ApplyOrbitFile
            fileReader = new FileReader(configDir + File.separator + "apply_orbit_file.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            parameters = new HashMap();
            parameters.put("polyDegree", Integer.valueOf(((HashMap) jsonParameters.get("polyDegree")).get("value").toString()));
            parameters.put("continueOnFail", Boolean.valueOf(((HashMap) jsonParameters.get("continueOnFail")).get("value").toString()));
            parameters.put("orbitType", ((HashMap) jsonParameters.get("orbitType")).get("value"));
            stageParameters.put("ApplyOrbitFile", parameters);

            fileReader.close();

            // Subset
            fileReader = new FileReader(configDir + File.separator + "subset.json");
            jsonObject = (JSONObject) parser.parse(fileReader);
            jsonParameters = (HashMap) jsonObject.get("parameters");

            String[] geoRegionCoordinates = ((HashMap) jsonParameters.get("geoRegion")).get("value").toString().split(",");
            parameters = new HashMap();
            parameters.put("topLeftLat", Double.valueOf(geoRegionCoordinates[0].trim().split(" ")[1]));
            parameters.put("topLeftLon", Double.valueOf(geoRegionCoordinates[0].trim().split(" ")[0]));
            parameters.put("topRightLat", Double.valueOf(geoRegionCoordinates[1].trim().split(" ")[1]));
            parameters.put("topRightLon", Double.valueOf(geoRegionCoordinates[1].trim().split(" ")[0]));
            parameters.put("bottomLeftLat", Double.valueOf(geoRegionCoordinates[2].trim().split(" ")[1]));
            parameters.put("bottomLeftLon", Double.valueOf(geoRegionCoordinates[2].trim().split(" ")[0]));
            parameters.put("bottomRightLat", Double.valueOf(geoRegionCoordinates[3].trim().split(" ")[1]));
            parameters.put("bottomRightLon", Double.valueOf(geoRegionCoordinates[3].trim().split(" ")[0]));
            parameters.put("topLeftLat1", Double.valueOf(geoRegionCoordinates[4].trim().split(" ")[1]));
            parameters.put("topLeftLon1", Double.valueOf(geoRegionCoordinates[4].trim().split(" ")[0]));
            stageParameters.put("Subset", parameters);

            /* parameters.put("topLeftLat", 55.60507332069096);
            parameters.put("topLeftLon", 86.1867704184598);
            parameters.put("topRightLat", 55.6487070962106);
            parameters.put("topRightLon", 86.18718760125022);
            parameters.put("bottomLeftLat", 55.64874125567167);
            parameters.put("bottomLeftLon", 86.08502696051652);
            parameters.put("bottomRightLat", 55.60510658714328);
            parameters.put("bottomRightLon", 86.08502696051652);
            parameters.put("topLeftLat1", 55.60507332069096);
            parameters.put("topLeftLon1", 86.1867704184598); */

            fileReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}