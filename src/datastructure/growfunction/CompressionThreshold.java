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
            thresholds.add(new Threshold(threshold, compression));
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
        switch (dataSet) {
        case "trove":
            clear();
            add( 1_000_000, 25d / 39d); // about 0.64
            add(10_000_000, 4d / 9d); // about 0.44
            return true;
        }
        return false;
    }

    /**
     * Given a glyph, return the weight of that glyph. This will normally be the
     * number of entities represented by the glyph, but it may be multiplied by
     * a compression factor if thresholds have been added and apply.
     *
     * @param glyph Glyph to read weight of.
     */
    public double getN(Glyph glyph) {
        Threshold toUse = thresholds.floor(new Threshold(glyph.getN()));
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


    private static class Threshold implements Comparable<Threshold> {

        public final double compression;
        public final int threshold;

        public Threshold(int threshold) {
            this(threshold, Double.POSITIVE_INFINITY);
        }

        public Threshold(int threshold, double compression) {
            this.compression = compression;
            this.threshold = threshold;
        }

        @Override
        public int compareTo(Threshold that) {
            return this.threshold - that.threshold;
        }

    }

}
