
package org.myapp.satellite.radar.processing;

import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;


public class Stage1 {

    public static void main(String[] args) {

        String outputDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\processing";
        String snapDir = "F:\\intellij-idea-workspace\\monitor-radar-core-v3\\.snap";

        String filesList = "Y:\\Satellites\\Sentinel-1A\\S1A_IW_SLC__1SDV_20161229T002757_20161229T002826_014587_017B50_58CD.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1A_IW_SLC__1SDV_20170122T002755_20170122T002824_014937_018613_A687.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1A_IW_SLC__1SDV_20170215T002754_20170215T002824_015287_0190E5_24DE.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170104T002713_20170104T002743_003691_00656D_5FF1.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170128T002712_20170128T002742_004041_006FC5_0AD8.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170221T002712_20170221T002742_004391_007A3B_F1C3.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170305T002715_20170305T002745_004566_007F52_C5E6.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170317T002715_20170317T002745_004741_00847E_1A15.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170329T002715_20170329T002745_004916_00897B_465E.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170410T002716_20170410T002746_005091_008E88_DDF0.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170422T002716_20170422T002746_005266_009396_C5C0.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170504T002717_20170504T002747_005441_00989C_8A85.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170516T002718_20170516T002747_005616_009D5F_0717.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170528T002718_20170528T002748_005791_00A26E_FACB.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170609T002719_20170609T002749_005966_00A781_C46F.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170621T002720_20170621T002749_006141_00AC9F_5C35.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170703T002720_20170703T002750_006316_00B1B0_FAE2.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170715T002721_20170715T002751_006491_00B6A0_C8F0.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170727T002722_20170727T002752_006666_00BB9E_731D.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170808T002722_20170808T002752_006841_00C0AA_87D7.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170820T002723_20170820T002753_007016_00C5C6_4932.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170901T002723_20170901T002753_007191_00CAD2_0AD4.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170913T002724_20170913T002754_007366_00CFF7_B786.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20170925T002724_20170925T002754_007541_00D503_1DDA.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20171007T002725_20171007T002754_007716_00DA0B_3579.zip,"
                + "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20171019T002725_20171019T002755_007891_00DF03_0416.zip";

        HashMap parameters = getParameters(snapDir);
        String[] files = filesList.split(",");

        TOPSARSplitOpEnv topsarSplitOpEnv = new TOPSARSplitOpEnv();
        ApplyOrbitFileOpEnv applyOrbitFileOpEnv = new ApplyOrbitFileOpEnv(parameters);
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
                System.out.println(e.getMessage());
            }

            applyOrbitFileOpEnv.Dispose();
            topsarSplitOpEnv.Dispose();
        }
    }

    static HashMap getParameters(String snapDir) {

        // TODO: Загружать параметры из json-файла

        HashMap parameters = new HashMap();

        // TOPSARSplit
        parameters.put("selectedPolarisations", "VV");

        // ApplyOrbitFile
        parameters.put("snap.userdir", snapDir);
        parameters.put("polyDegree", 3);
        parameters.put("continueOnFail", false);

        // Subset
        parameters.put("topLeftLat", 55.60507332069096);
        parameters.put("topLeftLon", 86.1867704184598);
        parameters.put("topRightLat", 55.6487070962106);
        parameters.put("topRightLon", 86.18718760125022);
        parameters.put("bottomLeftLat", 55.64874125567167);
        parameters.put("bottomLeftLon", 86.08502696051652);
        parameters.put("bottomRightLat", 55.60510658714328);
        parameters.put("bottomRightLon", 86.08502696051652);
        parameters.put("topLeftLat1", 55.60507332069096);
        parameters.put("topLeftLon1", 86.1867704184598);

        return parameters;
    }

}
