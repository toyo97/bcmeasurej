package stack;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.MaximaFinder;
import java.util.ArrayList;
import java.util.Arrays;

import geom.Box3D;
import algorithm.Neighborhood;


public class CellStack extends ImagePlus {

    private int dim;
    private int[] seed; // seed is relative to the original image
    private int[] cellCenter; // cellCenter is relative to the stack.CellStack
    private double scaleZ;

    //  cube position in original image
    private Box3D box;

    /**
     * Constructor which extract the 3D box containing the cell pointed by seed coords in the given image
     *
     * @param imp    original image containing the cell
     * @param seed   approx. cell position
     * @param dim    dimension of the containing cube
     * @param scaleZ scale for z axis (1 is isotropic, less otherwise)
     */
    public CellStack(ImagePlus imp, int[] seed, int dim, double scaleZ) {
        super();
        this.dim = dim;
        this.seed = seed;
        this.scaleZ = scaleZ;

        int[] dimensions = imp.getDimensions();
        this.box = new Box3D(seed, dim, scaleZ, dimensions[0], dimensions[1], dimensions[3]);

        this.cellCenter = getRelativeCenter(seed, this.box);

        String title = Arrays.toString(this.seed) + " in " + imp.getTitle();
        this.setTitle(title);
        ImageStack stack = imp.getImageStack().crop(this.box.getX0(), this.box.getY0(), this.box.getZ0(),
                this.box.getWidth(), this.box.getHeight(), this.box.getDepth());
        this.setStack(stack);
    }

    /**
     * Overloads the main constructor for unspecified z scale. Isotropic image is assumed
     */
    public CellStack(ImagePlus imp, int[] seed, int dim) {
        this(imp, seed, dim, 1.0);
    }

    /**
     * Calculate the relative coordinate inside the cell stack given absolute coords and box position
     * (Simple 3D frame of reference change)
     *
     * @param absCenter absolute center, which means its coordinates are relative to the original image
     * @param box       position of the cell stack relatively to the original image
     * @return coordinates of the center inside the stack.CellStack
     */
    private int[] getRelativeCenter(int[] absCenter, Box3D box) {
        return new int[]{absCenter[0] - box.getX0(), absCenter[1] - box.getY0(), absCenter[2] - box.getZ0()};
    }

    /**
     * Method to check if a point is inside the stack.CellStack
     *
     * @param pos 3D coordinates to be checked
     * @return true if coordinates are inside
     */
    public boolean contains(int[] pos) {
        return pos[0] >= 0 && pos[1] >= 0 && pos[2] >= 0 &&
                pos[0] < this.box.getWidth() && pos[1] < this.box.getHeight() && pos[2] < this.box.getDepth();
    }

    /**
     * Random access to the voxel intensity
     *
     * @param pos 3D coordinates
     * @return voxel intensity as int
     * @throws Exception if 3D coordinates are outside the stack.CellStack frame
     */
    public int getVoxel(int[] pos) throws Exception {
        if (this.contains(pos)) {
            this.setPosition(pos[2] + 1);
            return this.getPixel(pos[0], pos[1])[0];
        } else throw new Exception("Position " + Arrays.toString(pos) + " is outside cell stack");
    }

    public void setCalibration() {
        Calibration cal = this.getCalibration();
        cal.pixelDepth = 1 / this.getScaleZ();
    }

    public double[] computeRadialDistribution3D(int maxRad) {
        double[] tab = new double[maxRad + 1];

        for (int r = 0; r < maxRad + 1; r++) {
            double mean = Neighborhood.getMean(this, r + 1, r);
            tab[r] = mean;
        }

        return tab;
    }

    public int computeCellRadius(double thresh, int maxRad) {
        int r = 0;
        double[] rad3D = computeRadialDistribution3D(maxRad);
        while (rad3D[r] >= thresh && r < rad3D.length - 1)
            r++;

        return r;
    }

    public int[] getLocalMaxPos() throws Exception {

        int[] maxPos = cellCenter;
        int maxValue = getVoxel(maxPos);

        boolean newMaxFound = true;

        while (newMaxFound) {
            newMaxFound = false;
            for (int[] pos : Neighborhood.getNeighborhood3x3x3(maxPos)) {
                try {
                    int v = getVoxel(pos);

                    if (v > maxValue) {
                        maxPos = pos;
                        maxValue = v;
                        newMaxFound = true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return maxPos;
    }

    public double getLocalMean(int r0, int r1, int r2, double weight) {
        double mSpot = Neighborhood.getMean(this, r0, 0);
        double mBack = Neighborhood.getMean(this, r2, r1);

        return mSpot * weight + (1 - weight) * mBack;
    }

    public ArrayList<int[]> findMaxima(int radius, float thresh) {
        ImageHandler imh = ImageHandler.wrap(this.duplicate());
        int radZ = (int) (radius * getScaleZ());

//        in MaximaFinder thresh is the noise tolerance value
        MaximaFinder mf = new MaximaFinder(imh, radius, radZ, thresh);
        ArrayList<Voxel3D> peaksVox = mf.getListPeaks();

        ArrayList<int[]> peaks = new ArrayList<>();
        for (Voxel3D peak : peaksVox) {
            if (peak.getValue() >= thresh) {
                double[] doubles = peak.getPosition().getArray();
                int[] ints = new int[3];
                for (int i = 0; i < 3; i++) {
                    ints[i] = (int) doubles[i];
                }
                peaks.add(ints);
            }
        }
        peaks.add(cellCenter);
        return peaks;
    }

    public static ArrayList<CellStack> getCellStacksFromSeeds(ImagePlus imp, ArrayList<int[]> seeds, int dim, double scaleZ) {
        ArrayList<CellStack> cellStacks = new ArrayList<>();
        for (int[] seed : seeds) {
            cellStacks.add(new CellStack(imp, seed, dim, scaleZ));
        }
        return cellStacks;
    }

    public boolean isOnBorder() {
        return dim != box.getWidth() || dim != box.getHeight() || dim * scaleZ != box.getDepth();
    }

    public int getDim() {
        return dim;
    }

    public int[] getSeed() {
        return seed;
    }

    public int[] getCellCenter() {
        return cellCenter;
    }

    public double getScaleZ() {
        return scaleZ;
    }

    public Box3D getBox() {
        return box;
    }

    public void setCellCenter(int[] cellCenter) {
        this.cellCenter = cellCenter;
    }
}
