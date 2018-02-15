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
        return gf.dist(a.getX(), a.getY(), b.getX(), b.getY()) /
                (weight(a) + weight(b));
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph c) {
        return gf.dist(r, c.getX(), c.getY()) / weight(c);
    }

    @Override
    public double radius(Glyph g, double at) {
        return at * weight(g);
    }

}
