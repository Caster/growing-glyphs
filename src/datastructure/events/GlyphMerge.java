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

    public Glyph getOther(Glyph glyph) {
        if (glyphs[0] != glyph && glyphs[1] != glyph) {
            throw new RuntimeException("given glyph must be in this event");
        }
        if (glyphs[0] == glyph) {
            return glyphs[1];
        }
        return glyphs[0];
    }

    @Override
    public Type getType() {
        return Type.MERGE;
    }

}
