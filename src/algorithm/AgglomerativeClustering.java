package algorithm;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.QuadTree.InsertedWhen;
import datastructure.Glyph;
import datastructure.Utils;
import datastructure.events.Event;
import datastructure.events.Event.Type;
import datastructure.events.OutOfCell;
import datastructure.events.OutOfCell.Side;
import datastructure.growfunction.GrowFunction;
import datastructure.events.GlyphMerge;

public class AgglomerativeClustering {

    private static final Logger LOGGER =
            Logger.getLogger(AgglomerativeClustering.class.getName());


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
        LOGGER.log(Level.FINE, "ENTRY into AgglomerativeClustering#cluster()");
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
        LOGGER.log(Level.FINE, "created {0} events initially, for {1} glyphs",
                new Object[] {q.size(), alive.size()});
        // merge glyphs until no pairs to merge remain
        queue: while (!q.isEmpty() && alive.size() > 1) {
            Event e = q.poll();
            // we ignore this event if not all glyphs from it are alive anymore
            for (Glyph glyph : e.getGlyphs()) {
                if (!alive.contains(glyph)) {
                    continue queue;
                }
            }
            LOGGER.log(Level.FINE, "handling {0} at {1} involving",
                    new Object[] {e.getType(), e.getAt()});
            for (Glyph glyph : e.getGlyphs()) {
                LOGGER.log(Level.FINER, "{0}", glyph);
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
                            LOGGER.log(Level.FINER, "CREATING MERGE");
                            Event gme;
                            q.add(gme = new GlyphMerge(merged, glyph, g));
                            LOGGER.log(Level.FINEST, "{0} at {1}", new Object[] {glyph, gme.getAt()});
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
                        LOGGER.log(Level.FINER, "CREATING OUT_OF_CELL");
                        LOGGER.log(Level.FINEST, "cell = {0}", cell);
                        LOGGER.log(Level.FINEST, "side = {0}", side);
                        LOGGER.log(Level.FINEST, "rect = {0}", cell.getSide(side));
                        Event ooe;
                        q.add(ooe = new OutOfCell(merged, g, cell, side));
                        LOGGER.log(Level.FINEST, "at = {0}", ooe.getAt());
                    }
                }
                // update bookkeeping
                alive.add(merged);
                map.put(merged, mergedHC);
                // eventually, the last merged glyph is the root
                result = mergedHC;
                LOGGER.log(Level.FINE, "CHECK FOR MORE MERGES...");
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
                    LOGGER.log(Level.FINER, "handling {0} at {1} involving",
                            new Object[] {e.getType(), e.getAt()});
                    for (Glyph glyph : e.getGlyphs()) {
                        LOGGER.log(Level.FINEST, "{0}", glyph);
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
                LOGGER.log(Level.FINE, "...DONE");
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
        LOGGER.log(Level.FINE, "RETURN from AgglomerativeClustering#cluster()");
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
        LOGGER.log(Level.FINER, "growing into");
        for (QuadTree neighbor : neighbors) {
            LOGGER.log(Level.FINEST, "{0}", neighbor);
            if (neighbor.getGlyphs().contains(glyph)) {
                LOGGER.log(Level.FINEST, "-> but was already in there, so ignoring");
                continue;
            }
            for (Glyph otherGlyph : neighbor.getGlyphs()) {
                if (alive.contains(otherGlyph)) {
                    Event gme;
                    q.add(gme = new GlyphMerge(glyph, otherGlyph, g));
                    LOGGER.log(Level.FINEST, "merge at {0} with {1}",
                            new Object[] {gme.getAt(), otherGlyph});
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
                    LOGGER.log(Level.FINEST, "-> adding {0} {1} event at {2}",
                            new Object[] {side, Type.OUT_OF_CELL, at});
                    q.add(new OutOfCell(glyph, neighbor, side, at));
                }
            }
        }
    }

}
