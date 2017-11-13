package datastructure.events;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * @see Type#MERGE
 */
public class GlyphMerge extends Event {

    public GlyphMerge(Glyph a, Glyph b, GrowFunction g) {
        this(a, b, g.intersectAt(a, b));
    }

    public GlyphMerge(Glyph a, Glyph b, double at) {
        super(at, 2);
        this.glyphs[0] = a;
        this.glyphs[1] = b;
    }

    @Override
    public Type getType() {
        return Type.MERGE;
    }

}
