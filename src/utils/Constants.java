package utils;

import java.io.File;

import algorithm.FirstMergeRecorder;
import datastructure.QuadTree;
import datastructure.QuadTreeChangeListener;
import datastructure.growfunction.GrowFunction;
import datastructure.queues.BucketingStrategy;
import ui.GrowingGlyphsDaemon;
import utils.Utils.Stats;
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
        MIN_CELL_SIZE(0.001);


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
         * The maximum number of glyphs that should intersect any leaf
         * {@link QuadTree cell} at any point in time. Cells will split when
         * this constant is about to be violated, and will join when a glyph
         * is removed from a cell and joining would not violate this.
         */
        MAX_GLYPHS_PER_CELL(35),

        /**
         * Number of merge events that a glyph will record at most. This is not
         * strictly enforced by the glyph itself, but should be respected by the
         * {@link FirstMergeRecorder} and other code that records merges.
         *
         * More merges can be recorded with a glyph when many merges occur at the
         * exact same time.
         */
        MAX_MERGES_TO_RECORD(5);


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
        if (args.length > 0) {
            toOpen = new File(args[0]);
            if (!toOpen.isFile() || !toOpen.canRead()) {
                System.err.println("Cannot open file for reading.");
                return;
            }
        } else {
            System.err.println("Pass path to file to test on as only argument.");
            return;
        }

        GrowFunction g = GrowFunction.getAll().get(GrowFunction.DEFAULT);
        GrowingGlyphsDaemon daemon = new GrowingGlyphsDaemon(
                I.DEFAULT_SIZE.get(), I.DEFAULT_SIZE.get(), g);
        Stat runTime = new Stat();
        for (int i = 0; i < 10; ++i) {
            daemon.openFile(toOpen);
            System.out.println(daemon.getTree().getTreeHeight());
            daemon.cluster();
            runTime.record(Timers.in(Timers.elapsed("clustering"), Units.SECONDS));
            System.out.print(".");

            Stats.reset();
            Timers.reset();
        }
        System.out.println();
        System.out.println(runTime.getAverage());
    }

}
