package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;

/**
 * A grow function that scales glyphs logarithmically, using the base 2
 * logarithm. The distance function is to be defined by extending classes,
 * which will in turn define the shape of glyphs.
 */
public abstract class LogarithmicGrowFunction extends GrowFunction {

    protected final double LOG_BASE = 2;
    protected final double LOG_DIV = Math.log(LOG_BASE);


    /**
     * The scale function used by this grow function is `fA * log(t * n)`.
     * The value of the constant defaults to 1, but can be changed by extending
     * classes. This is done in particular by bounded grow functions.
     */
    protected double fA = 1d;


    @Override
    public double intersectAt(Glyph cA, Glyph cB) {
        double d = dist(cA.getX(), cA.getY(), cB.getX(), cB.getY());
        // we want that `log(1 + t * w_a) + log(1 + t * w_b) = d / fA`, which
        // translates to the below equation according to WolframAlpha
        double a = w(cA);
        double b = w(cB);
        return (Math.sqrt(a * a + 4 * a * b * Math.pow(LOG_BASE, d / fA) -
                2 * a * b + b * b) - a - b) / (2 * a * b);
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph c) {
        double d = dist(r, c.getX(), c.getY());
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


    /**
     * Returns the distance between two points {@code p} and {@code q}.
     */
    protected abstract double dist(double px, double py, double qx, double qy);

    /**
     * Returns the minimum distance between a point and any point in the given
     * rectangle. This will in particular return 0 when the given point is
     * contained in the rectangle.
     */
    protected abstract double dist(Rectangle2D rect, double px, double py);

}
