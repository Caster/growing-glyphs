package datastructure.events;

import datastructure.Glyph;
import datastructure.events.Event.Type;
import utils.Utils;

public abstract class UncertainEvent implements Comparable<UncertainEvent> {

    /**
     * Timestamp/zoom level at which the event occurs at the earliest.
     */
    protected double lb;
    /**
     * Glyph(s) involved in the event.
     */
    protected final Glyph[] glyphs;

    public UncertainEvent(double lowerBound, int glyphCapacity) {
        this.lb = lowerBound;
        this.glyphs = new Glyph[glyphCapacity];
    }

    @Override
    public int compareTo(UncertainEvent that) {
        int diff = (int) Math.signum(this.lb - that.lb);
        if (diff != 0) {
            return diff;
        }
        return this.getType().priority - that.getType().priority;
    }

    /**
     * Returns when the event occurs at the earliest. This can be interpreted
     * either as a timestamp or as a zoom level.
     */
    public double getLowerBound() {
        return lb;
    }

    /**
     * Returns the number of glyphs involved in this event.
     */
    public int getSize() {
        return glyphs.length;
    }

    /**
     * Returns the glyph(s) involved in this event.
     */
    public Glyph[] getGlyphs() {
        return glyphs;
    }

    /**
     * Returns the {@link Type} of this {@link UncertainEvent}.
     */
    public abstract Type getType();

    /**
     * Update the lower bound on the event time, returns whether the lower bound
     * changed because of this.
     */
    public boolean recomputeLowerBound() {
        double oldLB = lb;
        recomputeLowerBoundInternal();
        return Utils.Double.neq(oldLB, lb);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("uncertain ");
        sb.append(getType().toString());
        sb.append(" at (lower bound) ");
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


    /**
     * Called by {@link #recomputeLowerBound()} to do the actual work.
     */
    protected abstract void recomputeLowerBoundInternal();

}
