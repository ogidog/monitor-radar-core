package org.myapp.satellite.radar.sbas;

import org.myapp.utils.ConsoleArgsReader;
import org.myapp.utils.Routines;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

public class Stage5 {

    public static void main(String[] args) {

        try {

            HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
            String outputDir = consoleParameters.get("outputDir").toString();

            String snaphuexportDir = outputDir + File.separator + "snaphuexport";
            String stage5Dir = outputDir + "" + File.separator + "stage5";

            String[] files = Files.walk(Paths.get(snaphuexportDir)).filter(file -> file.toString().endsWith(".conf"))
                    .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

            if (Files.exists(Paths.get(stage5Dir))) {
                Files.walk(Paths.get(stage5Dir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            new File(stage5Dir).mkdirs();

            PrintWriter cmdWriter = new PrintWriter(stage5Dir + File.separator + "stage5.cmd", "UTF-8");
            for (int i = 0; i < files.length; i++) {
                BufferedReader br = new BufferedReader(new FileReader(files[i]));
                for (String line; (line = br.readLine()) != null; ) {
                    if (line.contains("snaphu.conf")) {
                        String snaphuConfDir = Paths.get(files[i]).getParent().toString().trim();
                        cmdWriter.println("cd " + snaphuConfDir);
                        cmdWriter.println(line.replace("#", "").trim());
                        break;
                    }
                }
            }
            cmdWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static void process(String outputDir, String taskId) throws Exception {

        String taskDir = outputDir + File.separator + taskId;
        String snaphuexportDir = taskDir + File.separator + "snaphuexport";
        String stage5Dir = taskDir + "" + File.separator + "stage5";

        String[] files = Files.walk(Paths.get(snaphuexportDir)).filter(file -> file.toString().endsWith(".conf"))
                .map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);

        if (Files.exists(Paths.get(stage5Dir))) {
            Routines.deleteDir(new File(stage5Dir));
        }
        new File(stage5Dir).mkdirs();

        for (int i = 0; i < files.length; i++) {
            BufferedReader br = new BufferedReader(new FileReader(files[i]));
            for (String line; (line = br.readLine()) != null; ) {
                if (line.contains("snaphu.conf")) {
                    String snaphuConfDir = Paths.get(files[i]).getParent().toString().trim();
                    Routines.runScript("cd " + snaphuConfDir + "; " + line.replace("#", "").trim() + ";", "", "Stage5");
                    break;
                }
            }
        }
    }
}
