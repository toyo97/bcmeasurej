package geom;

public class Box3D {
    private int x0, y0, z0;
    private int width, height, depth;

    public Box3D(int x0, int y0, int z0, int width, int height, int depth) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    /**
     * Constructor to generate a box given its center and cubic dimension plus constraints of the
     * space in which it must fit
     *
     * @param center 3D coordinates of the center around which will be created the box
     * @param dim dimension of the cube
     * @param scaleZ scale for z axis (1 is isotropic, less otherwise)
     * @param maxW width of the image inside which the box must fit
     * @param maxH height of the image inside which the box must fit
     * @param maxD depth of the image inside which the box must fit
     */
    public Box3D(int[] center, int dim, double scaleZ, int maxW, int maxH, int maxD) {
        this.x0 = Math.max(center[0] - dim / 2, 0);
        this.y0 = Math.max(center[1] - dim / 2, 0);
        this.z0 = Math.max(center[2] - dim / 2, 0);

        this.width = Math.min(dim, maxW - x0);
        this.height = Math.min(dim, maxH - y0);
        this.depth = Math.min((int) (dim*scaleZ), maxD - z0);
    }

    public int getX0() {
        return x0;
    }

    public int getY0() {
        return y0;
    }

    public int getZ0() {
        return z0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }
}
