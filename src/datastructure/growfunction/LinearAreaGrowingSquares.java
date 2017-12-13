package datastructure.growfunction;

import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

/**
 * Grow function that scales squares in such a way that their area grows linearly.
 */
public class LinearAreaGrowingSquares extends GrowFunction {

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        double d = Utils.chebyshev(a.getX(), a.getY(), b.getX(), b.getY());
        return d / (w(a) + w(b));
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph s) {
        double d = Utils.chebyshev(r, s.getX(), s.getY());
        return d / w(s);
    }

    @Override
    public Rectangle2D sizeAt(Glyph s, double at) {
        double weight = w(s);
        return new Rectangle2D.Double(
                s.getX() - at * weight,
                s.getY() - at * weight,
                2 * at * weight, 2 * at * weight
            );
    }

    @Override
    protected double w(Glyph glyph) {
        double compression = thresholds.getCompression(glyph);
        return compression * Math.sqrt(glyph.getN());
    }

}