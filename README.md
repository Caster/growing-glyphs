This repository contains a work-in-progress implementation of a clustering
algorithm for growing glyphs. Glyphs are simple shapes, such as squares or
circles, that grow at individual speeds. Each glyph starts as a point, and the
algorithm assumes that all points are disjoint. When two glyphs grow into each
other, they are replaced by a single glyph at the weighted midpoint of the two
glyphs that touched, and with the sum of their weights as its weight. The
algorithm produces a tree that describes which glyphs merge at which point in
time.

## Logging/debugging
The algorithm should be run with some VM arguments if you want to see logging:

    -Djava.util.logging.config.file=/path/to/growing-glyphs/logging.properties -Xverify:none

The `logging.properties` file can be used to change how much output is produced
by the algorithm. Detailed debug output is available in a couple of classes
related to executing the algorithm and viewing the resulting clustering tree.

## Running the program
The program will by default display a GUI when run, and open a randomly
generated set of points. All available functionality can be accessed via the
menu bar. To open a specific dataset immediately, pass the absolute path to an
input file as a commandline argument.

    java -jar GrowingGlyphs.jar /path/to/growing-glyphs/input/some-file

### CLI
It is possible to have the program only execute the algorithm and terminate. The
`-d` flag should be passed to achieve so, followed by the input file. The output
of the clustering is currently lost, and this mode is only used for
benchmarking. Near future work is to have the program output the clustering in
some format to a file.

    java -jar GrowingGlyphs.jar -d /path/to/growing-glyphs/input/some-file

Passing any invalid arguments to the program will cause it to print a brief help.
