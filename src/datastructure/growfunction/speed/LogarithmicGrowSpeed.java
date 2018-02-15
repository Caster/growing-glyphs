package datastructure.growfunction.speed;

import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * A grow speed that scales glyphs logarithmically, using the base 2 logarithm.
 */
public class LogarithmicGrowSpeed extends GrowSpeedBase {

    protected final double LOG_BASE = 2;
    protected final double LOG_DIV = Math.log(LOG_BASE);


    /**
     * The scale function used by this grow function is `fA * log(t * n)`.
     * The value of the constant defaults to 1, but can be changed by extending
     * classes. This is done in particular by bounded grow functions.
     */
    protected double fA = 1d;


    public LogarithmicGrowSpeed(GrowFunction g) {
        super(g);
    }

    @Override
    public double intersectAt(Glyph gA, Glyph gB) {
        // we want that `log(1 + t * w_a) + log(1 + t * w_b) = d / fA`, which
        // translates to the below equation according to WolframAlpha
        double a = weight(gA);
        double b = weight(gB);
        return (Math.sqrt(a * a + 4 * a * b *
                Math.pow(LOG_BASE, gf.dist(gA, gB) / fA) -
                2 * a * b + b * b) - a - b) / (2 * a * b);
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph g) {
        // we want that `log(1 + t * w) = d / fA`, which translates to
        // `t = (base^(d / fA) - 1) / w`, which is used below
        return (Math.pow(LOG_BASE, gf.dist(r, g) / fA) - 1) / weight(g);
    }

    @Override
    public double radius(Glyph g, double at) {
        return (Math.log1p(at * weight(g)) / LOG_DIV) * fA;
    }

}
