/*  Class is used for composing interferometric pairs
    according baseline and timestamp parameters
    for a coregistration procedure in the SBAS method.
*/

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeseriesGraph {

    public static void main(String[] args) {
        intfPairCorrecting();
        // intfPairComposing();
    }

    public static void intfPairCorrecting() {

        String pairsStr = "";
        TreeMap<String, ArrayList> pairs = new TreeMap<>();
        Pattern datePattern = Pattern.compile("(\\d\\d\\d\\d\\d\\d\\d\\d)");

        // Read pairs.txt
        try {
            pairsStr = new String(Files.readAllBytes(Paths.get("pairs.txt")));
            Arrays.stream(pairsStr.split(";")).forEach(pair -> {
                String masterDate, slaveDate;

                Matcher dateMatcher = datePattern.matcher(pair.split(",")[0]);
                dateMatcher.find();
                masterDate = dateMatcher.group();

                dateMatcher = datePattern.matcher(pair.split(",")[1]);
                dateMatcher.find();
                slaveDate = dateMatcher.group();

                ArrayList<String> slaves = null;

                if (!pairs.containsKey(masterDate)) {
                    slaves = new ArrayList();
                    slaves.add(slaveDate);
                    pairs.put(masterDate, slaves);
                } else {
                    pairs.get(masterDate).add(slaveDate);
                }

                System.out.println();

            });

            String[] masterKeys = pairs.keySet().stream().toArray(String[]::new);
            String row = "", column = "";

            for (int i = 0; i < masterKeys.length; i++) {
                int rowSize = pairs.get(masterKeys[i]).size();
                for (int j = 0; j < masterKeys.length; j++) {
                    if (j < rowSize) {
                        row = row + pairs.get(masterKeys[i]).get(j);
                    }
                }
            }

            System.out.println(pairs);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void intfPairComposing() {

        String fileList = "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190106T002729_20190106T002759_014366_01ABBB_3DE0.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190118T002729_20190118T002759_014541_01B15F_2552.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190130T002729_20190130T002759_014716_01B6FF_32CF.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190211T002728_20190211T002758_014891_01BCBF_EF14.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190223T002728_20190223T002758_015066_01C279_1629.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190307T002728_20190307T002758_015241_01C842_51EA.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190319T002728_20190319T002758_015416_01CDED_4F45.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190331T002729_20190331T002758_015591_01D3A4_F37A.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190412T002729_20190412T002759_015766_01D977_02DF.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190424T002729_20190424T002759_015941_01DF44_C2DF.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190506T002730_20190506T002800_016116_01E521_3B49.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190518T002730_20190518T002800_016291_01EA92_0661.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190530T002731_20190530T002801_016466_01EFEC_5A79.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190611T002732_20190611T002802_016641_01F521_0071.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190623T002732_20190623T002802_016816_01FA53_B9AA.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190705T002733_20190705T002803_016991_01FF82_D865.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190717T002734_20190717T002804_017166_0204AA_CDC4.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190729T002735_20190729T002804_017341_0209C7_C915.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190810T002735_20190810T002805_017516_020F11_68A1.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190822T002736_20190822T002806_017691_021488_6C90.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190903T002737_20190903T002806_017866_0219F6_1FEF.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190915T002737_20190915T002807_018041_021F69_E11B.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190927T002738_20190927T002808_018216_0224CE_CE50.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190927T002738_20190927T002808_018216_0224CE_0DDF.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20191009T002738_20191009T002808_018391_022A52_6475.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20191021T002738_20191021T002808_018566_022FB2_C9C2.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20191102T002738_20191102T002808_018741_023530_B861.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20191114T002738_20191114T002808_018916_023AD6_210D.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20191126T002737_20191126T002807_019091_024077_FCDB.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20191208T002737_20191208T002807_019266_0245FF_B721.zip," +
                "Y:\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20191220T002737_20191220T002806_019441_024B94_7F20.zip";

        /*String fileList = "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180405T002722_20180405T002752_010341_012D2E_4CAC.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180417T002722_20180417T002752_010516_0132C3_A59B.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180429T002723_20180429T002753_010691_01385F_CCBB.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180511T002724_20180511T002753_010866_013E05_36C0.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180523T002724_20180523T002754_011041_0143B3_09A3.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180604T002725_20180604T002755_011216_01495F_8B1E.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180616T002726_20180616T002756_011391_014EB9_39F6.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180628T002726_20180628T002756_011566_01542A_002A.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180710T002727_20180710T002757_011741_015999_580C.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180722T002728_20180722T002758_011916_015EF3_AA2E.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180803T002729_20180803T002759_012091_016438_F06B.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180815T002729_20180815T002759_012266_0169A1_1E21.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180827T002730_20180827T002800_012441_016F0F_7233.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180908T002731_20180908T002800_012616_017476_2BFC.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20180920T002731_20180920T002801_012791_0179CD_D5A9.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181002T002731_20181002T002801_012966_017F2B_152B.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181014T002732_20181014T002801_013141_018480_94F7.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181026T002732_20181026T002801_013316_0189E9_9725.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181107T002731_20181107T002801_013491_018F6A_C2DA.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181119T002731_20181119T002801_013666_0194EC_6916.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181201T002731_20181201T002801_013841_019A85_C084.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181213T002730_20181213T002800_014016_01A031_B6C1.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20181225T002730_20181225T002800_014191_01A5FF_E74E.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190106T002729_20190106T002759_014366_01ABBB_3DE0.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190118T002729_20190118T002759_014541_01B15F_2552.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190130T002729_20190130T002759_014716_01B6FF_32CF.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190211T002728_20190211T002758_014891_01BCBF_EF14.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190223T002728_20190223T002758_015066_01C279_1629.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190307T002728_20190307T002758_015241_01C842_51EA.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190319T002728_20190319T002758_015416_01CDED_4F45.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190331T002729_20190331T002758_015591_01D3A4_F37A.zip," +
                "F:\\satimg\\Satellites\\Sentinel-1A\\S1B_IW_SLC__1SDV_20190412T002729_20190412T002759_015766_01D977_02DF.zip";*/

        Product[] products = Arrays.stream(fileList.split(",")).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        String pairsStr = "";

        try {
            InSARStackOverview.IfgPair[] pairs = null;
            InSARStackOverview.IfgPair pair = null;
            InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(products);
            int counter = 0;
            for (int i = 0; i < stackOverview.length; i++) {
                pairs = stackOverview[i].getMasterSlave();
                for (int j = i; j < pairs.length; j++) {
                    pair = pairs[j];
                    /*double bNorm = pair.getPerpendicularBaseline();
                    double bTemp = pair.getTemporalBaseline();
                    double dopplerDiff = pair.getDopplerDifference();
                    double heightAmbiguity = pair.getHeightAmb();
                    double bNormFrac, bTempFrac, dopplerDiffFrac = 0.0, gammaMin = 0.85, gamma;
                    bNormFrac = bNorm / 121 <= 1.0 ? bNorm / 121 : 1;
                    bTempFrac = bTemp / 120 <= 1.0 ? bTemp / 120 : 1;
                    dopplerDiffFrac = dopplerDiff / 325 <= 1.0 ? dopplerDiff / 325 : 1;
                    gamma = (1 - bNormFrac) * (1 - bTempFrac)*(1 - dopplerDiffFrac);*/

                    double coh = pair.getCoherence();
                    double bNorm = pair.getPerpendicularBaseline();
                    double bTemp = pair.getTemporalBaseline();
                    if (pair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString().equals(
                            pair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString())) {
                        continue;
                    }
                    //if (gamma > gammaMin) {
                    // if (coh >= 0.9) {
                    if (Math.abs(bTemp) <= 40) {
                        counter++;
                        /*System.out.println(pair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString()
                                + " - " + pair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString()
                                + " - B_norm: " + pair.getPerpendicularBaseline() + ", B_temp: " + pair.getTemporalBaseline() + ","
                                + " Doppler: " + pair.getDopplerDifference() + ", 2pi Amb: " + pair.getHeightAmb() + ", coh: " + coh);*/
                        pairsStr = pairsStr
                                + pair.getMasterMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString() + ","
                                + pair.getSlaveMetadata().getAbstractedMetadata().getAttribute("PRODUCT").getData().toString() + "," + bNorm + ";";
                    }
                }
                //System.out.println("\n");
            }

            pairsStr = pairsStr.substring(0, pairsStr.length() - 1);
            System.out.println("Total pairs: " + counter);

            // Write pairs to file
            PrintWriter out = new PrintWriter("pairs.txt");
            out.println(pairsStr);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
