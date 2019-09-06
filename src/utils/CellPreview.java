package utils;

import ij.ImagePlus;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

public class CellPreview extends ImagePlus {
    public double density;

    public CellPreview(String title, ImageProcessor ip, double density) {
        super(title, ip);
        this.density = density;
        ImageConverter c = new ImageConverter(this);
        c.convertToRGB();
    }
}
