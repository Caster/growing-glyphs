package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

/**
 * This {@link GrowFunction} scales {@link Glyph circles} logarithmically between
 * having a zero radius, and some set maximum radius. If no maximum is set,
 * regular logarithmic scaling is applied. For this function, the zoom level is
 * interpreted as time. The radius of a circle at time {@code t} is then defined
 * as {@code a * log(t * n)}, where {@code n} is the number of entities represented
 * by the circle, and {@code a} is a constant defined by the maximum glyph radius
 * and the number of entities represented at most by a single glyph. The base 2
 * logarithm is used for calculating the radius in the before.
 */
public class LogarithmicallyGrowingCircles extends GrowFunction {

    private static final double LOG_BASE = 2;
    private static final double LOG_DIV = Math.log(LOG_BASE);


    /**
     * The scale function used by this grow function is `fA * log(t * n)`.
     * Parameter `fA` is calculated from the maximum glyph radius and the
     * number of entities represented by such a glyph.
     */
    private double fA = 1d;


    @Override
    public void initialize(int numGlyphs, double maxRadius) {
        fA = maxRadius / (Math.log(numGlyphs) / LOG_DIV);
    }

    @Override
    public double intersectAt(Glyph cA, Glyph cB) {
        double d = Utils.euclidean(cA.getX(), cA.getY(), cB.getX(), cB.getY());
        // we want that `log(1 + t * w_a) + log(1 + t * w_b) = d / fA`, which
        // translates to the below equation according to WolframAlpha
        double a = w(cA);
        double b = w(cB);
        return (Math.sqrt(a * a + 4 * a * b * Math.pow(LOG_BASE, d / fA) -
                2 * a * b + b * b) - a - b) / (2 * a * b);
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph c) {
        double d = Utils.euclidean(r, c.getX(), c.getY());
        // we want that `log(1 + t * w) = d / fA`, which translates to
        // `t = (base^(d / fA) - 1) / w`, which is used below
        return (Math.pow(LOG_BASE, d / fA) - 1) / w(c);
    }

    @Override
    public Shape sizeAt(Glyph c, double at) {
        double r = (Math.log1p(at * w(c)) / LOG_DIV) * fA;
        return new Ellipse2D.Double(
                c.getX() - r,
                c.getY() - r,
                2 * r, 2 * r);
    }

}
