package org.myapp.utils;

import java.util.Arrays;
import java.util.HashMap;

public class ConsoleArgsReader {

    public static HashMap readConsoleArgs(String[] args) {
        HashMap parameters = new HashMap();

        Arrays.stream(args).forEach(argPair->{
            parameters.put(argPair.split("=")[0], argPair.split("=")[1]);
        });

        return parameters;
    }
}
