package org.myapp.utils;

/*  Class is used for composing interferometric pairs
    according baseline and timestamp parameters
    for a coregistration procedure in the SBAS method.
*/

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;
import java.util.Arrays;

public class ImagesPairsComposing {

    public static void main(String[] args) {

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

        Product[] products = Arrays.stream(fileList.split(",")).map(file -> {
            try {
                return ProductIO.readProduct(file);
            } catch (Exception e) {
                return null;
            }
        }).toArray(Product[]::new);

        try {
            InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(products);
            Product master = InSARStackOverview.findOptimalMasterProduct(products);
            System.out.println(stackOverview[0].getMasterSlave()[1].getDopplerDifference());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
