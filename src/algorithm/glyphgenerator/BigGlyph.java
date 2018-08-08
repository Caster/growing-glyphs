package algorithm.glyphgenerator;

import datastructure.Glyph;
import utils.Utils;

/**
 * This generator generates a very heavy glyph in the very center, with after
 * that a ring of uniformly distributed glyphs around it. The weight of the big
 * glyph will be a high power of the maximum value in the range given by
 * {@link GlyphGenerator#WEIGHT_RANGE}. All other glyphs will have the minimum
 * weight from the same range assigned to them.
 */
public class BigGlyph extends GlyphGenerator {

    /**
     * Percentage of rectangle size that points need to be away from the big
     * glyph in the center, at least.
     */
    public static final double MIN_DIST = 0.2;
    /**
     * Percentage of rectangle size that points can be away from the big glyph
     * in the center, at most.
     */
    public static final double MAX_DIST = 0.4;


    public BigGlyph() {
        super("Big glyph");
    }

    @Override
    public Glyph next() {
        count();

        // the first glyph returned will be the big glyph
        if (i == 1) {
            return new Glyph(0, 0, (int) Math.pow(WEIGHT_RANGE[1], 8), true);
        }

        // other glyphs are in a ring around the big one
        double x, y, r;
        double size = Math.min(rect.getWidth(), rect.getHeight());
        double minR = MIN_DIST * size;
        double maxR = MAX_DIST * size;
        do {
            x = rand.nextDouble() * rect.getWidth() + rect.getX();
            y = rand.nextDouble() * rect.getHeight() + rect.getY();
            r = Utils.chebyshev(x, y, 0, 0);
        } while (r < minR || maxR < r);
        return new Glyph(x, y, WEIGHT_RANGE[0], true);
    }

}
