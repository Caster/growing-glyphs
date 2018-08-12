package datastructure.queues;

import java.util.PriorityQueue;

import datastructure.Glyph;
import datastructure.events.GlyphMerge;
import datastructure.events.UncertainGlyphMerge;
import datastructure.growfunction.GrowFunction;
import utils.Utils;

public class UncertainQueue extends PriorityQueue<UncertainGlyphMerge> {

    private double α;
    private Glyph bigGlyph;
    private final GrowFunction g;


    /**
     * Constructs a new queue to track merge events with the given glyph.
     *
     * @param bigGlyph The {@linkplain Glyph#isBig() big} glyph of which merge
     *            events will be tracked by this queue.
     * @param g GrowFunction that is used to {@linkplain UncertainGlyphMerge#
     *            computeAt(GrowFunction) compute} when events happen.
     */
    public UncertainQueue(Glyph bigGlyph, GrowFunction g) {
        if (!bigGlyph.isBig()) {
            throw new IllegalArgumentException("only big glyphs have queues");
        }

        this.α = 1;
        this.bigGlyph = bigGlyph;
        this.g = g;
    }

    @Override
    public UncertainGlyphMerge peek() {
        while (!isEmpty()) {
            UncertainGlyphMerge merge = super.peek();
            Glyph with = merge.getOther(bigGlyph);
            if (!with.isAlive()) {
                continue; // try the next event
            }

            // check if event is the first
            double t = merge.computeAt(g);
            double τ = merge.getLowerBound();
            if (Utils.Double.eq(t, α * τ)) {
                return merge;
            }

            // if not, update its key and reinsert
            super.poll();
            merge.setLowerBound(t / α);
            super.add(merge);
        }
        return null;
    }

    @Override
    public UncertainGlyphMerge poll() {
        // ensure that the actual first event is the head of the queue
        if (peek() == null) {
            return null;
        }
        // return it if there is one
        return super.poll();
    }

    /**
     * Change the glyph of which events are tracked. Useful when a merge occurs
     * and the glyph object changes, even though the conceptual glyph doesn't.
     *
     * @param bigGlyph The {@linkplain Glyph#isBig() big} glyph of which merge
     *            events will be tracked by this queue.
     */
    public void setBigGlyph(Glyph bigGlyph) {
        if (!bigGlyph.isBig()) {
            throw new IllegalArgumentException("passed glyph isn't big");
        }

        this.bigGlyph = bigGlyph;
    }

    /**
     * Update α to maintain the invariant.
     *
     * @param event Event that caused need for updating α.
     */
    public void updateAlpha(GlyphMerge event) {
        double bigWeight = Double.POSITIVE_INFINITY;
        double smallWeight = Double.POSITIVE_INFINITY;
        for (Glyph glyph : event.getGlyphs()) {
            if (glyph.isBig()) {
                bigWeight = g.weight(glyph);
            } else {
                smallWeight = g.weight(glyph);
            }
        }

        if (Double.isInfinite(bigWeight) || Double.isInfinite(smallWeight)) {
            throw new RuntimeException("merge event without small and big glyph");
        }

        α = (bigWeight - smallWeight) / (bigWeight + smallWeight) * α;
    }

}
