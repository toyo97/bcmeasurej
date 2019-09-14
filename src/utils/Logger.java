package utils;

import ij.IJ;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Logger {

    private boolean verbose;
    private ArrayList<String> logs;
    private static Logger logger;

    /**
     * Singleton for custom logger.
     *
     * @param verbose If set to true, the class will use IJ.log() as logging tool, otherwise it will simply
     *                save the messages in a txt file at the end of the execution
     */
    private Logger(boolean verbose) {
        this.verbose = verbose;
        logs = new ArrayList<>();
    }

    public static Logger getInstance(boolean verbose) {
        if (logger == null) {
            logger = new Logger(verbose);
        }
        return logger;
    }

    public static Logger getInstance() {
        if (logger == null) {
            logger = new Logger(true);
            logger.log("Verbose parameter not given, set default verbose mode.");
        }
        return logger;
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
