package datastructure.events;

import datastructure.Glyph;
import datastructure.events.Event.Type;

public class UncertainGlyphMerge extends UncertainEvent {

    protected GlyphMerge from;


    public UncertainGlyphMerge(GlyphMerge m) {
        super(m.at, m.glyphs.length);
        for (int i = 0; i < m.glyphs.length; ++i) {
            this.glyphs[i] = m.glyphs[i];
        }
        this.from = m;
    }

    public GlyphMerge getGlyphMerge() {
        return from;
    }

    public Glyph getOther(Glyph glyph) {
        return from.getOther(glyph);
    }

    @Override
    public Type getType() {
        return Type.MERGE;
    }


    @Override
    protected void recomputeLowerBoundInternal() {
        // TODO Auto-generated method stub

    }

}
