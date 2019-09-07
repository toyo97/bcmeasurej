package utils;

import ij.ImagePlus;
import ij.process.ColorProcessor;

import java.util.*;

public class Montage extends ImagePlus {

    /**
     * Montage of nxn square identical images
     *
     * @param imgs list of images
     */
    public Montage(List<CellPreview> imgs) {
        super();
        int dim = imgs.get(0).getWidth();
        int n = (int) Math.ceil(Math.sqrt(imgs.size()));
        this.setProcessor(new ColorProcessor(dim * n, dim * n));
        for (int i = 0; i < imgs.size(); i++) {
            this.getProcessor().insert(imgs.get(i).getProcessor(), dim * (i % n), dim * (i / n));
        }
    }

    public static void showRandomMontages(ArrayList<CellPreview> total) {
        ArrayList<ArrayList<CellPreview>> l = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            l.add(new ArrayList<>());
        }
        Collections.shuffle(total);
        for (CellPreview cp : total) {
            if (cp.density <= 1000) {
                l.get(0).add(cp);
            } else if (cp.density > 1000 && cp.density <= 3000) {
                l.get(1).add(cp);
            } else {
                l.get(2).add(cp);
            }
        }

        for (int i = 0; i < l.size(); i++) {
            Montage m = new Montage(l.get(i).subList(0, Math.min(100, l.get(i).size())));
            String title;
            if (i == 0) {
                title = "Low density";
            } else if (i == 1) {
                title = "Mid density";
            } else {
                title = "High density";
            }
            m.setTitle(title);
            m.show();
        }

    }
}
