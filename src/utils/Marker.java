package utils;

import ij.IJ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utils.Logger;

public class Marker {

    private static String invertY(String y, int height) {
        return Integer.toString(height - (int) Float.parseFloat(y));
    }


    /**
     * Extract the seeds coordinates from a csv file
     *
     * @param markerPath absolute path of the .marker file
     * @param imgHeight  height of the image if y coordinates inversion is requested. If 0 or less is passed, no inversion is done
     * @return rows of the .marker file coordinates casted to int values
     * @throws IOException when no marker file is found
     */
    public static ArrayList<int[]> readMarker(String markerPath, int imgHeight) throws IOException {

        ArrayList<int[]> rows = new ArrayList<>();

        String row;
        BufferedReader csvReader = new BufferedReader(new FileReader(markerPath));
        //  skip header
        String header = csvReader.readLine();
        while ((row = csvReader.readLine()) != null) {
            try {
                String[] data = row.split(",");

                if (data.length < 3)
                    throw new ArrayIndexOutOfBoundsException();

                //  y inversion
                if (imgHeight > 0) {
                    data[1] = invertY(data[1], imgHeight);
                }

                //  takes only the x,y,z coordinates and convert them to int values
                int[] coords = new int[3];
                for (int i = 0; i < 3; i++) {
                    coords[i] = (int) Float.parseFloat(data[i]);
                }
                rows.add(coords);
            } catch (ArrayIndexOutOfBoundsException e) {
                Logger.getInstance().log("Skipped invalid line in marker " + markerPath);
            }
        }
        csvReader.close();
        return rows;
    }

    /**
     * Same as above but when no y inversion is needed
     */
    public static ArrayList<int[]> readMarker(String markerPath) throws IOException {
        return Marker.readMarker(markerPath, 0);
    }

    /**
     * Write rows containing 3D coordinates of the center and radius of each cell in a CSV file
     *
     * @param markerPath output file path
     * @param data       rows to be written
     */
    public static void writeMarker(String markerPath, List<List<String>> data) {
        try {
            FileWriter csvWriter = new FileWriter(markerPath);
            csvWriter.append("x,y,z,r,seed").append("\n");

            for (List<String> row : data) {
                csvWriter.append(String.join(",", row));
                csvWriter.append("\n");
            }

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            IJ.error("No file written: " + e.getMessage());
        }
    }

    public static ArrayList<int[]> readRadMarker(String markerPath) throws IOException {
        ArrayList<int[]> rows = new ArrayList<>();

        String row;
        BufferedReader csvReader = new BufferedReader(new FileReader(markerPath));
        //  skip header
        String header = csvReader.readLine();
        while ((row = csvReader.readLine()) != null) {
            try {
                String[] data = row.split(",");

                if (data.length < 4)
                    throw new ArrayIndexOutOfBoundsException();

                //  takes only the x,y,z coordinates and radius and convert them to int values
                int[] res = new int[4];
                for (int i = 0; i < 4; i++) {
                    res[i] = (int) Float.parseFloat(data[i]);
                }
                rows.add(res);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        csvReader.close();
        return rows;
    }
}
