package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

public class LevelGrowingCircles extends GrowFunction {

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        return Utils.euclidean(a.getX(), a.getY(), b.getX(), b.getY()) / 2d;
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph s) {
        return Utils.euclidean(r, s.getX(), s.getY());
    }

    @Override
    public Shape sizeAt(Glyph s, double at) {
        return new Ellipse2D.Double(
                s.getX() - at,
                s.getY() - at,
                2 * at, 2 * at
            );
    }

}
