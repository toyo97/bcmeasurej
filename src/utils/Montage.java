package utils;

import ij.ImagePlus;
import ij.process.ColorProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Montage extends ImagePlus {

    /**
     * Montage of nxn square identical images
     * @param imgs list of images
     */
    public Montage(ArrayList<CellPreview> imgs) {
        super();
        int dim = imgs.get(0).getWidth();
        int n = (int) Math.ceil(Math.sqrt(imgs.size()) );
        this.setProcessor(new ColorProcessor(dim*n, dim*n));
        for (int i = 0; i < imgs.size(); i++) {
            this.getProcessor().insert(imgs.get(i).getProcessor(), dim * (i % n), dim * (i / n));
        }
    }

    public static void showRandomMontages(ArrayList<CellPreview> total) {
        total.sort(Comparator.comparing(c -> c.density));
    }
}
