package algorithm;

import ij.IJ;
import java.util.ArrayList;

import stack.CellStack;


public class Neighborhood {

    public static ArrayList<int[]> getNeighborhood3x3x3(int[] center) {
        ArrayList<int[]> neigh = new ArrayList<>();

        for (int dz = -1; dz < 2; dz++) {
            for (int dy = -1; dy < 2; dy++) {
                for (int dx = -1; dx < 2; dx++) {
                    if (!(dx == 0 && dy == 0 && dz == 0))
                        neigh.add(new int[] {center[0]+dx, center[1]+dy, center[2]+dz});
                }
            }
        }
        return neigh;
    }

    public static float getMean(CellStack cs, int r1, int r0) {
        int index = 0;
        int total = 0;
        double r02 = r0 * r0;
        double r12 = r1 * r1;

        int x = cs.getCellCenter()[0];
        int y = cs.getCellCenter()[1];
        int z = cs.getCellCenter()[2];

        double dist;
        double ratio = 1 / cs.getScaleZ();
        double ratio2 = ratio * ratio;
        int vx = (int) Math.ceil(r1);
        int vy = (int) Math.ceil(r1);
        int vz = (int) (Math.ceil(r1 / ratio));

        for (int k = z - vz; k <= z + vz; k++) {
            for (int j = y - vy; j <= y + vy; j++) {
                for (int i = x - vx; i <= x + vx; i++) {
                    int[] current = new int[]{i, j, k};
                    if (cs.contains(current)) {
                        dist = ((x - i) * (x - i)) + ((y - j) * (y - j)) + ((z - k) * (z - k) * ratio2);
                        if ((dist >= r02) && (dist < r12)) {
                            try {
                                total += cs.getVoxel(current);
                            } catch (Exception e) {
                                IJ.log(e.getMessage());
                            }
                            index++;
                        }
                    }
                }
            }
        }
        // check if some values are set
        if (index > 0) {
            return (float) total / index;
        } else {
            return 0;
        }
    }
}
