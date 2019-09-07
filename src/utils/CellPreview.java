package utils;

import ij.ImagePlus;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * ImagePlus extension which contains the density value of the cell and only the slice corresponding to the center
 */
public class CellPreview extends ImagePlus {
    public double density;

    public CellPreview(String title, ImageProcessor ip, double density) {
        super(title, ip);
        this.density = density;
        ImageConverter c = new ImageConverter(this);
        c.convertToRGB();
    }
}
