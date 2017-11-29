package datastructure.growfunction;

import datastructure.Glyph;

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
public class LogarithmicallyGrowingCirclesBounded extends LogarithmicallyGrowingCircles {

    public LogarithmicallyGrowingCirclesBounded() {
        super();
        this.name = "Logarithmically Growing Circles (Bounded)";
    }

    @Override
    public void initialize(int numGlyphs, double maxRadius) {
        fA = maxRadius / (Math.log(numGlyphs) / LOG_DIV);
    }

}
