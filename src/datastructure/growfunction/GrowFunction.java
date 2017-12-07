package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import datastructure.Glyph;
import datastructure.QuadTree;
import datastructure.events.OutOfCell.Side;

/**
 * Function determining how {@link Glyph Glyphs} should be scaled.
 */
public abstract class GrowFunction {

    /**
     * Name of the grow function that is used by default.
     */
    public static final String DEFAULT = "Linear Area Growing Squares";


    /**
     * Map of names to instances of grow functions. These instances can be used
     * throughout the program, creating new instances should never be necessary.
     */
    private static final Map<String, GrowFunction> ALL = new HashMap<>();


    /**
     * Returns a map of names to instances of grow functions. Theses instances
     * can always be used, creating new instances should never be necessary.
     */
    public static Map<String, GrowFunction> getAll() {
        if (ALL.isEmpty()) {
            for (GrowFunction g : Arrays.asList(
                    new LevelGrowingCircles(),
                    new LevelGrowingSquares(),
                    new LinearAreaGrowingCircles(),
                    new LinearAreaGrowingSquares(),
                    new LinearlyGrowingCircles(),
                    new LinearlyGrowingSquares(),
                    new LogarithmicallyGrowingCircles(),
                    new LogarithmicallyGrowingSquares(),
                    new LogarithmicallyGrowingCirclesBounded(),
                    new LogarithmicallyGrowingSquaresBounded())) {
                ALL.put(g.getName(), g);
            }
        }
        return ALL;
    }


    /**
     * Thresholds that apply to this grow function.
     */
    public final CompressionThreshold thresholds = new CompressionThreshold();


    /**
     * Human readable name of this grow function. Created by {@link #getName()},
     * which uses this field as a cache.
     */
    protected String name = null;


    /**
     * Returns at which zoom level a glyph touches the given side of the given
     * cell. The glyph is scaled using this {@link GrowFunction}.
     *
     * @param glyph Growing glyph.
     * @param cell Cell that glyph is assumed to be inside of, altough if not
     *            the time of touching is still correctly calculated.
     * @param side Side of cell for which calculation should be done.
     * @return Zoom level at which {@code glyph} touches {@code side} side of
     *         {@code cell}.
     */
    public double exitAt(Glyph glyph, QuadTree cell, Side side) {
        return intersectAt(cell.getSide(side), glyph);
    }

    /**
     * Returns the human readable name of this function. This method guarantees
     * to always return the same exact instance of {@link String}.
     */
    public String getName() {
        if (this.name == null) {
            String name = getClass().getName();
            Matcher m = Pattern.compile("[A-Z][a-z]+")
                    .matcher(name.substring(name.lastIndexOf('.') + 1));
            if (m.find()) {
                StringBuilder result = new StringBuilder(m.group(0));
                while (m.find()) {
                    result.append(" ");
                    result.append(m.group(0));
                }
                this.name = result.toString();
                if (this.name.equals(DEFAULT)) {
                    this.name = DEFAULT;
                }
            } else {
                this.name = "unknown grow function";
            }
        }
        return this.name;
    }

    /**
     * Initialize a grow function to fit the specific data set that is to be
     * clustered. The default implementation of this function does nothing, but
     * specific grow function implementations may use this method to set
     * parameters and fit better on the data set.
     *
     * @param numGlyphs The number of glyphs that is present initially.
     * @param maxRadius The maximum radius of glyphs. The minimum radius of glyphs
     *            is always 0 because of restrictions in the clustering algorithm.
     */
    public void initialize(int numGlyphs, double maxRadius) {
        // default implementation does nothing
    }

    /**
     * Returns at which zoom level two glyphs will touch. Both glyphs are
     * scaled using this {@link GrowFunction}.
     *
     * @param a First glyph.
     * @param b Second glyph.
     * @return Zoom level at which {@code a} and {@code b} touch.
     */
    public abstract double intersectAt(Glyph a, Glyph b);

    /**
     * Returns at which zoom level a glyph touches a static rectangle. The
     * glyph is scaled using this {@link GrowFunction}.
     *
     * @param r Static rectangle.
     * @param glyph Growing glyph.
     * @return Zoom level at which {@code r} and {@code glyph} touch.
     */
    public abstract double intersectAt(Rectangle2D r, Glyph glyph);

    /**
     * Same as {@link #intersectAt(Rectangle2D, Glyph)}, just with different order
     * of parameters. This is a convenience function.
     */
    public double intersectAt(Glyph glyph, Rectangle2D r) {
        return intersectAt(r, glyph);
    }

    /**
     * Returns a shape representing the glyph at the given time stamp/zoom
     * level, according to this grow function.
     *
     * @param glyph glyph to compute the size of.
     * @param at Time stamp or zoom level at which size must be computed.
     * @return A rectangle representing the glyph at time/zoom {@code at}.
     */
    public abstract Shape sizeAt(Glyph glyph, double at);

    public Shape[] sizesAt(double at, Glyph... glyphs) {
        Shape[] result = new Shape[glyphs.length];
        for (int i = 0; i < glyphs.length; ++i) {
            result[i] = sizeAt(glyphs[i], at);
        }
        return result;
    }

    public Shape[] sizesAt(double at, List<Glyph> glyphs) {
        return this.sizesAt(at, glyphs.toArray(new Glyph[0]));
    }


    /**
     * Returns the actual weight of a glyph.
     */
    protected double w(Glyph glyph) {
        return thresholds.getN(glyph);
    }

}
