package algorithm.glyphgenerator;

import java.awt.geom.Rectangle2D;
import java.util.Random;

import datastructure.Glyph;

public abstract class GlyphGenerator {

    /**
     * Range for randomly selecting glyph ranges from.
     */
    public static final int[] WEIGHT_RANGE = new int[] {1, 10};

    protected final String name;
    protected final Random rand;

    protected int n;
    protected Rectangle2D rect;


    public GlyphGenerator(String name) {
        this.name = name;
        this.rand = new Random();
    }

    public String getName() {
        return name;
    }

    public void init(int n, Rectangle2D rect) {
        this.n = n;
        this.rect = rect;
    }

    public abstract Glyph next();

}
