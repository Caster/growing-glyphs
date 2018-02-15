package datastructure.growfunction.speed;

import datastructure.growfunction.GrowFunction;

/**
 * A grow speed that scales glyphs logarithmically, using the base 2
 * logarithm. The distance function is to be defined by extending classes,
 * which will in turn define the shape of glyphs.
 */
public class BoundedLogarithmicGrowSpeed extends LogarithmicGrowSpeed {

    public BoundedLogarithmicGrowSpeed(GrowFunction g) {
        super(g);
    }

    @Override
    public void initialize(int numGlyphs, double maxRadius) {
        fA = maxRadius / (Math.log(numGlyphs) / LOG_DIV);
    }

}
