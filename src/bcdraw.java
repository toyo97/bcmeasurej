/*
 *     Copyright (C) 2019  Vittorio Zampinetti
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;
import mcib3d.geom.Point3D;
import utils.Marker;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Additional script to make 3D images which shows the results of BCMeasure drawing
 * spheres overlapped with the original image
 */
public class bcdraw {

    public static void main(String[] args) {
        String imgPath = args[0];
        ImagePlus original = IJ.openImage(imgPath);
        Overlay overlay = new Overlay();

        try {
            ArrayList<int[]> cellsData = Marker.readRadMarker(imgPath + "[RAD].marker");

            for (int[] cellData: cellsData) {
                int x = cellData[0];
                int y = cellData[1];
                //  slice selection
                int slicePos = cellData[2] + 1;
                int R = cellData[3];
                int scaledR = (int) R/3;

                PointRoi newCenter = new PointRoi(x, y);
                newCenter.setPosition(slicePos + 1);
                newCenter.setSize(3);
                newCenter.setStrokeColor(new Color(0, 255, 0));

                PointRoi oldCenter = new PointRoi(cellData[4], cellData[5]);
                oldCenter.setPosition(cellData[6]+1);
                oldCenter.setSize(3);
                oldCenter.setStrokeColor(new Color(255, 0, 0));

                overlay.add(newCenter);
                overlay.add(oldCenter);

                for (int i = -scaledR -1; i < scaledR + 1; i++) {
                    //  radius of cross section at distance i from center
                    int r = (int) Math.sqrt(R*R - Math.pow(i*3, 2));
                    OvalRoi roi = new OvalRoi(x-r, y-r, 2*r, 2*r);
                    roi.setPosition(slicePos+i);
                    roi.setFillColor(new Color(255,127,0,102));
                    roi.setStrokeWidth(.0);
                    overlay.add(roi);
                }
            }
            ImageJ ij = new ImageJ();
            original.setOverlay(overlay);
            original.show();

            printStats(cellsData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printStats(ArrayList<int[]> cellsData) {
        double meanDistance = 0;
        double meanRadius = 0;
        int count = 0;

        for (int[] cellData: cellsData) {
            Point3D newCenter = new Point3D(cellData[0], cellData[1], cellData[2]);
            Point3D oldCenter = new Point3D(cellData[4], cellData[5], cellData[6]);
            meanDistance += newCenter.distance(oldCenter, 1, .33);
            meanRadius += cellData[3];
            count++;
        }

        meanDistance /= count;
        meanRadius /= count;

        IJ.log("Mean distance: " + Double.toString(meanDistance));
        IJ.log("Mean radius: " + Double.toString(meanRadius));
    }
}
