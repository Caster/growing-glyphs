package ui;

import static gui.GrowingGlyphs.SETTINGS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import gui.Settings.Setting;
import logging.ConfigurableConsoleHandler;
import utils.Constants.B;
import utils.Utils;
import utils.Utils.Stats;
import utils.Utils.Timers;
import utils.Utils.Timers.Units;

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
    private final List<GrowFunction> growFunctions;
    private final File home;
    private final List<String> inputs;
    private final PrintStream logStream;
    private final File outputDir;
    private final File outputFile;


    public Batch(File home) {
        this.algorithms = Arrays.asList(
                "naive", "basic:all events", "basic", "basic:big"
            );
        this.growFunctions = new ArrayList<>(6);
        this.home = home;
        this.inputs = Arrays.asList(
                "csv/glottovis.tsv",
                "points/trove",
                "points/big-glyph-10k",
                "points/big-glyph-50k",
                "points/big-glyph-100k",
                "points/big-glyph-200k",
                "points/uniform-10k",
                "points/uniform-50k",
                "points/uniform-100k",
                "points/uniform-200k"
            );
        this.outputDir = new File(home, "output/" + System.currentTimeMillis());

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new RuntimeException("could not create output directory");
            }
        } else if (!outputDir.isDirectory()) {
            throw new RuntimeException("output directory exists, but is not a directory");
        }

        this.outputFile = new File(outputDir, "log.txt");
        try {
            this.logStream = new PrintStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        initGrowFunctions();
    }

    public void run() throws IOException {
        try (PrintStream dummy = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException { /* ignored */ }})) {
            int total = inputs.size() * growFunctions.size() * algorithms.size();
            int curr = 0;

            ExecutorService executor = Executors.newSingleThreadExecutor();
            for (String input : inputs) {
                for (GrowFunction g : growFunctions) {
                    boolean isArea = (g.getSpeed() instanceof LinearAreaGrowSpeed);
                    SETTINGS.set(Setting.BORDERS, isArea);
                    SETTINGS.set(Setting.COMPRESSION, isArea);

                    for (String algorithm : algorithms) {
                        Future<Void> future = executor.submit(new Task(
                                algorithm, ++curr, total, input, g, dummy));

                        try {
                            future.get(5, TimeUnit.MINUTES);
                        } catch (TimeoutException te) {
                            future.cancel(true);
                            log("            → timed out");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            executor.shutdownNow();
        } finally {
            logStream.close();
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

    private void log(String string) {
        System.out.println(string);
        logStream.println(string);
    }

    private void prepare(String algorithm, GrowingGlyphsDaemon daemon) {
        String[] elements = algorithm.split(":");
        switch (elements[0]) {
        case "basic":
            daemon.setClusterer(new QuadTreeClusterer(daemon.getTree()));
            break;
        case "naive":
            daemon.setClusterer(new NaiveClusterer(daemon.getTree()));
            break;
        };

        B.BIG_GLYPHS.set(false);
        B.ROBUST.set(false);

        for (int i = 1; i < elements.length; ++i) {
            switch (elements[i]) {
            case "all events":
                B.ROBUST.set(true);
                break;
            case "big":
                B.BIG_GLYPHS.set(true);
                break;
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


    private class Task implements Callable<Void> {
        private final String algorithm;
        private final int curr;
        private final int total;
        private final String input;
        private final GrowFunction g;
        private final PrintStream dummy;

        private Task(String algorithm, int curr, int total, String input,
                GrowFunction g, PrintStream dummy) {
            this.algorithm = algorithm;
            this.curr = curr;
            this.total = total;
            this.input = input;
            this.g = g;
            this.dummy = dummy;
        }

        @Override
        public Void call() throws Exception {
            Stats.reset();
            Timers.reset();
            GrowingGlyphsDaemon daemon = new GrowingGlyphsDaemon(512, 512, null);

            // set correct clusterer
            prepare(algorithm, daemon);

            // open input
            ConfigurableConsoleHandler.redirectTo(dummy);
            int numEntities = daemon.openFile(new File(home, "input/" + input));
            int numLocations = Utils.size(daemon.getTree().iteratorGlyphsAlive());

            // set growfunction to use
            daemon.setGrowFunction(g);

            // show progress
            String name = name(input, numLocations, numEntities, g, algorithm);
            ConfigurableConsoleHandler.undoRedirect();
            log(String.format("[%3d / %3d] %s", curr, total, name));

            if ((B.BIG_GLYPHS.get() && g.getSpeed().getClass() != LinearGrowSpeed.class) ||
                    (daemon.getClusterer().getClass() == NaiveClusterer.class && numLocations >= 1e4)) {
                log("            → skipped");
                return null;
            }

            // do actual clustering, write output to file
            File output = new File(outputDir, name);
            ConfigurableConsoleHandler.redirectTo(new PrintStream(output));
            daemon.cluster();
            ConfigurableConsoleHandler.undoRedirect();

            log(String.format("            → %.2f seconds",
                    Timers.in(Timers.elapsed("clustering"), Units.SECONDS)));

            return null;
        }
    }

}
