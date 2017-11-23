package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static final String DEFAULT = "Linearly Growing Squares";


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
            ALL.put("Linearly Growing Circles", new LinearlyGrowingCircles());
            ALL.put(DEFAULT, new LinearlyGrowingSquares());
        }
        return ALL;
    }


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

}
