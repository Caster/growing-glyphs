package algorithm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.Glyph;
import datastructure.Utils;
import datastructure.events.Event;
import datastructure.events.OutOfCell;
import datastructure.events.OutOfCell.Side;
import datastructure.growfunction.GrowFunction;
import datastructure.events.GlyphMerge;

public class AgglomerativeClustering {

    /**
     * Tree with {@link Glyph glyphs} that need clustering.
     */
    private QuadTree tree;
    private GrowFunction g;
    /**
     * Resulting clustering.
     */
    private HierarchicalClustering result;


    /**
     * Initialize algorithm for clustering growing glyphs on the given QuadTree.
     *
     * @param tree Tree with glyphs to be clustered.
     * @param g Function to use for determining the size of glyphs.
     */
    public AgglomerativeClustering(QuadTree tree, GrowFunction g) {
        this.tree = tree;
        this.g = g;
        this.result = null;
    }

    /**
     * Returns the result of the algorithm. Initially {@code null}.
     *
     * @see #cluster()
     */
    public  HierarchicalClustering getClustering() {
        return result;
    }


    /**
     * Run clustering algorithm on the QuadTree provided at construction time.
     *
     * @param multiMerge Whether merges resulting in immediate overlap should be
     *            merged immediately, thus not showing up in the resulting
     *            {@link HierarchicalClustering}, or not.
     * @param includeOutOfCell Whether events caused by a glyph growing out of
     *            a cell should be included in the resulting clustering.
     * @return A reference to the clustering instance, for chaining.
     */
    public AgglomerativeClustering cluster(boolean multiMerge,
            boolean includeOutOfCell) {
        // construct a queue, put everything in there
        PriorityQueue<Event> q = new PriorityQueue<>();
        // also create a result for each glyph, and a map to find them
        Map<Glyph, HierarchicalClustering> map = new HashMap<>();
        // finally, create an indication of which glyphs still participate
        Set<Glyph> alive = new HashSet<>();
        for (QuadTree leaf : tree.leaves()) {
            Glyph[] glyphs = leaf.getGlyphs().toArray(new Glyph[0]);
            for (int i = 0; i < glyphs.length; ++i) {
                // add events for when two glyphs in the same cell touch
                for (int j = i + 1; j < glyphs.length; ++j) {
                    q.add(new GlyphMerge(glyphs[i], glyphs[j], g));
                }

                // add events for when a glyph grows out of its cell
                for (Side side : Side.values()) {
                    q.add(new OutOfCell(glyphs[i], g, leaf, side));
                }

                // create clustering leaves for all glyphs, mark them as alive
                map.put(glyphs[i], new HierarchicalClustering(glyphs[i], 0));
                alive.add(glyphs[i]);
            }
        }
        // merge glyphs until no pairs to merge remain
        queue: while (!q.isEmpty() && alive.size() > 1) {
            Event e = q.poll();
            // we ignore this event if not all glyphs from it are alive anymore
            for (Glyph glyph : e.getGlyphs()) {
                if (!alive.contains(glyph)) {
                    continue queue;
                }
            }
            // depending on the type of event, handle it appropriately
            switch (e.getType()) {
            case MERGE:
                GlyphMerge m = (GlyphMerge) e;
                double mergedAt = m.getAt();
                // create a merged glyph
                Glyph merged = new Glyph(m.getGlyphs());
                HierarchicalClustering mergedHC = new HierarchicalClustering(merged,
                        mergedAt, Utils.map(m.getGlyphs(), map,
                                new HierarchicalClustering[m.getSize()]));
                // add new glyph to QuadTree cell(s)
                tree.insert(merged, mergedAt, g);
                // mark merged glyphs as dead
                for (Glyph glyph : m.getGlyphs()) {
                    alive.remove(glyph);
                }
                // create events with remaining glyphs
                for (QuadTree cell : merged.getCells()) {
                    for (Glyph glyph : cell.getGlyphs()) {
                        if (alive.contains(glyph)) {
                            q.add(new GlyphMerge(merged, glyph, g));
                        }
                    }
                }
                // update bookkeeping
                alive.add(merged);
                map.put(merged, mergedHC);
                // eventually, the last merged glyph is the root
                result = mergedHC;
                if (!multiMerge) {
                    continue queue;
                }
                // keep merging while next glyph overlaps before current event
                Event next = q.peek();
                Set<Glyph> inMerged = null; // keep track of glyphs that are merged
                Set<Glyph> wasMerged = null; // keep track of intermediate merges
                if (next != null) {
                    inMerged = new HashSet<>(m.getSize() * 2);
                    inMerged.addAll(Arrays.asList(m.getGlyphs()));
                    wasMerged = new HashSet<>(m.getSize());
                }
                merging: while (next != null && next.getAt() < mergedAt &&
                        Utils.indexOf(next.getGlyphs(), merged) >= 0) {
                    e = q.poll();
                    // we ignore this event if not all glyphs from it are alive anymore
                    for (Glyph glyph : e.getGlyphs()) {
                        if (!alive.contains(glyph)) {
                            continue merging;
                        }
                    }
                    switch (e.getType()) {
                    case MERGE:
                        m = (GlyphMerge) e;
                        // previous merged glyph was no good, let it die and update
                        wasMerged.add(merged);
                        alive.remove(merged);
                        for (Glyph s : m.getGlyphs()) {
                            if (!wasMerged.contains(s)) {
                                inMerged.add(s);
                                mergedHC.alsoCreatedFrom(map.get(s));
                                alive.remove(s);
                            }
                        }
                        map.remove(merged); // it's as if this glyph never existed
                        // create updated merged glyph
                        merged = new Glyph(inMerged);
                        mergedHC.setGlyph(merged);
                        // add new glyph to QuadTree cell(s)
                        tree.insert(merged, m.getAt(), g);
                        // create events with remaining glyphs
                        for (QuadTree cell : merged.getCells()) {
                            for (Glyph glyph : cell.getGlyphs()) {
                                if (alive.contains(glyph)) {
                                    q.add(new GlyphMerge(merged, glyph, g));
                                }
                            }
                        }
                        alive.add(merged);
                        map.put(merged, mergedHC);
                        break;
                    case OUT_OF_CELL:
                        // TODO: implement
                        break;
                    }
                    next = q.peek();
                }
                break;
            case OUT_OF_CELL:
                if (includeOutOfCell) {
                    Glyph glyph = e.getGlyphs()[0];
                    HierarchicalClustering hc = new HierarchicalClustering(glyph,
                            e.getAt(), map.get(glyph));
                    map.put(glyph, hc);
                }
                OutOfCell o = (OutOfCell) e;
                for (Side side : o.getSide().opposite().others()) {
                    // TODO add OutOfCell event when at >= o.getAt()
                }
                break;
            }
        }
        return this;
    }

}
