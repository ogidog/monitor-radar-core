import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.esa.s1tbx.sentinel1.gpf.TOPSARSplitOp;
import org.esa.s1tbx.sentinel1.gpf.BackGeocodingOp;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestForArticle {

    public static void main(String args[]) {
        try {
            String s1aImg1Path = "/mnt/hdfs/s1adata/" +
                    "S1B_IW_SLC__1SDV_20180405T002722_20180405T002752_010341_012D2E_4CAC.zip.zip";

            Product sourceProduct1 = ProductIO.readProduct(new File(s1aImg1Path));

            Operator op1 = (TOPSARSplitOp) (new TOPSARSplitOp.Spi().createOperator());
            // ... настройка параметров оператора
            {
                op1.setParameter("selectedPolarisations", "VV");
                op1.setParameter("subswath", "IW");
                op1.setParameter("firstBurstIndex", 1);
                op1.setParameter("lastBurstIndex", 2);
            }
            Product targetProduct1 = op1.getTargetProduct();
            Operator op11 = (ApplyOrbitFileOp) (new ApplyOrbitFileOp.Spi().createOperator());
            op11.setSourceProduct(targetProduct1);
            Product targetProduct11 = op1.getTargetProduct();

            {
                Operator op2 = (TOPSARSplitOp) (new TOPSARSplitOp.Spi().createOperator());
                Product targetProduct2 = op2.getTargetProduct();
                Operator op22 = (ApplyOrbitFileOp) (new ApplyOrbitFileOp.Spi().createOperator());
                op22.setSourceProduct(targetProduct1);
                Product targetProduct22 = op1.getTargetProduct();
            }

            {
                Operator op3 = (BackGeocodingOp) (new BackGeocodingOp.Spi().createOperator());
                op3.setSourceProducts(new Product[]{targetProduct11, targetProduct11});
                {
                    op3.setParameter("demName", "SRTM 3Sec");
                    op3.setParameter("demResamplingMethod", "BICUBIC_INTERPOLATION");
                    op3.setParameter("resamplingType", "BISINC_5_POINT_INTERPOLATION");
                }
                Product targetProduct3 = op3.getTargetProduct();

                ProductIO.writeProduct(targetProduct3,
                        "/mnt/hdfs/output/20180405_20190412_Orb_Stack_DInSAR.dim",
                        "BEAM-DIMAP");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        {
            String[] s1aImgPaths = new String[]{
                    "/mnt/hdfs/s1adata/S1B_IW_SLC__1SDV_20180405T002722_20180405T002752_010341_012D2E_4CAC.zip",
                    "/mnt/hdfs/s1adata/S1B_IW_SLC__1SDV_20180417T002722_20180417T002752_010516_0132C3_A59B.zip",
                    // ....
                    "/mnt/hdfs/s1adata/S1B_IW_SLC__1SDV_20190412T002729_20190412T002759_015766_01D977_02DF.zip"
            };
            SparkConf sparkConf = new SparkConf();
            JavaSparkContext sc = new JavaSparkContext(sparkConf);
            List<Boolean> topsSplitResults = sc.parallelize(Arrays.asList(s1aImgPaths), 32)
                    .map(new TOPSSplitFunction("/mnt/hdfs/proc/tops/"))
                    .collect();
        }
    }

    static class TOPSSplitFunction implements Function<String, Boolean> {
        private String pathToSaveResult;
        private String savedFileName;

        TOPSSplitFunction(String pathToSaveResult) {
            this.pathToSaveResult = pathToSaveResult;
        }

        @Override
        public Boolean call(String pathToS1AImage) {
            boolean result = false;
            // Формируем имя файла результата из переменной pathToS1AImage
            {
                savedFileName = savedFileName + "dim";
                System.out.println(savedFileName);
            }
            try {
                Product sourceProduct1 = ProductIO.readProduct(new File(pathToS1AImage));

                Operator op1 = (TOPSARSplitOp) (new TOPSARSplitOp.Spi().createOperator());
                {
                    op1.setParameter("selectedPolarisations", "VV");
                    op1.setParameter("subswath", "IW");
                    op1.setParameter("firstBurstIndex", 1);
                    op1.setParameter("lastBurstIndex", 2);
                }
                Product targetProduct1 = op1.getTargetProduct();
                ProductIO.writeProduct(targetProduct1, this.pathToSaveResult + savedFileName,
                        "BEAM-DIMAP");
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }
}

