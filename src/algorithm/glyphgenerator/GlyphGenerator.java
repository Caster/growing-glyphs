package algorithm.glyphgenerator;

import java.awt.geom.Rectangle2D;
import java.util.Random;

import datastructure.Glyph;
import datastructure.QuadTree;

/**
 * A glyph generator will iteratively generate glyphs according to some scheme.
 */
public abstract class GlyphGenerator {

    /**
     * A stateful glyph generator uses the positions of already placed points.
     */
    public interface Stateful {

        /**
         * Returns the number of glyphs that have been placed so far.
         */
        public int getNumPlaced();

        /**
         * Can be called after calling {@link #init(int, Rectangle2D)}; will consider
         * all glyphs in the given tree to be already placed points.
         *
         * @param tree Tree that contains glyphs.
         * @return Whether the tree should be cleared now. This is used if the glyph
         *         generator replaces the placed glyphs.
         */
        public boolean init(QuadTree tree);

    }


    /**
     * Range for randomly selecting glyph ranges from.
     */
    public static final int[] WEIGHT_RANGE = new int[] {1, 10};

    protected final String name;
    protected final Random rand;

    protected int i;
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
        this.i = 0;
        this.n = n;
        this.rect = rect;
    }

    public abstract Glyph next();


    /**
     * Increment the counter for number of generated glyphs.
     */
    protected final void count() {
        i++;
    }

    protected final int randN() {
        return rand.nextInt(WEIGHT_RANGE[1]) + WEIGHT_RANGE[0];
    }

}
