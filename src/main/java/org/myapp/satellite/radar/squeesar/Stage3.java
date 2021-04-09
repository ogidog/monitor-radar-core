package org.myapp.satellite.radar.squeesar;

import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

public class Stage3 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String squeesarDir = outputDir + File.separator + "squeesar";
            String backgeocodingDir = outputDir + "" + File.separator + "backgeocoding";

            if (Files.exists(Paths.get(squeesarDir))) {
                Files.walk(Paths.get(squeesarDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(squeesarDir).mkdirs();

            String[] iSlvHdrFiles = Files.walk(Paths.get(backgeocodingDir), 10).filter(path -> {
                if (path.toString().endsWith(".hdr") && path.getFileName().toString().startsWith("i_") && path.toString().contains("slv1")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            String iMstHdrFile = Files.walk(Paths.get(backgeocodingDir), 10).filter(path -> {
                if (path.toString().endsWith(".hdr") && path.getFileName().toString().startsWith("i_") && path.toString().contains("mst")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new)[0];

            String[] qSlvHdrFiles = Files.walk(Paths.get(backgeocodingDir), 10).filter(path -> {
                if (path.toString().endsWith(".hdr") && path.getFileName().toString().startsWith("q_") && path.toString().contains("slv1")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            String qMstHdrFile = Files.walk(Paths.get(backgeocodingDir), 10).filter(path -> {
                if (path.toString().endsWith(".hdr") && path.getFileName().toString().startsWith("q_") && path.toString().contains("mst")) {
                    return true;
                } else {
                    return false;
                }
            }).map(path -> path.toAbsolutePath().toString()).toArray(String[]::new)[0];

            PrintWriter inputIQFilesWriter = new PrintWriter(squeesarDir + File.separator + "input_i_q_files.txt", "UTF-8");
            for (int i = 0; i < iSlvHdrFiles.length; i++) {
                inputIQFilesWriter.println(iSlvHdrFiles[i] + ";" + qSlvHdrFiles[i]);
            }
            inputIQFilesWriter.println(iMstHdrFile + ";" + qMstHdrFile);
            inputIQFilesWriter.close();

            return;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
