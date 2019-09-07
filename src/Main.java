import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import algorithm.MeanShift;
import stack.CellStack;
import utils.*;


public class Main {

    //  "/home/zemp/IdeaProjects/bcmeasurej/testbatch"
    private static final String SOURCE_DIR = "/home/zemp/bcfind_GT";
    //    TODO add target dir option
//    private static final String TARGET_DIR = "/home/zemp/bcfind_GT";
    private static final int CUBE_DIM = 70;  // dim of cube as region of interest (ROI) around every cell center
    private static final double SCALE_Z = 0.4;  // approx proportion with xy axis, equals to resZ/resXY
    private static final boolean INVERT_Y = true;

    //  localMean params
    private static final int R0 = 13;
    private static final int R1 = 18;
    private static final int R2 = 40;
    private static final double MEAN_WEIGHT = 0.4;  // 0.5 perfect balance, less than 0.5 gives more weight to background values

    //  filter params
    private static final String FILTER = "none";
    private static final float FILTER_SIGMA = 2f;

    //  3d radial distribution params
    private static final int MAX_RADIUS = 40;

    //  MeanShift params
    private static final double MS_SIGMA = 10;

    //  Look-Up-Table (alternatives: fire, default)
    private static final String COLOR_MAP = "fire";

    //  display params
    private static final boolean DISCARD_MARGIN_CELLS = true;
    private static final boolean DEBUG = true;

    private static Progress progress;
    private static ArrayList<CellPreview> cellPreviews = new ArrayList<>();


    public static void main(String[] args) {
        //  open imagej frame
        ImageJ imageJ = new ImageJ();

        try {
            fullProcess();

            if (DEBUG) {
                System.out.println("DEBUG: Loading previews");
                Montage.showRandomMontages(cellPreviews);
            }

            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fullProcess() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(SOURCE_DIR))) {
            List<String> files = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".marker"))
                    .filter(p -> !(p.getFileName().toString().contains("[RAD]")))
                    .map(Path::toString)
                    .map(FilenameUtils::removeExtension)
                    .sorted()
                    .collect(Collectors.toList());

            progress = new Progress(files.size());
            for (String filePath : files) {
                try {
                    progress.stepImg();

                    processImg(filePath);

                } catch (Exception e) {
                    e.printStackTrace();
                    IJ.error(e.getMessage());
                }
            }
        } catch (NoSuchFileException nsfe) {
            nsfe.printStackTrace();
            IJ.error("Source dir '" + nsfe.getMessage() + "' not valid");
        } catch (Exception e) {
            e.printStackTrace();
            IJ.error("Unknown error, please check stack trace");
        }
    }

    private static void processImg(String imgPath) {
        IJ.log("Processing " + imgPath + "...");

        //  open image
        ImagePlus imp = IJ.openImage(imgPath);

        //  read relative csv file rows (coordinates of centers)
        String markerPath = imgPath + ".marker";
        ArrayList<int[]> seeds;
        try {
            if (INVERT_Y)
                seeds = Marker.readMarker(markerPath, imp.getHeight());
            else
                seeds = Marker.readMarker(markerPath);

            ArrayList<List<String>> rows = new ArrayList<>();

            progress.resetCellCount(seeds.size(), Paths.get(imgPath).getFileName().toString());
            for (CellStack cellStack : CellStack.getCellStacksFromSeeds(imp, seeds, CUBE_DIM, SCALE_Z)) {
                progress.stepCell();
                progress.show();

                if (cellStack.isOnBorder() && DISCARD_MARGIN_CELLS) {
                    IJ.log("Skipped on border cell " + Arrays.toString(cellStack.getCellCenter()));
                } else {
                    try {
                        processCell(cellStack);

                        rows.add(cellStack.getData());

                        //  apply a different LUT for display
                        if (!COLOR_MAP.equals("default"))
                            Display.applyLUT(cellStack, COLOR_MAP);

                        if (DEBUG) {
                            cellPreviews.add(cellStack.savePreview());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        IJ.error("Skipped cell " + Arrays.toString(cellStack.getCellCenter()) + ", reason: " + e.getMessage());
                    }
                }
            }

            String outMarkerPath = imgPath + "[RAD].marker";
            Marker.writeMarker(outMarkerPath, rows);

        } catch (IOException e) {
            e.printStackTrace();
            IJ.error("Error with marker " + markerPath + "\nSkipped");
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            IJ.error("Invalid img path: " + imgPath);
        }
    }

    private static void processCell(CellStack cellStack) throws Exception {
        IJ.log("Cell at " + Arrays.toString(cellStack.getCellCenter()));
        cellStack.setCalibration();

        if (!FILTER.equals("none")) {
            IJ.log("- Applying " + FILTER + " 3D filtering");
            Filter.filterCellStack(cellStack, FILTER, FILTER_SIGMA);
        }

        IJ.log("- Computing first radius approximation using local max");
        int[] localMax = cellStack.getLocalMaxPos();
        IJ.log("- Local max in " + Arrays.toString(localMax) + ", " +
                "value: " + cellStack.getVoxel(localMax));

        double localMean = cellStack.getLocalMean(R0, R1, R2, MEAN_WEIGHT);
        IJ.log("- Local mean: " + localMean);

        int radius = cellStack.computeCellRadius(localMean, MAX_RADIUS);
        IJ.log("- First radius: " + radius);

        ArrayList<int[]> peaks = cellStack.findMaxima(radius / 2, (float) localMean);

        IJ.log("- Applying mean shift with peaks found...");
        MeanShift ms = new MeanShift(cellStack, radius, peaks, MS_SIGMA, localMean);
        int[] centroid = ms.getCentroid();

        cellStack.setCellCenter(centroid);
        IJ.log("- New center: " + Arrays.toString(centroid));

        double newLocalMean = cellStack.getLocalMean(radius - 3, radius + 3, radius + 23, MEAN_WEIGHT);

        int newRadius = cellStack.computeCellRadius(newLocalMean, MAX_RADIUS);
        IJ.log("- New radius: " + newRadius);
        cellStack.setRadius(newRadius);
        cellStack.computeDensity(newLocalMean);
    }

}
