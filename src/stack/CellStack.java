package stack;

import ij.ImagePlus;
import geom.Box3D;
import ij.ImageStack;

import java.util.Arrays;

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
