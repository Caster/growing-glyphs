package ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import algorithm.clustering.NaiveClusterer;
import algorithm.clustering.QuadTreeClusterer;
import datastructure.growfunction.GrowFunction;
import datastructure.growfunction.shape.CirclesGrowShape;
import datastructure.growfunction.shape.GrowShape;
import datastructure.growfunction.shape.SquaresGrowShape;
import datastructure.growfunction.speed.GrowSpeed;
import datastructure.growfunction.speed.LinearAreaGrowSpeed;
import datastructure.growfunction.speed.LinearGrowSpeed;
import datastructure.growfunction.speed.LogarithmicGrowSpeed;
import logging.ConfigurableConsoleHandler;
import utils.Constants.B;

public class CLI {

    /**
     * @param args [input file, output file, algorithm (naive/quad/plus/big),
     *            grow function, collect stats (y/n)]
     */
    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("usage: CLI [input] [output] [algorithm] "
                    + "[grow function] [stats]");
            return;
        }


        String pInput = args[0];
        String pOutput = args[1];
        String pAlgorithm = args[2];
        String pGrowFunction = args[3];
        String pStats = args[4];


        // collect stats?
        B.STATS_ENABLED.set(pStats.equals("y"));
        if (!B.STATS_ENABLED.get() && !pStats.equals("n")) {
            System.err.println("Stats argument must be either 'y' or 'n'.");
            return;
        }

        // set up output
        try {
            File output = new File(pOutput);
            ConfigurableConsoleHandler.redirectTo(new PrintStream(output));
        } catch (FileNotFoundException e) {
            System.err.println("Cannot open output file for writing.");
            return;
        }

        // set up a daemon
        GrowingGlyphsDaemon daemon = new GrowingGlyphsDaemon(512, 512, null);

        // select correct algorithm
        switch (pAlgorithm) {
        case "naive":
            daemon.setClusterer(new NaiveClusterer(daemon.getTree()));
            break;
        case "quad":
        case "plus":
        case "big":
            daemon.setClusterer(new QuadTreeClusterer(daemon.getTree()));
            break;
        default:
            System.err.println("Algorithm not recognized.");
            return;
        }
        // with the correct parameters
        B.BIG_GLYPHS.set(false);
        B.ROBUST.set(false);
        if (pAlgorithm.equals("quad")) {
            B.ROBUST.set(true);
        } else if (pAlgorithm.equals("big")) {
            B.BIG_GLYPHS.set(true);
        }

        // set correct grow function
        String[] pGrowFunctionSplit = pGrowFunction.split("-");
        if (pGrowFunctionSplit.length != 2) {
            System.err.println("Grow function must be passed as [speed]-[shape].");
            return;
        }
        Class<? extends GrowShape> shape;
        Class<? extends GrowSpeed> speed;
        switch (pGrowFunctionSplit[1]) {
        case "circles": shape = CirclesGrowShape.class; break;
        case "squares": shape = SquaresGrowShape.class; break;
        default:
            System.err.println("Grow function shape must be 'circles' or 'squares'.");
            return;
        }
        switch (pGrowFunctionSplit[0]) {
        case "linear": speed = LinearGrowSpeed.class; break;
        case "lineararea": speed = LinearAreaGrowSpeed.class; break;
        case "logarithmic": speed = LogarithmicGrowSpeed.class; break;
        default:
            System.err.println("Grow function speed must be 'linear' or 'lineararea' or 'logarithmic'.");
            return;
        }
        daemon.setGrowFunction(new GrowFunction(shape, speed));

        // open right input
        daemon.openFile(new File(pInput));

        // cluster!
        try {
            daemon.cluster();
        } catch (InterruptedException e) {
            System.err.println("Timed out.");
        } finally {
            ConfigurableConsoleHandler.undoRedirect();
        }
    }

}
