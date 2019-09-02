package utils;

import ij.IJ;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Marker {

    private static String invertY(String y, int height) {
        return Integer.toString(height - (int) Float.parseFloat(y));
    }

    public static ArrayList<int[]> readMarker(String markerPath, int imgHeight) throws IOException {

        ArrayList<int[]> rows = new ArrayList<>();
        IJ.log("Reading marker " + markerPath + "...");

        String row;
        BufferedReader csvReader = new BufferedReader(new FileReader(markerPath));
        //  skip header
        String header = csvReader.readLine();
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(",");

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
        }
        csvReader.close();
        IJ.log("Read " + rows.size() + " rows from " + markerPath);
        return rows;
    }

    public static ArrayList<int[]> readMarker(String markerPath) throws IOException {
        return Marker.readMarker(markerPath, 0);
    }
}
