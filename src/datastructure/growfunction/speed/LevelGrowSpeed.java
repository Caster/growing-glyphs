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
        return gf.dist(a.getX(), a.getY(), b.getX(), b.getY()) / 2.0;
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph g) {
        return gf.dist(r, g.getX(), g.getY());
    }

    @Override
    public double radius(Glyph g, double at) {
        return at;
    }

}
