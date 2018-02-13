package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

/**
 * A simple {@link GrowFunction} that scales {@link Glyph circles} linearly.
 * For this function, the zoom level is interpreted as time. The radius of a
 * circles at time {@code t} is then defined as {@code t * n}, where {@code n}
 * is the number of entities represented by the circle.
 */
public class LinearlyGrowingCircles extends GrowFunction {

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        double d = Utils.euclidean(a.getX(), a.getY(), b.getX(), b.getY());
        return d / (w(a) + w(b));
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph c) {
        double d = Utils.euclidean(r, c.getX(), c.getY());
        return d / w(c);
    }

    @Override
    public Shape sizeAt(Glyph g, double at, int c) {
        double r = at * w(g);
        return new Ellipse2D.Double(
                g.getX() - r - 2 * c,
                g.getY() - r - 2 * c,
                2 * r + 4 * c, 2 * r + 4 * c);
    }

}
