package datastructure.events;

import datastructure.Glyph;
import datastructure.events.Event.Type;
import datastructure.growfunction.GrowFunction;
import utils.Utils;

public class UncertainGlyphMerge extends UncertainEvent {

    /**
     * Original event that the uncertain variant was constructed from.
     */
    protected GlyphMerge from;
    /**
     * Computed (and updated) actual timestamp/zoom level of merge event.
     */
    protected double at;


    public UncertainGlyphMerge(GlyphMerge m) {
        super(m.at, m.glyphs.length);
        for (int i = 0; i < m.glyphs.length; ++i) {
            this.glyphs[i] = m.glyphs[i];
        }
        this.from = m;
        this.at = m.at;
    }

    /**
     * Recompute when this event will happen, but only if the big glyph changed.
     * Otherwise, a cached result is returned immediately.
     *
     * @param g Used to determine when the two glyphs will intersect.
     */
    public double computeAt(GrowFunction g) {
        // check if the cached answer still holds
        boolean changed = false;
        for (int i = 0; i < glyphs.length; ++i) {
            if (glyphs[i].isBig()) {
                Glyph prev = glyphs[i];
                glyphs[i] = glyphs[i].getAdoptivePrimalParent();
                if (glyphs[i] != prev) {
                    changed = true;
                    from.glyphs[i] = glyphs[i];
                }
            }
        }
        // recompute, but only if needed
        if (changed) {
            at = g.intersectAt(glyphs[0], glyphs[1]);
        }
        return at;
    }

    public double getAt() {
        return at;
    }

    public GlyphMerge getGlyphMerge() {
        if (Utils.Double.neq(from.at, at)) {
            from = new GlyphMerge(from.glyphs[0], from.glyphs[1], at);
        }
        return from;
    }

    public Glyph getSmallGlyph() {
        if (glyphs[0].isBig()) {
            return glyphs[1];
        }
        return glyphs[0];
    }

    @Override
    public Type getType() {
        return Type.MERGE;
    }

    /**
     * Update the lower bound of this event.
     *
     * @param lowerBound New lower bound.
     */
    public void setLowerBound(double lowerBound) {
        this.lb = lowerBound;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("uncertain ");
        sb.append(getType().toString());
        sb.append(" at ");
        sb.append(at);
        sb.append(", lower bound ");
        sb.append(lb);
        sb.append(" involving [");
        boolean first = true;
        for (Glyph glyph : glyphs) {
            if (!first) {
                sb.append(", ");
            }
            if (glyph == null) {
                sb.append("null");
            } else {
                sb.append(glyph.toString());
            }
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

}
