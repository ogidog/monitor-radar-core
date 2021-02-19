import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.myapp.utils.ConsoleArgsReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.HashMap;

public class TestCopyFilesFromDataset {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String configDir = consoleParameters.get("configDir").toString();
            String outputDir = consoleParameters.get("outputDir").toString();

            JSONParser parser = new JSONParser();
            FileReader fileReader = null;
            fileReader = new FileReader(configDir + File.separator + "dataset.json");

            JSONObject jsonObject = (JSONObject) parser.parse(fileReader);
            HashMap<String, HashMap> jsonParameters = (HashMap) jsonObject.get("parameters");

            String[] filesList = ((String) ((JSONObject) jsonParameters.get("filesList")).get("value")).replace("/mnt/satimg/", "Y:\\").replace("/", "\\").split(",");

            for (int i = 0; i < filesList.length; i++) {
                System.out.print(Paths.get(filesList[i]).getFileName() + " ...");
                Files.copy(Paths.get(filesList[i]), Paths.get(outputDir + File.separator + "data" + File.separator + Paths.get(filesList[i]).getFileName()), StandardCopyOption.REPLACE_EXISTING);
                System.out.println(" done.");
            }

            PrintWriter cmdWriter = new PrintWriter(outputDir + File.separator + "data" + File.separator + "unzip_all.cmd", "UTF-8");
            for (int i = 0; i < filesList.length; i++) {
                cmdWriter.println("powershell -command \"Expand-Archive " + outputDir + File.separator + "data" + File.separator + Paths.get(filesList[i]).getFileName() + " "
                        + outputDir + File.separator + "SLC");

            }
            cmdWriter.close();

            return;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
