package datastructure.growfunction;

import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

/**
 * A simple {@link GrowFunction} that scales {@link Glyph squares} linearly.
 * For this function, the zoom level is interpreted as time. The radius of a
 * square at time {@code t} is then defined as {@code t * n}, where {@code n}
 * is the number of entities represented by the square.
 */
public class LinearlyGrowingSquares extends GrowFunction {

        @Override
        public double intersectAt(Glyph a, Glyph b) {
            double d = Utils.chebyshev(a.getX(), a.getY(), b.getX(), b.getY());
            return d / (w(a) + w(b));
        }

        @Override
        public double intersectAt(Rectangle2D r, Glyph s) {
            double d = Utils.chebyshev(r, s.getX(), s.getY());
            return d / w(s);
        }

        @Override
        public Rectangle2D sizeAt(Glyph s, double at) {
            double weight = w(s);
            return new Rectangle2D.Double(
                    s.getX() - at * weight,
                    s.getY() - at * weight,
                    2 * at * weight, 2 * at * weight
                );
        }

}
