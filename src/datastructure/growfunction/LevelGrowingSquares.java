package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

public class LevelGrowingSquares extends GrowFunction {

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        return Utils.chebyshev(a.getX(), a.getY(), b.getX(), b.getY()) / 2d;
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph s) {
        return Utils.chebyshev(r, s.getX(), s.getY());
    }

    @Override
    public Shape sizeAt(Glyph g, double at, int c) {
        return new Rectangle2D.Double(
                g.getX() - at - 2 * c,
                g.getY() - at - 2 * c,
                2 * at + 4 * c, 2 * at + 4 * c
            );
    }

}
