package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.List;

import datastructure.Glyph;
import datastructure.QuadTree;
import datastructure.events.OutOfCell.Side;

/**
 * Function determining how {@link Glyph Glyphs} should be scaled.
 */
public abstract class GrowFunction {

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
    public abstract double exitAt(Glyph glyph, QuadTree cell, Side side);

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
     * @param s Growing glyph.
     * @return Zoom level at which {@code r} and {@code s} touch.
     */
    public abstract double intersectAt(Rectangle2D r, Glyph s);

    /**
     * Same as {@link #intersectAt(Rectangle2D, Glyph)}, just with different order
     * of parameters. This is a convenience function.
     */
    public double intersectAt(Glyph s, Rectangle2D r) {
        return intersectAt(r, s);
    }

    /**
     * Returns a rectangle representing the glyph at the given time stamp/zoom
     * level, according to this grow function.
     *
     * @param s glyph to compute the size of.
     * @param at Time stamp or zoom level at which size must be computed.
     * @return A rectangle representing the glyph at time/zoom {@code at}.
     */
    public abstract Shape sizeAt(Glyph s, double at);

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
