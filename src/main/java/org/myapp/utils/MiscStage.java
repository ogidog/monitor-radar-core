package org.myapp.utils;

import java.util.HashMap;

public class MiscStage {

    public static void main(String[] args){

        HashMap consoleParameters = ConsoleArgsReader.readConsoleArgs(args);
        String outputDir = consoleParameters.get("outputDir").toString();
        String graphDir = consoleParameters.get("graphDir").toString();
        String filesList = consoleParameters.get("filesList").toString();
    }

}
