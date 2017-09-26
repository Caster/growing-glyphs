package algorithm.glyphgenerator;

import datastructure.Glyph;

/**
 * Generator that picks the centers of glyphs uniformly at random in the given
 * bounding rectangle. Weights are chosen uniformly at random between 1 and a
 * given {@code maxWeight}, which defaults to {@code 10}.
 */
public class UniformRandom extends GlyphGenerator {

    public UniformRandom() {
        super("Uniformly distributed");
    }

    @Override
    public Glyph next() {
        return new Glyph(
                rand.nextDouble() * rect.getWidth() + rect.getX(),
                rand.nextDouble() * rect.getHeight() + rect.getY(),
                rand.nextInt(WEIGHT_RANGE[1]) + WEIGHT_RANGE[0]
            );
    }

}
