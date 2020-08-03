package org.myapp.satellite.radar.sbas;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Stage3 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();
            String configDir = consoleParameters.get("configDir").toString();
            String graphDir = consoleParameters.get("graphDir").toString();
            String filesList = consoleParameters.get("filesList").toString();

            HashMap parameters = getParameters(configDir);
            if (parameters == null) {
                System.out.println("Fail to read parameters.");
                return;
            }

            String[] files;
            if (!filesList.contains(",")) {
                files = Files.walk(Paths.get(filesList)).skip(1)
                        .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
            } else {
                files = filesList.split(",");
            }

            String intfDir = outputDir+ File.separator + "intf";
            if (Files.exists(Paths.get(intfDir))) {
                Files.walk(Paths.get(intfDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            String stage3Dir = outputDir + "" + File.separator + "stage3";

            new File(intfDir).mkdirs();
            new File(stage3Dir).mkdirs();

        }catch(Exception e){
            e.printStackTrace();
            return;
        }
    }

    static HashMap getParameters(String configDir) {

        HashMap<String, HashMap> stageParameters = null;

        try {
            stageParameters = new HashMap<>();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stageParameters;
    }

}
