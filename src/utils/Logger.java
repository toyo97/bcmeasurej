package utils;

import ij.IJ;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Logger {

    private boolean verbose;
    private ArrayList<String> logs;

    public Logger(boolean verbose) {
        this.verbose = verbose;
        logs = new ArrayList<>();
    }

    public void log(String message) {
        if (verbose)
            IJ.log(message);
        else
            logs.add(message);
    }

    public void writeLogFile(String targetDir) {
        try {
            FileWriter csvWriter = new FileWriter(targetDir + "/log.txt");

            for (String row : logs) {
                csvWriter.append(row);
                csvWriter.append("\n");
            }

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            IJ.log("No file written: " + e.getMessage());
        }

    }
}
