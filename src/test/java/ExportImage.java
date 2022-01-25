
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ExportImage {

    public static void main(String[] args) {

        dim2img("D:\\mnt\\fast\\dockers\\monitor-radar-core\\ogidog_mail_ru\\1643082483404\\applyorbitfile\\20190705_sub.dim");
    }

    public static void dim2img(String dimFile) {
        try {
            ProgressHandle handle = ProgressHandleFactory.createSystemHandle("");
            ProgressMonitor pm = new ProgressHandleMonitor(handle);

            Product product = ProductIO.readProduct(dimFile);
            Band intensityBand = product.getBandAt(0);
            intensityBand.readRasterDataFully();

            BufferedImage bi = intensityBand.createRgbImage(pm);
            BufferedImage resized = resize(bi, 1500, 500);

            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("JPG").next();
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();

            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(0.7f);

            jpgWriter.setOutput(ImageIO.createImageOutputStream(new File("test.jpg")));
            jpgWriter.write(null, new IIOImage(resized, null, null), jpgWriteParam);
            jpgWriter.dispose();

            //ImageIO.write(bi, "jpg", new File("test.jpg"));
            product.closeIO();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static BufferedImage resize(BufferedImage img, int width, int height) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }
}
