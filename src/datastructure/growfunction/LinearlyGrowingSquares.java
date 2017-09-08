package datastructure.growfunction;

import java.awt.geom.Rectangle2D;

import datastructure.QuadTree;
import datastructure.Glyph;
import datastructure.Utils;
import datastructure.events.OutOfCell.Side;

/**
 * A simple {@link GrowFunction} that scales {@link Glyph squares} linearly.
 * For this function, the zoom level is interpreted as time. The radius of a
 * square at time {@code t} is then defined as {@code t * n}, where {@code n}
 * is the number of entities represented by the square.
 */
public class LinearlyGrowingSquares extends GrowFunction {

        @Override
        public double exitAt(Glyph square, QuadTree cell, Side side) {
            return intersectAt(cell.getSide(side), square);
        }

        @Override
        public double intersectAt(Glyph a, Glyph b) {
            double d = Utils.chebyshev(a.getX(), a.getY(), b.getX(), b.getY());
            return d / (a.getN() + b.getN());
        }

        @Override
        public double intersectAt(Rectangle2D r, Glyph s) {
            double d = Utils.chebyshev(r, s.getX(), s.getY());
            return d / s.getN();
        }

        @Override
        public Rectangle2D sizeAt(Glyph square, double at) {
            int n = square.getN();
            return new Rectangle2D.Double(
                    square.getX() - at * n,
                    square.getY() - at * n,
                    2 * at * n, 2 * at * n
                );
        }

}
