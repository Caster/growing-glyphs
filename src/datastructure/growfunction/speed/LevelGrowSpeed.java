package datastructure.growfunction.speed;

import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * A grow speed that scales glyphs linearly without taking weights into account.
 */
public class LevelGrowSpeed extends GrowSpeedBase {

    public LevelGrowSpeed(GrowFunction g) {
        super(g);
    }

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        if (a.hasSamePositionAs(b)) {
            return Double.NEGATIVE_INFINITY;
        }
        return gf.dist(a, b) / 2.0;
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph g) {
        return gf.dist(r, g);
    }

    @Override
    public double radius(Glyph g, double at) {
        return at;
    }

}
