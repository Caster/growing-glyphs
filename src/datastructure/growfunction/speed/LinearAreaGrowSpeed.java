package datastructure.growfunction.speed;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * A grow speed that scales glyphs such that their area grows linear in their
 * weight. The only difference with {@link LinearGrowSpeed} is that the weight
 * is modified to return the square root of the number of entities.
 */
public class LinearAreaGrowSpeed extends LinearGrowSpeed {

    public LinearAreaGrowSpeed(GrowFunction g) {
        super(g);
    }

    @Override
    public double weight(Glyph glyph) {
        return gf.thresholds.getCompression(glyph) * Math.sqrt(glyph.getN());
    }

}
