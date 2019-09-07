package stack;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.MaximaFinder;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import geom.Box3D;
import algorithm.Neighborhood;
import utils.CellPreview;


public class CellStack extends ImagePlus {

    private int dim;
    private int[] seed; // seed is relative to the original image
    private int[] cellCenter; // cellCenter is relative to the stack.CellStack
    private double scaleZ;

    private int radius = 35; //  default value, maximum radius in 70x70x70 box
    private double density;

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

    public int[] getAbsoluteCenter() {
        return new int[]{box.getX0() + cellCenter[0], box.getY0() + cellCenter[1], box.getZ0() + cellCenter[2]};
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

    /**
     * Set pixel depth value according to default value scaleZ
     * NOTE: to be clear, pixelDepth >= 1, scaleZ <= 1 (the latter is the proportion resZ/resXY -res := resolution-)
     */
    public void setCalibration() {
        Calibration cal = this.getCalibration();
        cal.pixelDepth = 1 / this.getScaleZ();
    }

    /**
     * Compute the 3D radial distribution in the given radius
     * Reference https://github.com/mcib3d/mcib3d-core/blob/master/src/main/java/mcib3d/image3d/ImageHandler.java
     *
     * @param maxRad maximum radius computed
     * @return values of (half) the gaussian in list of length maxRad+1
     */
    public double[] computeRadialDistribution3D(int maxRad) {
        double[] tab = new double[maxRad + 1];

        for (int r = 0; r < maxRad + 1; r++) {
            double mean = Neighborhood.getMean(this, r, r + 1);
            tab[r] = mean;
        }

        return tab;
    }

    /**
     * Find the radius of the cell from the 3D radial distribution counting the values above the given threshold
     *
     * @param thresh threshold value to give to computeRadialDistribution 3D
     * @param maxRad maximum radius computed
     * @return radius of the cell
     */
    public int computeCellRadius(double thresh, int maxRad) {
        int r = 0;
        double[] rad3D = computeRadialDistribution3D(maxRad);
        while (rad3D[r] >= thresh && r < rad3D.length - 1)
            r++;

        radius = r;

        return r;
    }

    /**
     * Simply finds the local maximum oround the current cell center
     *
     * @return 3D coordinates of the local max found in the stack
     * @throws Exception if cellCenter coordinates are wrong (it should always be inside the CellStack)
     */
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

    /**
     * Calculate a threshold value computing the center/background mean and weighting the sum
     * Reference at: https://github.com/mcib3d/mcib3d-core/blob/master/src/main/java/mcib3d/image3d/Segment3DSpots.java
     *
     * @param r0     radius inside the center (estimate)
     * @param r1     radius of the beginning of the background spherical cap
     * @param r2     radius of the end of the background spherical cap
     * @param weight weight of the center mean
     * @return weighted mean which represents the threshold
     */
    public double getLocalMean(int r0, int r1, int r2, double weight) {
        double mSpot = Neighborhood.getMean(this, 0, r0);
        double mBack = Neighborhood.getMean(this, r1, r2);

        return mSpot * weight + (1 - weight) * mBack;
    }

    /**
     * Find maxima in the entire stack with given radius. Exclude peaks below thresh
     *
     * @param radius search radius
     * @param thresh intensity threshold
     * @return list of maxima 3D coordinates
     */
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

    /**
     * Generate all the CellStacks in the given image at the given coordinates
     *
     * @param imp    source image
     * @param seeds  list of seeds
     * @param dim    dimension of the resulting CellStack
     * @param scaleZ scale for z axis (1 is isotropic, less otherwise)
     * @return list of CellStacks
     */
    public static ArrayList<CellStack> getCellStacksFromSeeds(ImagePlus imp, ArrayList<int[]> seeds, int dim, double scaleZ) {
        ArrayList<CellStack> cellStacks = new ArrayList<>();
        for (int[] seed : seeds) {
            cellStacks.add(new CellStack(imp, seed, dim, scaleZ));
        }
        return cellStacks;
    }

    /**
     * Compute the density (in intensity) of the cell in the found radius sphere
     * Values below threshold are not considered belonging to the cell, thus not counted
     *
     * @param thresh threshold value
     * @return density
     */
    public double computeDensity(double thresh) {
        //  formula of spheroid volume
        ImageHandler imh = ImageHandler.wrap(this);

        double volume = 4 / 3. * Math.PI * Math.pow(radius, 3) * scaleZ;
        int total = 0;
        int[] values = imh.getNeighborhoodSphere(cellCenter[0], cellCenter[1], cellCenter[2], radius, radius, (float) (radius * scaleZ)).getArrayInt();
        for (int i = 0; i < values.length; i++) {
            if (values[i] >= thresh)
                total += values[i];
        }

        density = total / volume;
        return density;
    }

    /**
     * Handy method to check if the cell is entirely inside the stack volume
     *
     * @return False if the two opposite vertices of the cube circumscribed to the sphere containing the cell
     */
    public boolean isOnBorder() {
        int[] vertex1 = new int[]{cellCenter[0] - radius / 2, cellCenter[1] - radius / 2, cellCenter[2] - (int) (radius * scaleZ / 2)};
        int[] vertex2 = new int[]{cellCenter[0] + radius / 2, cellCenter[1] + radius / 2, cellCenter[2] + (int) (radius * scaleZ / 2)};
        boolean expr1 = contains(vertex1);
        boolean expr2 = contains(vertex2);
        return !(expr1 && expr2);
    }

    //    Below some marginal helper method

    public List<String> getData() {
        List<String> row = new ArrayList<>();
        int[] absoluteCenter = getAbsoluteCenter();
        row.add(Integer.toString(absoluteCenter[0]));
        row.add(Integer.toString(absoluteCenter[1]));
        row.add(Integer.toString(absoluteCenter[2]));
        row.add(Integer.toString(radius));
        return row;
    }

    /**
     * Save the current cell details for being showed in debug mode
     * Draw a colored circle around the cell with the found radius
     *
     * @return CellPreview object
     */
    public CellPreview savePreview() {
        CellPreview cellPreview = new CellPreview(this.getTitle(),
                this.getImageStack().getProcessor(this.cellCenter[2] + 1),
                this.density);
        cellPreview.getProcessor().setColor(Color.GREEN);
        cellPreview.getProcessor().drawOval(cellCenter[0] - radius, cellCenter[1] - radius, radius * 2, radius * 2);
        return cellPreview;
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

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }
}
