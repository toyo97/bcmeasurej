package utils;

import ij.IJ;
import ij.ImageStack;
import ij.plugin.Filters3D;
import ij.plugin.GaussianBlur3D;
import stack.CellStack;

/**
 * Main filters which can be applied to the cell before the main process
 */
public class Filter {

    private static void gaussianIJ(CellStack cellStack, float sigma) {
        double sigmaZ = cellStack.getScaleZ() * sigma;
        GaussianBlur3D.blur(cellStack, sigma, sigma, sigmaZ);
    }

    private static void medianIJ(CellStack cellStack, float sigma) {
        ImageStack stack = cellStack.getImageStack();
        float sigmaZ = (float) cellStack.getScaleZ() * sigma;
        ImageStack newStack = Filters3D.filter(stack, Filters3D.MEDIAN, sigma, sigma, sigmaZ);

        cellStack.setStack(newStack);
    }

    private static void meanIJ(CellStack cellStack, float sigma) {
        ImageStack stack = cellStack.getImageStack();
        float sigmaZ = (float) cellStack.getScaleZ() * sigma;
        ImageStack newStack = Filters3D.filter(stack, Filters3D.MEAN, sigma, sigma, sigmaZ);

        cellStack.setStack(newStack);
    }

    public static void filterCellStack(CellStack cellStack, String method, float sigma) {
        switch (method) {
            case "gauss":
                gaussianIJ(cellStack, sigma);
                break;
            case "mean":
                meanIJ(cellStack, sigma);
                break;
            case "median":
                medianIJ(cellStack, sigma);
                break;
        }
    }
}
