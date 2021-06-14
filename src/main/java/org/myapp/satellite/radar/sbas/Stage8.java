package org.myapp.satellite.radar.sbas;

import org.myapp.utils.ConsoleArgsReader;

import java.io.*;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Stage8 {
    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String kmlObjectUrl = consoleParameters.get("kmlObjectUrl").toString();

            File file = new File(outputDir + File.separator + "prep" + File.separator + "velocity.kml");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String velocityKML = br.lines().map(line -> {
                if (line.contains("<href>velocity.png</href>")) {
                    line = "<href>" + kmlObjectUrl + "/velocity.png</href>";
                }
                if (line.contains("<href>velocity_cbar.png</href>")) {
                    line = "<href>" + kmlObjectUrl + "/velocity_cbar.png</href>";
                }
                return line;
            }).collect(Collectors.joining());
            br.close();

            file = new File(outputDir + File.separator + "prep" + File.separator + "velocity_online.kml");
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(velocityKML);
            bw.flush();
            bw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
