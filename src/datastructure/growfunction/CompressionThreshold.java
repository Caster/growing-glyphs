package datastructure.growfunction;

import java.util.NavigableSet;
import java.util.TreeSet;

import datastructure.Glyph;

/**
 * A grow function may use compression, which is captured by instances of this
 * class. In a nutshell, when the number of entities represented by a glyph
 * surpasses a given threshold, then the compression factor recorded by an
 * instance of this class can be used to scale down the glyph. This is done by
 * multiplying the number of entities represented by the glyph with this factor
 * before feeding it to the actual grow function.
 */
public class CompressionThreshold {

    /**
     * Threshold object (re)used for querying, to avoid repeated construction of
     * Threshold objects.
     */
    private static final Threshold QUERY = new Threshold(0);


    private NavigableSet<Threshold> thresholds;

    public CompressionThreshold() {
        this.thresholds = new TreeSet<Threshold>();
    }

    /**
     * Add a threshold that will make it so that glyphs representing at least
     * {@code threshold} entities will be compressed by a factor
     * {@code compression}, unless some other threshold with greater value
     * applies.
     */
    public void add(int threshold, double compression) {
        if (Double.isFinite(compression)) {
            thresholds.add(new Threshold(
                    thresholds.size() + 1, threshold, compression));
        }
    }

    /**
     * Clear all thresholds recorded so far.
     */
    public void clear() {
        thresholds.clear();
    }

    /**
     * {@link #clear() Clear} all thresholds and replace them by the defaults
     * associated with the given data set. When the given data set is unknown,
     * no thresholds are removed nor added.
     *
     * @param dataSet Name of the data set to find defaults for.
     * @return Whether defaults were found, and thus this
     *         {@link CompressionThreshold} changed state, or not.
     */
    public boolean defaultFor(String dataSet) {
        if (dataSet != null && dataSet.startsWith("trove")) {
            // Jasper used 25/39 from 1_000_000 and 4/9 from 10_000_000
            // according to his thesis (approx. 0.64 and 0.44). We tweaked this
            // to the below values, that appear to work well with the Linear
            // Area Squares grow function.
            add( 1_000_000, 0.3);
            add( 5_000_000, 0.2);
            add(10_000_000, 0.1);
            return true;
        }
        return false;
    }

    /**
     * Given a glyph, return the compression factor to be used on that glyph.
     *
     * @param glyph Glyph to find compression factor for.
     */
    public double getCompression(Glyph glyph) {
        Threshold toUse = getThreshold(glyph);
        if (toUse == null) {
            return 1d;
        }
        return toUse.compression;
    }

    /**
     * Given a glyph, return the index of the threshold that applies to it.
     *
     * @param glyph Glyph to find compression level of.
     * @see #getCompression(Glyph)
     */
    public int getCompressionLevel(Glyph glyph) {
        Threshold toUse = getThreshold(glyph);
        if (toUse == null) {
            return thresholds.size() + 1;
        }
        return toUse.level;
    }

    /**
     * Given a glyph, return the weight of that glyph. This will normally be the
     * number of entities represented by the glyph, but it may be multiplied by
     * a compression factor if thresholds have been added and apply.
     *
     * @param glyph Glyph to read weight of.
     */
    public double getN(Glyph glyph) {
        Threshold toUse = getThreshold(glyph);
        if (toUse == null) {
            return glyph.getN();
        }
        return glyph.getN() * toUse.compression;
    }

    /**
     * Returns the number of thresholds recorded so far.
     */
    public int size() {
        return thresholds.size();
    }


    /**
     * Given a glyph, find the threshold to use on it.
     *
     * @param glyph Glyph to find threshold for.
     */
    private Threshold getThreshold(Glyph glyph) {
        if (!glyph.threshold.isSet()) {
            QUERY.threshold = glyph.getN();
            glyph.threshold.set(thresholds.ceiling(QUERY));
        }

        return glyph.threshold.get();
    }


    public static class Threshold implements Comparable<Threshold> {

        private final int level;
        private final double compression;
        private int threshold; // not final because of QUERY

        public Threshold(int threshold) {
            this(-1, threshold);
        }

        public Threshold(int level, int threshold) {
            this(level, threshold, Double.POSITIVE_INFINITY);
        }

        public Threshold(int level, int threshold, double compression) {
            this.level = level;
            this.compression = compression;
            this.threshold = threshold;
        }

        @Override
        public int compareTo(Threshold that) {
            return this.threshold - that.threshold;
        }

    }

}
