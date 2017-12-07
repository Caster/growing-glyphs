package datastructure.growfunction;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

/**
 * Grow function that scales circles in such a way that their area grows linearly.
 */
public class LinearAreaGrowingCircles extends GrowFunction {

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        double d = Utils.euclidean(a.getX(), a.getY(), b.getX(), b.getY());
        return d / (w(a) + w(b));
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph c) {
        double d = Utils.euclidean(r, c.getX(), c.getY());
        return d / w(c);
    }

    @Override
    public Ellipse2D sizeAt(Glyph c, double at) {
        double weight = w(c);
        return new Ellipse2D.Double(
                c.getX() - at * weight,
                c.getY() - at * weight,
                2 * at * weight, 2 * at * weight
            );
    }

    @Override
    protected double w(Glyph glyph) {
        double compression = thresholds.getCompression(glyph);
        return compression * Math.sqrt(glyph.getN());
    }

}
