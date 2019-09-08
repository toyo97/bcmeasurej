package utils;

import org.apache.commons.cli.*;

import java.io.File;

public class Params {

    //  "/home/zemp/IdeaProjects/bcmeasurej/testbatch"
    public static String SOURCE_DIR = "";
    //    private static final String TARGET_DIR = "/home/zemp/bcfind_GT";
    public static int CUBE_DIM = 70;  // dim of cube as region of interest (ROI) around every cell center
    public static double SCALE_Z = 0.4;  // approx proportion with xy axis, equals to resZ/resXY
    public static boolean INVERT_Y = true;  // if the markers are in graphics coordinate system must be set to true

    //  localMean params
    public static final int R0 = 13;
    public static final int R1 = 18;
    public static final int R2 = 40;
    public static double MEAN_WEIGHT = 0.4;  // 0.5 perfect balance, less than 0.5 gives more weight to background values

    //  filter params
    public static String FILTER = "none";
    public static final float FILTER_SIGMA = 2f;

    //  3d radial distribution params
    public static int MAX_RADIUS = 40;

    //  MeanShift params
    public static final double MS_SIGMA = 10;

    //  Look-Up-Table (alternatives: fire, default)
    public static String COLOR_MAP = "default";

    //  display params
    public static boolean DISCARD_EDGE_CELLS = true;
    public static boolean DEBUG = false;
    public static Options options= new Options();

    public static void parse(String[] args) throws ParseException {

        options.addOption(new Option("d", "debug", false, "Enable debug mode"));
        options.addOption(new Option("ec", "edge-cells", false, "Include cells on edges"));
        options.addOption(new Option("mc", "matrix-coord", false,
                "Apecifies that the markers follow the matrix coordinate system instead of the graphic c.s."));
        options.addOption(new Option("fire", "fire-color-map", false,
                "Apply different color map (LUT) than default"));

        Option filter = Option.builder("f")
                .longOpt("filter")
                .hasArg()
                .argName("filter-name")
                .desc("Apply a filter before processing. Possible values are: gauss, mean, median, none")
                .build();

        Option dim = Option.builder("dim")
                .longOpt("cube-dim")
                .hasArg()
                .argName("int")
                .desc("Dimension of the cube containing the cell for local operations")
                .build();

        Option scaleZ = Option.builder("z")
                .longOpt("scale-z")
                .hasArg()
                .argName("float")
                .desc("Scale of the z axis. 1 if isotropic, less otherwise (resZ/resXY)")
                .build();

        Option meanWeight = Option.builder("mw")
                .longOpt("local-mean-weight")
                .hasArg()
                .argName("int in (0,1)")
                .desc("Give more weight to background (<0.5) or to the cell (>0.5)")
                .build();

        Option maxRadius = Option.builder("maxr")
                .longOpt("max-radius")
                .hasArg()
                .argName("int")
                .desc("Maximum radius of the cells")
                .build();

        Option sourceDir = Option.builder("sd")
                .longOpt("source-dir")
                .hasArg()
                .argName("path")
                .desc("Absolute path of the source directory (for both images and csv files)")
                .required()
                .build();

        options.addOption(filter)
                .addOption(dim)
                .addOption(scaleZ)
                .addOption(meanWeight)
                .addOption(maxRadius)
                .addOption(sourceDir);

        CommandLineParser parser = new DefaultParser();

        // parse the command line arguments
        CommandLine line = parser.parse( options, args );

        if (line.hasOption("d"))
            DEBUG = true;
        if (line.hasOption("ec"))
            DISCARD_EDGE_CELLS = false;
        if (line.hasOption("matrix-coord"))
            INVERT_Y = false;
        if (line.hasOption("fire"))
            COLOR_MAP = "fire";
        if (line.hasOption("f")) {
            String choice = line.getOptionValue("f");
            if (choice.equals("mean") || choice.equals("gauss") || choice.equals("median") || choice.equals("none"))
                FILTER = line.getOptionValue("f");
            else
                throw new ParseException("Filter " + choice + " is not valid");
        }
        if (line.hasOption("dim"))
            CUBE_DIM = Integer.parseInt(line.getOptionValue("dim"));
        if (line.hasOption("z"))
            SCALE_Z = Double.parseDouble(line.getOptionValue("z"));
        if (line.hasOption("mw"))
            MEAN_WEIGHT = Double.parseDouble(line.getOptionValue("mw"));
        if (line.hasOption("maxr"))
            MAX_RADIUS = Integer.parseInt(line.getOptionValue("maxr"));

        SOURCE_DIR = line.getOptionValue("sd");
        File source = new File(SOURCE_DIR);
        if (!source.isDirectory()) {
            throw new ParseException("Source dir " + source.toString() + " is not valid");
        }
    }
}
