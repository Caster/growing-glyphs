package ui;

import static gui.GrowingGlyphs.SETTINGS;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import datastructure.growfunction.GrowFunction;
import datastructure.growfunction.shape.CirclesGrowShape;
import datastructure.growfunction.shape.GrowShape;
import datastructure.growfunction.shape.SquaresGrowShape;
import datastructure.growfunction.speed.GrowSpeed;
import datastructure.growfunction.speed.LinearAreaGrowSpeed;
import datastructure.growfunction.speed.LinearGrowSpeed;
import datastructure.growfunction.speed.LogarithmicGrowSpeed;
import gui.Settings.Setting;
import logging.ConfigurableConsoleHandler;
import utils.Utils;

/**
 * Executes experiments and stores the results in a directory.
 */
public class Batch {

    /**
     * Executes experiments.
     *
     * @param args First argument needs to be the path to the growing-glyphs
     *            repository. This is used to find input and write output.
     * @throws IOException If a file could not be created.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("need at least one argument");
        }

        File home = new File(args[0]);
        if (!home.exists() || !home.isDirectory()) {
            throw new IllegalArgumentException("argument must be directory path");
        }

        new Batch(home).run();
    }


    private final List<String> algorithms;
    private final GrowingGlyphsDaemon daemon;
    private final List<GrowFunction> growFunctions;
    private final File home;
    private final List<String> inputs;
    private final File outputDir;


    public Batch(File home) {
        this.algorithms = Arrays.asList(
                "naive"
            );
        this.daemon = new GrowingGlyphsDaemon(512, 512, null);
        this.growFunctions = new ArrayList<>(6);
        this.home = home;
        this.inputs = Arrays.asList(
                "csv/glottovis.tsv",
                "points/trove"
            );
        this.outputDir = new File(home, "output/" + System.currentTimeMillis());

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new RuntimeException("could not create output directory");
            }
        } else if (!outputDir.isDirectory()) {
            throw new RuntimeException("output directory exists, but is not a directory");
        }

        initGrowFunctions();
    }

    public void run() throws IOException {
        try (PrintStream dummy = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException { /* ignored */ }})) {
            int total = inputs.size() * growFunctions.size() * algorithms.size();
            int curr = 0;

            for (String input : inputs) {
                ConfigurableConsoleHandler.redirectTo(dummy);
                int numEntities = daemon.openFile(new File(home, "input/" + input));
                int numLocations = Utils.size(daemon.getTree().iteratorGlyphsAlive());

                for (GrowFunction g : growFunctions) {
                    boolean isArea = (g.getSpeed() instanceof LinearAreaGrowSpeed);
                    SETTINGS.set(Setting.BORDERS, isArea);
                    SETTINGS.set(Setting.COMPRESSION, isArea);
                    daemon.setGrowFunction(g);

                    for (String algorithm : algorithms) {
                        String name = name(input, numLocations, numEntities, g, algorithm);
                        ConfigurableConsoleHandler.undoRedirect();
                        System.out.println(String.format("[%2d / %2d] %s", ++curr, total, name));

                        if (daemon.isClustered()) {
                            ConfigurableConsoleHandler.redirectTo(dummy);
                            daemon.reopen();
                        }

                        File output = new File(outputDir, name);
                        ConfigurableConsoleHandler.redirectTo(new PrintStream(output));
                        daemon.cluster();
                        ConfigurableConsoleHandler.undoRedirect();
                    }
                }
            }
        }
    }


    private void initGrowFunctions() {
        List<Class<? extends GrowShape>> shapes = Arrays.asList(
            CirclesGrowShape.class,
            SquaresGrowShape.class
        );
        List<Class<? extends GrowSpeed>> speeds = Arrays.asList(
            LinearGrowSpeed.class,
            LinearAreaGrowSpeed.class,
            LogarithmicGrowSpeed.class
        );

        for (Class<? extends GrowSpeed> speed : speeds) {
            for (Class<? extends GrowShape> shape : shapes) {
                growFunctions.add(new GrowFunction(shape, speed));
            }
        }
    }

    private String name(String input) {
        String[] pathElements = input.split("/");
        String[] nameElements = pathElements[pathElements.length - 1].split("\\.");
        return nameElements[0];
    }

    private String name(String input, int numLocations, int numEntities,
            GrowFunction g, String algorithm) {
        StringBuilder sb = new StringBuilder();
        sb.append(name(input));
        sb.append(" : ");
        sb.append(numLocations);
        sb.append(" : ");
        sb.append(numEntities);
        sb.append(" : ");
        sb.append(g.getName());
        sb.append("(");
        if (!SETTINGS.getBoolean(Setting.BORDERS)) {
            sb.append("no ");
        }
        sb.append("borders, ");
        if (!SETTINGS.getBoolean(Setting.COMPRESSION)) {
            sb.append("no ");
        }
        sb.append("compression) : ");
        sb.append(algorithm);
        return sb.toString();
    }

}
