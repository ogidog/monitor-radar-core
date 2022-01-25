package org.myapp.utils;

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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

public class Common {

    public enum TaskStatus {

        COMPLETED("Completed"),
        ERROR("Error"),
        PROCESSING("Processing");

        public final String label;

        private TaskStatus(String label) {
            this.label = label;
        }
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    public static String getGPTScriptName() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows")) {
            return "gpt.exe";
        } else {
            return "/usr/local/snap/bin/gpt";
        }
    }

    public static void runGPTScript(String graphFile, String stageName) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(getGPTScriptName(), graphFile);
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException(stageName + ": execution of GPT script failed");
        }
    }

    public static void runScript(String scriptFile, String param, String stageName) throws Exception {
        ProcessBuilder pb;
        pb = new ProcessBuilder(scriptFile, param);
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException(stageName + " : execution of script failed");
        }
    }

    public static void runScript(String[] command, String execDir, String stageName) throws Exception {
        ProcessBuilder pb;
        pb = new ProcessBuilder(command);
        pb.directory(new File(execDir));
        pb.inheritIO();
        Process process = pb.start();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            // check for errors
            new BufferedInputStream(process.getErrorStream());
            throw new RuntimeException(stageName + " : execution of script failed");
        }
    }

    public static void writeStatus(String taskDir, TaskStatus status, String message) {
        try {

            PrintWriter pr;
            BufferedReader br;

            switch (status) {
                case COMPLETED:
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("PROCESSING").label));
                    pr = new PrintWriter(taskDir + File.separator + TaskStatus.valueOf("COMPLETED").label);
                    pr.print("");
                    pr.flush();
                    pr.close();

                    br = new BufferedReader(new FileReader(taskDir + File.separator + "start_time"));
                    long startTimeInSeconds = Long.valueOf(br.readLine());
                    br.close();
                    long totalElapsedTime = Instant.now().getEpochSecond() - startTimeInSeconds;
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "total_elapsed_time"));
                    pr = new PrintWriter(taskDir + File.separator + "total_elapsed_time");
                    pr.print(totalElapsedTime);
                    pr.flush();
                    pr.close();

                    break;

                case PROCESSING:
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("COMPLETED").label));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("ERROR").label));
                    pr = new PrintWriter(taskDir + File.separator + TaskStatus.valueOf("PROCESSING").label);
                    pr.print("");
                    pr.flush();
                    pr.close();

                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "start_time"));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "total_elapsed_time"));
                    pr = new PrintWriter(taskDir + File.separator + "start_time");
                    pr.print(Instant.now().getEpochSecond());
                    pr.flush();
                    pr.close();

                    break;

                case ERROR:
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "start_time"));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + "total_elapsed_time"));

                    Files.deleteIfExists(Paths.get(taskDir + TaskStatus.valueOf("COMPLETED").label));
                    Files.deleteIfExists(Paths.get(taskDir + File.separator + TaskStatus.valueOf("PROCESSING").label));
                    pr = new PrintWriter(taskDir + File.separator + TaskStatus.valueOf("ERROR").label);
                    pr.print(message);
                    pr.flush();
                    pr.close();
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkPreviousErrors(String resultDir) {
        if (Files.exists(Paths.get(resultDir + File.separator + TaskStatus.valueOf("ERROR").label))) {
            return true;
        } else {
            return false;
        }
    }

    public static void exportProductToImg(String ProductFile, String resultDir, float resizeFactor, float compressFactor, String imageFormat) {
        try {

            ProgressHandle handle = ProgressHandleFactory.createSystemHandle("");
            ProgressMonitor pm = new ProgressHandleMonitor(handle);

            Product product = ProductIO.readProduct(ProductFile);
            Band intensityBand = product.getBandAt(0);
            int width = intensityBand.getRasterWidth();
            int height = intensityBand.getRasterHeight();
            intensityBand.readRasterDataFully();

            BufferedImage bi = intensityBand.createRgbImage(pm);
            BufferedImage resized = resize(bi, (int) (width * resizeFactor), (int) (height * resizeFactor));

            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName(imageFormat).next();
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();

            if (jpgWriteParam.canWriteCompressed()) {
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(compressFactor);
            }

            jpgWriter.setOutput(ImageIO.createImageOutputStream(new File(resultDir + File.separator + product.getName() + ".jpg")));
            jpgWriter.write(null, new IIOImage(resized, null, null), jpgWriteParam);
            jpgWriter.dispose();

            product.closeIO();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static BufferedImage resize(BufferedImage img, int width, int height) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.SCALE_SMOOTH);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }
}
