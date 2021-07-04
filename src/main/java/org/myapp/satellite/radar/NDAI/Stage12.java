package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.CustomErrorHandler;

import java.io.*;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Stage12 {
    public static void main(String[] args) {

        if(CustomErrorHandler.checkPreviousErrors()){
            return;
        }

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String kmlObjectUrl = consoleParameters.get("kmlObjectUrl").toString();

            Product tcfilteredavgndaiProduct = ProductIO.readProduct(outputDir + File.separator + "avgndai" + File.separator + "tcfilteredavgndai.tif");
            String first_far_long = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_long");
            String first_far_lat = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_lat");
            String last_near_long = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_long");
            String last_near_lat = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_lat");
            tcfilteredavgndaiProduct.closeIO();
            tcfilteredavgndaiProduct.dispose();

            File file = new File(configDir + File.separator + "ndai.kml");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String ndaiKML = br.lines().map(line -> {
                if (line.contains("<north>")) {
                    line = "<north>" + first_far_lat + "</north>";
                }
                if (line.contains("<east>")) {
                    line = "<east>" + first_far_long + "</east>";
                }
                if (line.contains("<south>")) {
                    line = "<south>" + last_near_lat + "</south>";
                }
                if (line.contains("<west>")) {
                    line = "<west>" + last_near_long + "</west>";
                }
                return line;
            }).collect(Collectors.joining());
            br.close();
            String ndaiKMLOnline = ndaiKML.replace("legend.png", kmlObjectUrl + "/legend.png")
                    .replace("tcfilteredavgndai_new.png", kmlObjectUrl + "/tcfilteredavgndai_new.png");

            file = new File(outputDir + File.separator + "avgndai" + File.separator + "ndai.kml");
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(ndaiKML);
            bw.flush();
            bw.close();
            file = new File(outputDir + File.separator + "avgndai" + File.separator + "ndai_online.kml");
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(ndaiKMLOnline);
            bw.flush();
            bw.close();

            // System.out.printf(first_far_lat + "," + first_far_long + "," + last_near_lat + "," + last_near_long);

        } catch (Exception e) {
            CustomErrorHandler.writeErrorToFile(e.getMessage(), "/mnt/task" + File.separator + "ERROR");
            e.printStackTrace();
        }
    }
}
