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
import ij.process.ImageProcessor;
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

                for (int i = -scaledR -1; i < scaledR + 1; i++) {
                    //  radius of cross section at distance i from center
                    int r = (int) Math.sqrt(R*R - Math.pow(i*3, 2));
                    OvalRoi roi = new OvalRoi(x-r, y-r, 2*r, 2*r);
                    roi.setPosition(slicePos+i);
                    overlay.add(roi);
                }
            }
            ImageJ ij = new ImageJ();
            overlay.setFillColor(new Color(255,127,0,102));
            overlay.setStrokeWidth(.0);
            original.setOverlay(overlay);
            original.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
