package algorithm.glyphgenerator;

import datastructure.Glyph;

/**
 * This generator generates a very heavy glyph in the very center, with after
 * that a ring of uniformly distributed glyphs around it. The weight of the big
 * glyph will be the fourth power of the maximum value in the range given by
 * {@link GlyphGenerator#WEIGHT_RANGE}.
 */
public class BigGlyph extends GlyphGenerator {

    /**
     * Percentage of rectangle size that points need to be away from the big
     * glyph in the center, at least.
     */
    public static final double MIN_DIST_SQ = 0.2;
    /**
     * Percentage of rectangle size that points can be away from the big glyph
     * in the center, at most.
     */
    public static final double MAX_DIST_SQ = 0.4;


    public BigGlyph() {
        super("Big glyph");
    }

    @Override
    public Glyph next() {
        count();

        // the first glyph returned will be the big glyph
        if (i == 1) {
            return new Glyph(0, 0, WEIGHT_RANGE[1] * WEIGHT_RANGE[1] *
                    WEIGHT_RANGE[1] * WEIGHT_RANGE[1], true);
        }

        // other glyphs are in a ring around the big one
        double x, y, r;
        double size = Math.min(rect.getWidth(), rect.getHeight());
        double minR_SQ = (MIN_DIST_SQ * size) * (MIN_DIST_SQ * size);
        double maxR_SQ = (MAX_DIST_SQ * size) * (MAX_DIST_SQ * size);
        do {
            x = rand.nextDouble() * rect.getWidth() + rect.getX();
            y = rand.nextDouble() * rect.getHeight() + rect.getY();
            r = x * x + y * y;
        } while (r < minR_SQ || maxR_SQ < r);
        return new Glyph(x, y, randN(), true);
    }

}
