package datastructure.queues;

import java.util.PriorityQueue;

import datastructure.Glyph;
import datastructure.events.GlyphMerge;
import datastructure.events.UncertainGlyphMerge;
import datastructure.growfunction.GrowFunction;
import utils.Utils;

public class UncertainQueue extends PriorityQueue<UncertainGlyphMerge> {

    private double α;
    private final GrowFunction g;


    /**
     * Constructs a new queue to track merge events with the given glyph.
     *
     * @param g GrowFunction that is used to {@linkplain UncertainGlyphMerge#
     *            computeAt(GrowFunction) compute} when events happen.
     */
    public UncertainQueue(GrowFunction g) {
        this.α = 1;
        this.g = g;
    }

    @Override
    public boolean add(UncertainGlyphMerge merge) {
        double t = merge.computeAt(g);
        merge.setLowerBound(t / α);
        return super.add(merge);
    }

    @Override
    public UncertainGlyphMerge peek() {
        while (!isEmpty()) {
            UncertainGlyphMerge merge = super.peek();
            Glyph with = merge.getSmallGlyph();
            if (!with.isAlive()) {
                super.poll();
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
     * Update α to maintain the invariant.
     *
     * @param event Event that caused need for updating α.
     */
    public void updateAlpha(GlyphMerge event) {
        double bigRadius = g.radius(event.getGlyphs()[0], event.getAt()) +
                g.border(event.getGlyphs()[0], event.getAt());
        double smallRadius = g.radius(event.getGlyphs()[1], event.getAt()) +
                g.border(event.getGlyphs()[1], event.getAt());
        if (bigRadius < smallRadius) {
            double tmp = smallRadius;
            smallRadius = bigRadius;
            bigRadius = tmp;
        }

        α = (bigRadius - smallRadius) / (bigRadius + smallRadius) * α;
    }

}
