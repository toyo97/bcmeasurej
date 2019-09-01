package algorithm;

import mcib3d.geom.Point3D;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageHandler;
import stack.CellStack;

import java.util.ArrayList;
import java.util.ListIterator;

public class MeanShift {

    private CellStack cellStack;
    private int radius;
    private ArrayList<int[]> peaks;
    private float sigma;  // Gaussian kernel parameter
    private float thresh;

    public MeanShift(CellStack cellStack, int radius, ArrayList<int[]> peaks, float sigma, float thresh) {
        this.cellStack = cellStack;
        this.radius = radius;
        this.peaks = peaks;
        this.sigma = sigma;
        this.thresh = thresh;
    }

    private ArrayList<int[]> runMeanShift() {
        ImageHandler imh = ImageHandler.wrap(cellStack);

//        copy peaks list
        ArrayList<int[]> X = new ArrayList<>();
        for (int[] p : peaks) {
            X.add(p.clone());
        }

//        use pastX if halt condition is tolerance instead of iterations
//        ArrayList<int[]> pastX = new ArrayList<>();
        int nIterations = 15;
        for (int it = 0; it < nIterations; it++) {
            ListIterator<int[]> iterator = X.listIterator();
            while (iterator.hasNext()) {
                int i = iterator.nextIndex();
                int[] x = iterator.next();

                Point3D pointX = new Point3D(x[0], x[1], x[2]);

//                for each point x in X, find the neighboring points N(x) of x
                ArrayList<Voxel3D> neighbors = imh.getNeighborhoodLayerList(x[0], x[1], x[2], 0, radius);

//                for each point x in X compute the mean shift m(x)
                float[] numerator = new float[]{0, 0, 0};
                float denominator = 0;

                for (Voxel3D neighbor : neighbors) {
                    if (neighbor.getValue() >= thresh) {
//                        neighbor is a Voxel3D Object, getPosition is a Point3D. For reference,
//                        see https://github.com/mcib3d/mcib3d-core/blob/master/src/main/java/mcib3d/geom/Voxel3D.java
                        double[] neighPos = neighbor.getPosition().getArray();
                        double distance = pointX.distance(neighbor, 1, cellStack.getScaleZ());
                        double kernel = gaussianKernel(distance);

                        for (int j = 0; j < 3; j++) {
                            numerator[j] += neighPos[j] * kernel * neighbor.getValue();
                        }
                        denominator += kernel * neighbor.getValue();
                    }
                }

                for (int j = 0; j < 3; j++) {
                    X.get(i)[j] = (int) (numerator[j] / denominator);
                }
            }
//            for (int[] c : X) {
//                pastX.add(c.clone());
//            }
        }
        return X;
    }

    public int[] getCentroid() {

        Point3D center = new Point3D(cellStack.getCellCenter()[0], cellStack.getCellCenter()[1], cellStack.getCellCenter()[2]);
        ArrayList<int[]> newPeaks = new ArrayList<>();

//        use only peaks close to the cell center
        for (int[] p : peaks) {
            Point3D peak = new Point3D(p[0], p[1], p[2]);
            if (center.distance(peak, 1, cellStack.getScaleZ()) <= radius)
                newPeaks.add(p);
        }
        this.peaks = newPeaks;

//        run main algorithm
        ArrayList<int[]> centroids = runMeanShift();

//        find cell centroid amongst all centroids
        double minDist = Double.MAX_VALUE;
        int minIdx = 0;
        for (int i = 0; i < centroids.size(); i++) {
            Point3D centroidPoint = new Point3D(centroids.get(i)[0], centroids.get(i)[1], centroids.get(i)[2]);
            double dist = center.distance(centroidPoint, 1, cellStack.getScaleZ());
            if (dist < minDist) {
                minDist = dist;
                minIdx = i;
            }
        }

        return centroids.get(minIdx);
    }

    private double gaussianKernel(double distance) {
        return (1 / (this.sigma * Math.sqrt(2 * Math.PI))) * Math.exp(-0.5 * Math.pow(distance / sigma, 2));
    }

}
