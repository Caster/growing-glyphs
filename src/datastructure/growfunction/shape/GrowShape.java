package datastructure.growfunction.shape;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * Interface that partially determines {@linkplain GrowFunction grow functions};
 * this part dictates which shape glyphs have.
 */
public interface GrowShape {

    /**
     * Returns the distance between two glyphs. This is the distance the glyphs
     * have to bridge before their borders will touch. Effectively, this method
     * returns the distance between their center points minus the widths of
     * their respective borders. The value returned by this method may be
     * negative in case the borders of glyphs overlap.
     *
     * @param a First glyph.
     * @param b Second glyph.
     */
    public double dist(Glyph a, Glyph b);

    /**
     * Returns the minimum distance between a glyph and any point in the given
     * rectangle. This will in particular return 0 when the given glyph's center
     * point is contained in the rectangle.
     *
     * <p>As with {@link #dist(Glyph, Glyph)}, this method takes the border
     * width of the glyph into account. This means that it effectively returns
     * the minimum distance between the center point of the glyph and the
     * rectangle, minus the width of the glyph's border. This method will return
     * a non-negative value though.
     *
     * @param rect Description of rectangle.
     * @param g Glyph to consider.
     */
    public double dist(Rectangle2D rect, Glyph g);

    /**
     * Returns a shape representing the glyph at the given time stamp/zoom
     * level, according to this grow function and with the given compression
     * level applied (the higher the compression level, the thicker the border
     * around the glyph will be.
     *
     * @param glyph glyph to compute the size of.
     * @param at Time stamp or zoom level at which size must be computed.
     * @param compressionLevel The compression level that is to be applied to
     *            the given glyph; this determines the border width.
     * @return A rectangle representing the glyph at time/zoom {@code at}.
     * @see GrowFunction#radius(double, int)
     */
    public Shape sizeAt(Glyph g, double at, int c);

}
