package utils;

import java.io.File;

import algorithm.FirstMergeRecorder;
import algorithm.clustering.QuadTreeClusterer;
import datastructure.Glyph;
import datastructure.QuadTree;
import datastructure.QuadTreeChangeListener;
import datastructure.growfunction.GrowFunction;
import datastructure.queues.BucketingStrategy;
import io.PointIO;
import ui.GrowingGlyphsDaemon;
import utils.Utils.Timers;
import utils.Utils.Timers.Units;

/**
 * This class holds all constants related to the clustering algorithm. Changing
 * the values of these constants will impact performance of the algorithm, and
 * in some cases even its behavior.
 *
 * The class also provides a utility function to find optimal values for the
 * constants. This is done by repeatedly executing the algorithm and doing a
 * binary search to find a sweet spot.
 */
public final class Constants {

    /**
     * Boolean value constants. Short name for easy usage.
     */
    public static enum B {

        /**
         * Whether glyph generators should run asynchronously.
         */
        ASYNC_GLYPH_GENERATORS(false),
        /**
         * Whether the big glyph optimization should be used.
         */
        BIG_GLYPHS(false),
        /**
         * Whether the {@link QuadTreeClusterer} checks the total number of works
         * represented by all alive glyphs after every step of the algorithm.
         * This will only actually enable checking if
         * {@linkplain #LOGGING_ENABLED logging is enabled}.
         */
        CHECK_NUMBER_REPRESENTED(false),
        /**
         * Whether {@link QuadTree} {@link QuadTreeChangeListener listeners} are
         * accepted and notified of events.
         */
        ENABLE_LISTENERS(true),

        /**
         * Whether messages should be logged at all. This overrides logging
         * configuration from {@code logging.properties} (but only negatively,
         * it will not log messages when this is disabled in
         * {@code logging.properties}).
         */
        LOGGING_ENABLED(true),

        /**
         * Whether merge events are to be created for all pairs of glyphs, or only
         * the first one. Setting this to {@code true} implies a performance hit.
         *
         * This constant will also determine whether all out of cell events are
         * put into the global event queue ({@code true}), or not.
         *
         * Please note that {@link QuadTree#MAX_GLYPHS_PER_CELL} cannot be set to
         * high values when setting this to {@code true}, or you need to allocate
         * more memory to the clustering process for large data sets.
         */
        ROBUST(false),

        /**
         * Whether some statistics should be collected that may be time-intensive
         * to collect. Disable this before measuing running time, just in case.
         */
        STATS_ENABLED(false),

        /**
         * Whether timers should be used to track wall clock computation time.
         */
        TIMERS_ENABLED(true),

        /**
         * When {@link #ROBUST} is {@code false}, this flag toggles behavior where
         * glyphs track which glyphs think they'll merge with them first. Merge
         * events are then updated for tracking glyphs, as glyphs merge.
         *
         * Same concern about {@link QuadTree#MAX_GLYPHS_PER_CELL} holds as for
         * {@link #ROBUST}, except for CPU time instead of memory.
         */
        TRACK(true);


        /**
         * Returns the actual value of the constant.
         */
        public boolean get() {
            return value;
        }

        /**
         * Change the value of the constant. Should not be needed normally!
         *
         * @param value New value for the constant.
         */
        public void set(boolean value) {
            // TODO: only allow calling this method from Batch?
            this.value = value;
        }


        /**
         * Value of the constant.
         */
        private boolean value;


        private B(boolean value) {
            this.value = value;
        }
    }


    /**
     * Double value constants. Short name for easy usage.
     */
    public static enum D {

        /**
         * Factor of average number of entities a glyph should represent before
         * it is considered to be a {@linkplain Glyph#isBig() big glyph}.
         */
        BIG_GLYPH_FACTOR(100),

        /**
         * Default maximum radius that the grow function is {@link
         * GrowFunction#initialize(int, double) initialized} with by the {@link
         * GrowingGlyphsDaemon#cluster(boolean, boolean) GrowingGlyphsDaemon}.
         */
        MAX_RADIUS(256),

        /**
         * Minimum width/height of a {@link QuadTree cell}.
         */
        MIN_CELL_SIZE(1e-8),

        /**
         * Minimum zoom factor in the GUI.
         */
        MIN_ZOOM(0.1),

        /**
         * How often the number of merge events processed so far should be
         * logged (if logging is {@link B#LOGGING_ENABLED enabled}). To
         * disable this logging, a value of 0 or smaller can be set.
         */
        TIME_MERGE_EVENT_AGGLOMERATIVE(0);


        /**
         * Returns the actual value of the constant.
         */
        public double get() {
            return value;
        }

        /**
         * Returns a string representation of the actual value of the constant.
         */
        public String getString() {
            return Double.toString(value);
        }


        /**
         * Value of the constant.
         */
        private double value;


        private D(double value) {
            this.value = value;
        }
    }


    /**
     * Enum value constants. Short name for easy usage.
     */
    public static enum E {

        /**
         * Whether the event queue should be split into multiple queues, and when.
         */
        QUEUE_BUCKETING(BucketingStrategy.NO_BUCKETING);


        /**
         * Returns the actual value of the constant.
         */
        @SuppressWarnings("unchecked")
        public <T extends Enum<?>> T get() {
            return (T) value;
        }


        /**
         * Value of the constant.
         */
        private Enum<?> value;


        private E(Enum<?> value) {
            this.value = value;
        }
    }


    /**
     * Integer value constants. Short name for easy usage.
     */
    public static enum I {

