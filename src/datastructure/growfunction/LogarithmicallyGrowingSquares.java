package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

/**
 * This {@link GrowFunction} scales exactly like
 * {@link LogarithmicallyGrowingCircles}, but instead uses square shaped glyphs.
 */
public class LogarithmicallyGrowingSquares extends LogarithmicGrowFunction {

    @Override
    protected double dist(double px, double py, double qx, double qy) {
        return Utils.chebyshev(px, py, qx, qy);
    }

    @Override
    protected double dist(Rectangle2D rect, double px, double py) {
        return Utils.chebyshev(rect, px, py);
    }

    @Override
    public Shape sizeAt(Glyph c, double at) {
        double r = (Math.log1p(at * w(c)) / LOG_DIV) * fA;
        return new Rectangle2D.Double(
                c.getX() - r,
                c.getY() - r,
                2 * r, 2 * r);
    }

}
