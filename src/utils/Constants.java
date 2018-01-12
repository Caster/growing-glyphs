package utils;

import java.io.File;

import algorithm.FirstMergeRecorder;
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
         * Whether {@link QuadTree} {@link QuadTreeChangeListener listeners} are
         * accepted and notified of events.
         */
        ENABLE_LISTENERS(true),

        /**
         * Whether messages should be logged at all. This overrides logging
         * configuration from {@code logging.properties}.
         */
        LOGGING_ENABLED(true),

        /**
         * Whether merge events are to be created for all pairs of glyphs, or only
         * the first one. In practice, creating only the first merge event appears
         * to result in clusterings free of overlap, but in theory overlap can
         * occur. Setting this to {@code true} implies a performance hit.
         *
         * Please note that {@link QuadTree#MAX_GLYPHS_PER_CELL} cannot be set to
         * high values when setting this to {@code true}, or you need to allocate
         * more memory to the clustering process for large data sets.
         */
        ROBUST(false),

        /**
         * Whether timers should be used.
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
         * Minimum width/height of a {@link QuadTree cell}.
         */
        MIN_CELL_SIZE(0.0001),

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
        LARGE_SQUARES_TRACK(5),

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
        MAX_MERGES_TO_RECORD(4);


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
     * Will repeatedly run the algorithm on some input, and optimize a number
     * of constant values of interest. When done, these values are reported.
     *
     * @param args Command line arguments; should be path of data set to test on.
     */
    public static void main(String[] args) {
        File toOpen;
        String gName = GrowFunction.DEFAULT;
        if (args.length > 0) {
            toOpen = new File(args[0]);
            if (!toOpen.isFile() || !toOpen.canRead()) {
                System.err.println("Cannot open file for reading.");
                return;
            }
            if (args.length > 1) {
                gName = args[1].replaceAll("_", " ");
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
        if (args.length > 1 && args[1].equals("repeat")) {
            for (int i = 0; i < 5; ++i) {
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
            daemon.cluster();
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
