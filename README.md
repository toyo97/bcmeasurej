# Brain Cell Measure
_A tool for measuring of radius of brain cells in 3D images_

![3D image animation](https://i.imgur.com/bkA2sbJ.gif)
![radius](https://i.imgur.com/leBe7eG.png)

1. [Description](#description)
2. [Requirements](#requirements)
3. [Usage](#usage)
    1. [Options](#options)
4. [Sources](#sources)

## Description
Brain Cell Measure is a tool for automated determination of radius of cells (at given coordinates) in 3D mouse brain images acquired by 
confocal light sheet microscopy.
It processes every cell listed in the marker files firstly applying adaptive thresholding to find an estimate of the radius 
around the seed (which may be not properly centered) and then using this estimate to apply [mean shift](https://en.wikipedia.org/wiki/Mean_shift)
algorithm to obtain a more accurate centroid. Finally another threshold is re-evaluated and applied again to the 3D radial distribution (this time
around the centroid) for the final radius measurement. For more details please see the [full report](#)
_(coming soon)_

After launching the script, it will search for every marker file in the source folder (walking in every subfolder) and then
sequentially process every pointed cell on the relative image stack. Then it will create another marker file (CSV) with the updated coordinates
of the corrected centroid of the cells and their radius measure and save it in the same folder of the source files.

The script is written in Java just like the ImageJ library upon which the tool is based.
It uses therefore [ImageJ API](https://imagej.nih.gov/ij/developer/api/overview-summary.html) and also other libraries included in the lib folder,
such as [3D ImageJ Suite core library](https://imagejdocu.tudor.lu/doku.php?id=plugin:stacks:3d_ij_suite:start).

## Requirements
The tool requires a path to a directory in which are stored .tif images and the relative marker file with the seed 3D coordinates 
(which must have the same name).
_Example:_
```
Marker file:  SST_11_2.tif.marker
Image file:   SST_11_2.tif
```
Marker files should have the first line as the header and the x,y,z coordinates as the first 3 columns. Blank lines are not skipped and will raise an error so please check that the files follow this convention
Decimal coordinates are read correctly but converted to integer values.
The marker file can have additional columns (which are not considered) but only after x,y,z.
The separator is a simple comma without space ','.
_e.g._
```
#x, y, z, comment
20.2,12.1,7.0,
43.8,123.4,45.0,
59.7,51.2,100.7,
...
```

## Usage
After cloning the source with
```bash
$ git clone https://github.com/toyo97/bcmeasurej
```
compile the source code including also the jar files in the lib folder
```bash
$ cd /bcmeasurej/src/
$ javac -cp ../lib/*:. bcmeasure.java
```
then run the script specifying the source directory with the option ```-sd```
```bash
$ java -cp ../lib/*:. bcmeasure -sd /home/user/path/to/source/files
```
### Options
You can also set some parameters with the following options:
```
usage: bcmeasure [OPTIONS]
 -d,--debug                                 Enable debug mode
 
 -dim,--cube-dim <int>                      Dimension of the cube
          default: 70                       containing the cell for local
                                            operations
                                            
 -ec,--edge-cells                           Include cells on edges
 
 -f,--filter <filter-name>                  Apply a filter before
          default: none                     processing. Possible values
                                            are: gauss, mean, median, none
                                            
 -fire,--fire-color-map                     Apply different color map
                                            (LUT) than default
                                            
 -maxr,--max-radius <int>                   Maximum radius of the cells
          default: 40
          
 -mc,--matrix-coord                         Specifies that the markers
                                            follow the matrix coordinate
                                            system instead of the graphic
                                            c.s.
                                            
 -mw,--local-mean-weight <float in (0,1)>   Give more weight to background
          default: 0.4                      (<0.5) or to the cell (>0.5)
          
 -sd,--source-dir <path>                    Absolute path of the source
          required!                         directory (for both images and
                                            csv files)
                                            
 -z,--scale-z <float>                       Scale of the z axis. 1 if
          default: 0.4                      isotropic, less otherwise
                                            (resZ/resXY)
```

_Examples:_
```bash
$ java -cp ../lib/*:. bcmeasure -sd /home/user/path/to/source/files -d
```
This command will start the script in debug mode, printing log messages in the dedicated ImageJ frame and showing a preview
of a maximum of 300 random cells after the whole processing. 
```bash
$ java -cp ../lib/*:. bcmeasure -sd /home/user/path/to/source/files -ec -f gauss
```
With this command the tool will include cells that are on the edges (XYZ) of the stack. The resulting radius might be less precise.
Then a 3D gaussian blur filter will be applied before radius determination process.

## Sources
Documentation for the code was mainly found here:

- [ImageJ API](https://imagej.nih.gov/ij/developer/api/)
- [mcib3d-core library](https://github.com/mcib3d/mcib3d-core) (Thomas Boudier, Jean Ollion)
- [bcfind](https://github.com/paolo-f/bcfind)
