package org.myapp.satellite.radar.NDAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Stage12 {
    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();

            Product tcfilteredavgndaiProduct = ProductIO.readProduct(outputDir + File.separator + "avgndai" + File.separator + "tcfilteredavgndai.tif");
            String first_near_long = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_long");
            String first_near_lat = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_near_lat");
            String first_far_long = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_long");
            String first_far_lat = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("first_far_lat");
            String last_far_long = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_far_long");
            String last_far_lat = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_far_lat");
            String last_near_long = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_long");
            String last_near_lat = tcfilteredavgndaiProduct.getMetadataRoot().getElement("Abstracted_Metadata").getAttributeString("last_near_lat");
            tcfilteredavgndaiProduct.closeIO();
            tcfilteredavgndaiProduct.dispose();

            File file = new File(configDir + File.separator + "ndai.kml");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String ndaiKML = br.lines().map(line->{

                return line;
            }).collect(Collectors.joining());

            System.out.printf(first_far_lat + "," + first_far_long + "," + last_near_lat + "," + last_near_long);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }
}
