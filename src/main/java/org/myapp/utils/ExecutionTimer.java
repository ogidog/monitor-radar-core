package org.myapp.utils;

import java.util.Date;
import java.util.Locale;

public class ExecutionTimer {
    long startTime, endTime;

    public ExecutionTimer() {
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
    }

    public long result() {
        return endTime - startTime;
    }

    public String getStartTime() {
        Date d = new Date();
        d.setTime(startTime);
        return d.toString();
    }

    public String getEndTime() {
        Date d = new Date();
        d.setTime(endTime);
        return d.toString();
    }
}
