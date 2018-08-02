package datastructure.growfunction.speed;

import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * A grow speed that scales glyphs linear in their weight.
 */
public class LinearGrowSpeed extends GrowSpeedBase {

    public LinearGrowSpeed(GrowFunction g) {
        super(g);
    }

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        if (a.hasSamePositionAs(b)) {
            return Double.NEGATIVE_INFINITY;
        }
        return gf.dist(a, b) / (weight(a) + weight(b));
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph g) {
        double d = gf.dist(r, g);
        if (Double.isInfinite(d)) {
            return d;
        }
        return d / weight(g);
    }

    @Override
    public double radius(Glyph g, double at) {
        return Math.max(0, at) * weight(g);
    }

}
