package algorithm.glyphgenerator;

import algorithm.PerlinNoise;
import datastructure.Glyph;

/**
 * Generator that picks the centers of glyphs using Perlin noise in the given
 * bounding rectangle. Weights are chosen uniformly at random between 1 and a
 * given {@code maxWeight}, which defaults to {@code 10}.
 */
public class Perlin extends GlyphGenerator {

    /**
     * Maximum value for all coordinates of points to be fed into
     * {@link PerlinNoise#noise(double, double, double)}.
     */
    private static final double MAX = 256d;


    public Perlin() {
        super("Perlin noise");
    }

    @Override
    public Glyph next() {
        while (true) {
            double x = rand.nextDouble() * MAX;
            double y = rand.nextDouble() * MAX;
            if (Math.abs(PerlinNoise.noise(x, y, 0)) > 0.75) {
                return new Glyph(
                        x / MAX * rect.getWidth() + rect.getX(),
                        y / MAX * rect.getHeight() + rect.getY(),
                        rand.nextInt(WEIGHT_RANGE[1]) + WEIGHT_RANGE[0]
                    );
            }
        }
    }

}
