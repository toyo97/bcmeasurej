package utils;

import ij.IJ;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.LutLoader;
import ij.process.LUT;
import ij.process.StackStatistics;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.IOException;

import stack.CellStack;


public class Display {

    /**
     * Apply a different Look Up Table according to the given colorMap name
     *
     * @param cellStack cell stack being processed
     * @param colorMap  name of the color map (can be 'fire')
     */
    public static void applyLUT(CellStack cellStack, String colorMap) {
        StackStatistics stats = new StackStatistics(cellStack);

        if (colorMap.equals("fire")) {
            try {
                IndexColorModel colorModel = LutLoader.open("luts/fire.lut");

                LUT lut = new LUT(colorModel, stats.min, stats.max);
                cellStack.setLut(lut);
            } catch (IOException e) {
                IJ.error("Cannot open LUT file\nDefault LUT applied");
            }
        } else
            IJ.error("Invalid color map " + colorMap + "\nDefault LUT applied");
    }

    public static void circleRoi(CellStack cellStack, int r, Color color) {
        int d = r * 2;
        int x = cellStack.getCellCenter()[0] - r;
        int y = cellStack.getCellCenter()[1] - r;
        Roi roi = new OvalRoi(x, y, d, d);
        roi.setStrokeColor(color);
        cellStack.setRoi(roi);
    }
}
