package algorithm;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.QuadTree.InsertedWhen;
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
     * @param step Whether processing should be paused after every event.
     * @return A reference to the clustering instance, for chaining.
     */
    public AgglomerativeClustering cluster(boolean multiMerge,
            boolean includeOutOfCell, boolean step) {
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
            System.out.println(e.getType() + " at " + e.getAt());
            for (Glyph glyph : e.getGlyphs()) {
                System.out.println("  " + glyph);
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
                    for (QuadTree cell : glyph.getCells()) {
                        cell.removeGlyphIf(glyph, InsertedWhen.BY_ALGORITHM);
                    }
                }
                // create events with remaining glyphs
                for (QuadTree cell : merged.getCells()) {
                    for (Glyph glyph : cell.getGlyphs()) {
                        if (alive.contains(glyph)) {
                            System.out.println("  CREATING MERGE");
                            Event asdf;
                            q.add(asdf = new GlyphMerge(merged, glyph, g));
                            System.out.println("    " + glyph + " at " + asdf.getAt());
                        }
                    }
                    // create out of cell events
                    for (Side side : Side.values()) {
                        // only create an event when at least one neighbor on
                        // this side does not contain the merged glyph yet
                        boolean create = false;
                        Set<QuadTree> neighbors = cell.getNeighbors(side);
                        for (QuadTree neighbor : neighbors) {
                            if (!neighbor.getGlyphs().contains(merged)) {
                                create = true;
                                break;
                            }
                        }
                        if (!create) {
                            continue;
                        }
                        // now, actually create an OUT_OF_CELL event
                        System.out.println("  CREATING OUT_OF_CELL");
                        System.out.println("    cell = " + cell);
                        System.out.println("    side = " + side);
                        System.out.println("    rect = " + cell.getSide(side));
                        Event asdf;
                        q.add(asdf = new OutOfCell(merged, g, cell, side));
                        System.out.println("    at = " + asdf.getAt());
                    }
                }
                // update bookkeeping
                alive.add(merged);
                map.put(merged, mergedHC);
                // eventually, the last merged glyph is the root
                result = mergedHC;
                System.out.println("CHECK FOR MORE MERGES...");
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
                    System.out.println(e.getType() + " at " + e.getAt());
                    for (Glyph glyph : e.getGlyphs()) {
                        System.out.println("  " + glyph);
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
                                // TODO: use multiMerge parameter here
                                mergedHC.alsoCreatedFrom(map.get(s));
                                alive.remove(s);
                            }
                        }
                        map.remove(merged); // it's as if this glyph never existed
                        // create updated merged glyph
                        merged = new Glyph(inMerged);
                        mergedHC.setGlyph(merged); // TODO: use multiMerge parameter here
                        // TODO: update result to new HC node
                        // add new glyph to QuadTree cell(s)
                        tree.insert(merged, m.getAt(), g);
                        // create events with remaining glyphs
                        for (QuadTree cell : merged.getCells()) {
                            for (Glyph glyph : cell.getGlyphs()) {
                                if (alive.contains(glyph)) {
                                    q.add(new GlyphMerge(merged, glyph, g));
                                }
                            }
                            // create out of cell events
                            for (Side side : Side.values()) {
                                // only create an event when at least one neighbor on
                                // this side does not contain the merged glyph yet
                                boolean create = false;
                                Set<QuadTree> neighbors = cell.getNeighbors(side);
                                for (QuadTree neighbor : neighbors) {
                                    if (!neighbor.getGlyphs().contains(merged)) {
                                        create = true;
                                        break;
                                    }
                                }
                                if (!create) {
                                    continue;
                                }
                                // now, actually create an OUT_OF_CELL event
                                q.add(new OutOfCell(merged, g, cell, side));
                            }
                        }
                        alive.add(merged);
                        map.put(merged, mergedHC);
                        break;
                    case OUT_OF_CELL:
                        handleOutOfCell((OutOfCell) e, map, alive,
                                includeOutOfCell, q);
                        break;
                    }
                    next = q.peek();
                    if (step) {
                        try {
                            System.in.read();
                        } catch (IOException e1) {
                            // Well, that's weird. #ShouldNeverHappen #FamousLastWords
                        }
                    }
                }
                System.out.println("...DONE");
                break;
            case OUT_OF_CELL:
                handleOutOfCell((OutOfCell) e, map, alive, includeOutOfCell, q);
                break;
            }
            if (step) {
                try {
                    System.in.read();
                } catch (IOException e1) {
                    // Well, that's weird. #ShouldNeverHappen #FamousLastWords
                }
            }
        }
        return this;
    }


    private void handleOutOfCell(OutOfCell o, Map<Glyph, HierarchicalClustering> map,
            Set<Glyph> alive, boolean includeOutOfCell, PriorityQueue<Event> q) {
        Glyph glyph = o.getGlyphs()[0];
        // possibly include the event
        if (includeOutOfCell) {
            HierarchicalClustering hc = new HierarchicalClustering(glyph,
                    o.getAt(), map.get(glyph));
            map.put(glyph, hc);
        }
        double oAt = o.getAt();
        // create merge events with the glyphs in the neighbors
        Set<QuadTree> neighbors = o.getCell().getNeighbors(o.getSide());
        System.out.println("  growing into");
        for (QuadTree neighbor : neighbors) {
            System.out.println("  " + neighbor);
            if (neighbor.getGlyphs().contains(glyph)) {
                System.out.println("    but was already in there, so ignoring");
                continue;
            }
            for (Glyph otherGlyph : neighbor.getGlyphs()) {
                if (alive.contains(otherGlyph)) {
                    Event asdf;
                    q.add(asdf = new GlyphMerge(glyph, otherGlyph, g));
                    System.out.println("    merge at " + asdf.getAt() + " with " + otherGlyph);
                }
            }
        }
        // register glyph in cell(s) it grows into
        for (QuadTree neighbor : neighbors) {
            if (neighbor.getGlyphs().contains(glyph)) {
                continue;
            }

            neighbor.insert(glyph, oAt, g);

            // create out of cell events for the cells the glyph grows into,
            // but only when they happen after the current event
            for (Side side : o.getSide().opposite().others()) {
                double at = g.exitAt(glyph, neighbor, side);
                if (at >= oAt) {
                    System.out.println("    -> adding " + side + " event at " + at);
                    q.add(new OutOfCell(glyph, neighbor, side, at));
                }
            }
        }
    }

}
