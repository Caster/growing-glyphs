package algorithm.glyphgenerator;

import datastructure.Glyph;
import utils.Utils;

/**
 * This generator generates a number of very heavy glyphs evenly spaced out on a
 * circle, with after that a ring of uniformly distributed glyphs around each of
 * them. The weight of the big glyphs will be a high power of the maximum value
 * in the range given by {@link GlyphGenerator#WEIGHT_RANGE}. All other glyphs
 * will have the minimum weight from the same range assigned to them.
 */
public class SomeBigGlyphs extends GlyphGenerator {

    /**
     * Percentage of rectangle size that points need to be away from the big
     * glyph in the center, at least.
     */
    public static final double MIN_DIST = 0.05;
    /**
     * Percentage of rectangle size that points can be away from the big glyph
     * in the center, at most.
     */
    public static final double MAX_DIST = 0.2;
    /**
     * Percentage of the rectangle size that is the radius of the circle along
     * which the big glyphs are positioned.
     */
    public static final double RADIUS = 0.3;


    public final int numBigGlyphs;


    /**
     * Construct a glyph generator that will generate the given number of big
     * glyphs before outputting small glyphs around them.
     *
     * @param numBigGlyphs Number of big glyphs to generate.
     */
    public SomeBigGlyphs(int numBigGlyphs) {
        super("Some big glyphs");

        this.numBigGlyphs = numBigGlyphs;
    }

    @Override
    public Glyph next() {
        count();

        // the first glyph returned will be the big glyph
        double size = Math.min(rect.getWidth(), rect.getHeight());
        double r = RADIUS * size;
        if (i <= numBigGlyphs) {
            return new Glyph(
                    Math.cos(2 * Math.PI / numBigGlyphs * i) * r,
                    Math.sin(2 * Math.PI / numBigGlyphs * i) * r,
                    (int) Math.pow(WEIGHT_RANGE[1], 8), true
                );
        }

        // other glyphs are in a ring around the big one
        double x, y, d;
        double minR = MIN_DIST * size;
        double maxR = MAX_DIST * size;
        int around = rand.nextInt(numBigGlyphs);
        double offsetX = Math.cos(2 * Math.PI / numBigGlyphs * around) * r;
        double offsetY = Math.sin(2 * Math.PI / numBigGlyphs * around) * r;
        do {
            x = offsetX + rand.nextDouble() * rect.getWidth() + rect.getX();
            y = offsetY + rand.nextDouble() * rect.getHeight() + rect.getY();
            d = Utils.chebyshev(x, y, offsetX, offsetY);
        } while (d < minR || maxR < d);
        return new Glyph(x, y, WEIGHT_RANGE[0], true);
    }

}
