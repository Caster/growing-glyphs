package datastructure.events;

import datastructure.Glyph;
import datastructure.events.Event.Type;
import utils.Utils;

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
        if (Utils.Double.neq(from.at, lb)) {
            from = new GlyphMerge(from.glyphs[0], from.glyphs[1], lb);
        }
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
        // ensure that the event concerns the current size of the big glyph(s)
        int bigIndex = -1;
        for (int i = 0; i < glyphs.length; ++i) {
            if (glyphs[i].isBig()) {
                glyphs[i] = glyphs[i].getAdoptivePrimalParent();
                bigIndex = i;
            }
        }

        // recompute lower bound
        // TODO: this assumes linear growth!
        lb = Math.pow(((double) glyphs[bigIndex].getN()) /
                (glyphs[0].getN() + glyphs[1].getN()), 2);
    }

}
