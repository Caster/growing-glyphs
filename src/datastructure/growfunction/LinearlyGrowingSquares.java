package datastructure.growfunction;

import java.awt.geom.Rectangle2D;

import datastructure.QuadTree;
import datastructure.Square;
import datastructure.Utils;
import datastructure.events.OutOfCell.Side;

/**
 * A simple {@link GrowFunction} that scales {@link Square squares} linearly.
 * For this function, the zoom level is interpreted as time. The radius of a
 * square at time {@code t} is then defined as {@code t * n}, where {@code n}
 * is the number of entities represented by the square.
 */
public class LinearlyGrowingSquares extends GrowFunction {

        @Override
        public double exitAt(Square square, QuadTree cell, Side side) {
            return intersectAt(cell.getSide(side), square);
        }

        @Override
        public double intersectAt(Square a, Square b) {
            double d = Utils.chebyshev(a.getX(), a.getY(), b.getX(), b.getY());
            return d / (a.getN() + b.getN());
        }

        @Override
        public double intersectAt(Rectangle2D r, Square s) {
            double d = Utils.chebyshev(r, s.getX(), s.getY());
            return d / s.getN();
        }

        @Override
        public Rectangle2D sizeAt(Square s, double at) {
            int n = s.getN();
            return new Rectangle2D.Double(
                    s.getX() - at * n,
                    s.getY() - at * n,
                    2 * at * n, 2 * at * n
                );
        }

}
