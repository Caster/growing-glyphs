package datastructure.growfunction.speed;

import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * Interface that partially determines {@linkplain GrowFunction grow functions};
 * this part dictates how fast glyphs grow.
 */
public interface GrowSpeed {

    /**
     * Initialize a grow function to fit the specific data set that is to be
     * clustered. The default implementation of this function does nothing, but
     * specific grow function implementations may use this method to set
     * parameters and fit better on the data set.
     *
     * @param numGlyphs The number of glyphs that is present initially.
     * @param maxRadius The maximum radius of glyphs. The minimum radius of glyphs
     *            is always 0 because of restrictions in the clustering algorithm.
     * @see GrowFunction#initialize(int, double)
     */
    public void initialize(int numGlyphs, double maxRadius);

    /**
     * Returns at which zoom level two glyphs will touch. Both glyphs are
     * scaled using this {@link GrowFunction}.
     *
     * @param a First glyph.
     * @param b Second glyph.
     * @return Zoom level at which {@code a} and {@code b} touch. Returns
     *     {@link Double#NEGATIVE_INFINITY} if the two glyphs share coordinates.
     */
    public double intersectAt(Glyph a, Glyph b);

    /**
     * Returns at which zoom level a glyph touches a static rectangle. The
     * glyph is scaled using this {@link GrowFunction}.
     *
     * @param r Static rectangle.
     * @param glyph Growing glyph.
     * @return Zoom level at which {@code r} and {@code glyph} touch. If the glyph
     *     is contained in the rectangle, {@link Double#NEGATIVE_INFINITY} must be
     *     returned. A negative value may still be returned in case the
     *     {@code glyph} is right outside {@code r}, but its border overlaps it.
     */
    public double intersectAt(Rectangle2D r, Glyph glyph);

    /**
     * Returns the radius of the given glyph at the given time stamp/zoom level.
     *
     * @param g Glyph to calculate radius of.
     * @param at Time stamp/zoom level to determine radius at.
     */
    public double radius(Glyph g, double at);

}