        /**
         * Default size (width and height) of the QuadTree in the interface.
         */
        DEFAULT_SIZE(512),

        /**
         * Number of large squares to track throughout program execution.
         * This affects {@link PointIO} marking {@link Glyph glyphs} that it
         * reads from a file to be interesting to {@link Glyph#track track}. The
         * {@link #LARGE_SQUARES_TRACK} biggest glyphs will be tracked.
         *
         * This behavior can be fully disabled by setting the number of squares
         * to track to 0 or smaller.
         */
        LARGE_SQUARES_TRACK(0),

        /**
         * The maximum number of glyphs that should intersect any leaf
         * {@link QuadTree cell} at any point in time. Cells will split when
         * this constant is about to be violated, and will join when a glyph
         * is removed from a cell and joining would not violate this.
         */
        MAX_GLYPHS_PER_CELL(10),

        /**
         * Number of merge events that a glyph will record at most. This is not
         * strictly enforced by the glyph itself, but should be respected by the
         * {@link FirstMergeRecorder} and other code that records merges.
         *
         * More merges can be recorded with a glyph when many merges occur at the
         * exact same time.
         */
        MAX_MERGES_TO_RECORD(4),

        /**
         * Padding around the QuadTree in the GUI, in the preferred display
         * (before panning and zooming has taken place).
         */
        PADDING(10);


        /**
         * Returns the actual value of the constant.
         */
        public int get() {
            return value;
        }


        /**
         * Value of the constant.
         */
        private int value;


        private I(int value) {
            this.value = value;
        }

    }

    /**
     * String value constants. Short name for easy usage.
     */
    public static enum S {

        /**
         * Name of the algorithm that is used by default.
         */
        CLUSTERER("QuadTree Clusterer"),

        /**
         * Name of the grow function that is used by default.
         *
         * @see GrowFunction#getAll()
         */
        GROW_FUNCTION("Linear Growing Squares");


        /**
         * Returns the actual value of the constant.
         */
        public String get() {
            return value;
        }


        /**
         * Value of the constant.
         */
        private String value;


        private S(String value) {
            this.value = value;
        }
    }


    /**
     * Will repeatedly run the algorithm on some input, and optimize a number
     * of constant values of interest. When done, these values are reported.
     *
     * @param args Command line arguments; should be path of data set to test on.
     */
    public static void main(String[] args) {
        File toOpen;
        String gName = S.GROW_FUNCTION.get();
        int argI = 0;
        if (args.length > argI) {
            toOpen = new File(args[argI++]);
            if (!toOpen.isFile() || !toOpen.canRead()) {
                System.err.println("Cannot open file for reading.");
                return;
            }
            if (args.length > argI) {
                gName = args[argI++].replaceAll("_", " ");
            }
        } else {
            System.err.println("Pass path to file to test on as only argument.");
            return;
        }

        GrowFunction g = GrowFunction.getAll().get(gName);
        if (g == null) {
            System.err.println("Uknown grow function '" + gName + "'");
            return;
        }
        GrowingGlyphsDaemon daemon = new GrowingGlyphsDaemon(
                I.DEFAULT_SIZE.get(), I.DEFAULT_SIZE.get(), g);
        System.out.println("using " + g.getName());

        System.out.print("warming up: ");
        run(daemon, toOpen, true);
        if (args.length > argI && args[argI++].equals("repeat")) {
            int numRepeats = 5;
            if (args.length > argI && args[argI++].matches("^\\d+$")) {
                numRepeats = Integer.parseInt(args[argI - 1]);
            }
            for (int i = 0; i < numRepeats; ++i) {
                run(daemon, toOpen);
            }
        } else {
            search(I.MAX_GLYPHS_PER_CELL, 32, daemon, toOpen);
            search(I.MAX_MERGES_TO_RECORD, 4, daemon, toOpen);
        }
    }


    private static double run(GrowingGlyphsDaemon daemon, File toOpen) {
        return run(daemon, toOpen, false);
    }

    /**
     * Perform clustering a number of times, return the average running time.
     */
    private static double run(GrowingGlyphsDaemon daemon, File toOpen, boolean warmingUp) {
        if (!warmingUp) {
            System.out.print(String.format(
                    "%4d / %3d: ",
                    I.MAX_GLYPHS_PER_CELL.value,
                    I.MAX_MERGES_TO_RECORD.value));
        }

        Stat runTime = new Stat();
        for (int i = 0; i < 10; ++i) {
            daemon.openFile(toOpen);
            try {
                daemon.cluster();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runTime.record(Timers.in(Timers.elapsed("clustering"), Units.SECONDS));
            System.out.print(".");
        }
        if (warmingUp) {
            System.out.println();
        } else {
            System.out.println(String.format("%6.2f", runTime.getAverage()));
        }
        return runTime.getAverage();
    }

    /**
     * Searches for a sweet spot that optimizes average running time.
     */
    private static void search(I constant, int initial, GrowingGlyphsDaemon daemon, File toOpen) {
        constant.value = initial;
        double curr = run(daemon, toOpen);
        double last;
        int direction = 1;
        int step = constant.value / 2;
        int minValue = constant.value;
        double minTime = Double.POSITIVE_INFINITY;
        do {
            constant.value += direction * step;
            last = curr;
            curr = run(daemon, toOpen);
            if (curr < minTime) {
                minTime = curr;
                minValue = constant.value;
            }
            if (curr >= last) {
                direction *= -1;
                step /= 2;
            }
        } while (step > 0 && (curr >= last || curr <= last - 0.01));
        constant.value = minValue;
    }

}
