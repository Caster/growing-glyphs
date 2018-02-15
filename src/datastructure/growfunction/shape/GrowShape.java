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
     * Returns the distance between two points {@code p} and {@code q}.
     *
     * @param px X-coordinate of {@code p}.
     * @param py Y-coordinate of {@code p}.
     * @param qx X-coordinate of {@code q}.
     * @param qy Y-coordinate of {@code q}.
     */
    public double dist(double px, double py, double qx, double qy);

    /**
     * Returns the minimum distance between a point and any point in the given
     * rectangle. This will in particular return 0 when the given point is
     * contained in the rectangle.
     *
     * @param rect Description of rectangle.
     * @param px X-coordinate of {@code p}.
     * @param py Y-coordinate of {@code p}.
     */
    public double dist(Rectangle2D rect, double px, double py);

    /**
     * Returns a shape representing the glyph at the given time stamp/zoom
     * level, according to this grow function and with the given compression
     * level applied (the higher the compression level, the thicker the border
     * around the glyph will be.
     *
     * <p>In particular, a glyph with compression level <code>k</code> will have a
     * border of width <code>2k</code>. See {@link #radius(double, int)}.
     *
     * @param glyph glyph to compute the size of.
     * @param at Time stamp or zoom level at which size must be computed.
     * @param compressionLevel The compression level that is to be applied to
     *            the given glyph; this determines the border width.
     * @return A rectangle representing the glyph at time/zoom {@code at}.
     */
    public Shape sizeAt(Glyph g, double at, int c);

}
