/*
 *     Copyright (C) 2019  Vittorio Zampinetti
 *                         zampinetti@gmail.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

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

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import algorithm.MeanShift;
import stack.CellStack;
import utils.*;


public class bcmeasure {

    private static Progress progress;
    private static ArrayList<CellPreview> cellPreviews = new ArrayList<>();
    private static Logger logger;


    public static void main(String[] args) {
        try {
            Params.parse(args);

            logger = new Logger(Params.DEBUG);

            //  open imagej frame if debug mode on
            ImageJ imageJ;
            if (Params.DEBUG)
                imageJ = new ImageJ();

            fullProcess();

            if (Params.DEBUG) {
                System.out.println("DEBUG: Loading previews");
                Montage.showRandomMontages(cellPreviews);
            } else
                logger.writeLogFile(Params.SOURCE_DIR);

            System.out.println("\nDone");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException exp) {
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            new HelpFormatter().printHelp("bcmeasure [OPTIONS]", Params.options);
        }
    }

    private static void fullProcess() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(Params.SOURCE_DIR))) {
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
        logger.log("Processing " + imgPath + "...");

        //  open image
        ImagePlus imp = IJ.openImage(imgPath);

        //  read relative csv file rows (coordinates of centers)
        String markerPath = imgPath + ".marker";
        ArrayList<int[]> seeds;
        try {
            if (Params.INVERT_Y)
                seeds = Marker.readMarker(markerPath, imp.getHeight());
            else
                seeds = Marker.readMarker(markerPath);

            ArrayList<List<String>> rows = new ArrayList<>();

            progress.resetCellCount(seeds.size(), Paths.get(imgPath).getFileName().toString());
            for (CellStack cellStack : CellStack.getCellStacksFromSeeds(imp, seeds, Params.CUBE_DIM, Params.SCALE_Z)) {
                progress.stepCell();
                progress.show();

                if (cellStack.isOnBorder() && Params.DISCARD_EDGE_CELLS) {
                    logger.log("Skipped on border cell " + Arrays.toString(cellStack.getCellCenter()));
                } else {
                    try {
                        processCell(cellStack);

                        rows.add(cellStack.getData());

                        //  apply a different LUT for display
                        if (!Params.COLOR_MAP.equals("default"))
                            if (!Display.applyLUT(cellStack, Params.COLOR_MAP))
                                Params.COLOR_MAP = "default";
                        if (Params.DEBUG) {
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
        logger.log("Cell at " + Arrays.toString(cellStack.getCellCenter()));
        cellStack.setCalibration();

        if (!Params.FILTER.equals("none")) {
            logger.log("- Applying " + Params.FILTER + " 3D filtering");
            Filter.filterCellStack(cellStack, Params.FILTER, Params.FILTER_SIGMA);
        }

        logger.log("- Computing first radius approximation using local max");
        int[] localMax = cellStack.getLocalMaxPos();
        logger.log("- Local max in " + Arrays.toString(localMax) + ", " +
                "value: " + cellStack.getVoxel(localMax));

        double localMean = cellStack.getLocalMean(Params.R0, Params.R1, Params.R2, Params.MEAN_WEIGHT);
        logger.log("- Local mean: " + localMean);

        int radius = cellStack.computeCellRadius(localMean, Params.MAX_RADIUS);
        logger.log("- First radius: " + radius);

        ArrayList<int[]> peaks = cellStack.findMaxima(radius / 2, (float) localMean);

        logger.log("- Applying mean shift with peaks found...");
        MeanShift ms = new MeanShift(cellStack, radius, peaks, Params.MS_SIGMA, localMean);
        int[] centroid = ms.getCentroid();

        cellStack.setCellCenter(centroid);
        logger.log("- New center: " + Arrays.toString(centroid));

        double newLocalMean = cellStack.getLocalMean(radius - 3, radius + 3, radius + 23, Params.MEAN_WEIGHT);

        int newRadius = cellStack.computeCellRadius(newLocalMean, Params.MAX_RADIUS);
        logger.log("- New radius: " + newRadius);
        cellStack.setRadius(newRadius);
        cellStack.computeDensity(newLocalMean);
    }

}
