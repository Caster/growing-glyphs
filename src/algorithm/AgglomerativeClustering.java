package algorithm;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.Glyph;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.QuadTree.InsertedWhen;
import datastructure.Utils;
import datastructure.events.Event;
import datastructure.events.Event.Type;
import datastructure.events.GlyphMerge;
import datastructure.events.OutOfCell;
import datastructure.events.OutOfCell.Side;
import datastructure.growfunction.GrowFunction;

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
     * Returns the latest result of executing the clustering algorithm. Initially
     * {@code null}.
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
        long time = System.currentTimeMillis();
        // construct a queue, put everything in there
        Q q = new Q();
        // also create a result for each glyph, and a map to find them
        Map<Glyph, HierarchicalClustering> map = new HashMap<>();
        // finally, create an indication of which glyphs still participate
        int numAlive = 0;
        Rectangle2D rect = tree.getRectangle();
        for (QuadTree leaf : tree.leaves()) {
            Glyph[] glyphs = leaf.getGlyphs().toArray(new Glyph[0]);
            for (int i = 0; i < glyphs.length; ++i) {
                // add events for when two glyphs in the same cell touch
                for (int j = i + 1; j < glyphs.length; ++j) {
                    q.add(new GlyphMerge(glyphs[i], glyphs[j], g));
                }

                // add events for when a glyph grows out of its cell
                for (Side side : Side.values()) {
                    // only create an event when it is not a border of the root
                    if (!Utils.onBorderOf(leaf.getSide(side), rect)) {
                        // now, actually create an OUT_OF_CELL event
                        q.add(new OutOfCell(glyphs[i], g, leaf, side));
                    }
                }

                // create clustering leaves for all glyphs, mark them as alive
                map.put(glyphs[i], new HierarchicalClustering(glyphs[i], 0));
                glyphs[i].alive = true; numAlive++;
            }
        }
        LOGGER.log(Level.FINE, "created {0} events initially, for {1} glyphs",
                new Object[] {q.size(), numAlive});
        // merge glyphs until no pairs to merge remain
        queue: while (!q.isEmpty() && numAlive > 1) {
            Event e = q.peek();
            // we ignore this event if not all glyphs from it are alive anymore
            for (Glyph glyph : e.getGlyphs()) {
                if (!glyph.alive) {
                    q.discard();
                    continue queue;
                }
            }
            e = q.poll();
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
                    glyph.alive = false; numAlive--;
                    for (QuadTree cell : glyph.getCells()) {
                        cell.removeGlyphIf(glyph, InsertedWhen.BY_ALGORITHM);
                    }
                }
                // create events with remaining glyphs
                for (QuadTree cell : merged.getCells()) {
                    for (Glyph glyph : cell.getGlyphs()) {
                        if (glyph.alive) {
                            Event gme;
                            q.add(gme = new GlyphMerge(merged, glyph, g));
                            LOGGER.log(Level.FINEST, "-> merge at {0} with {1}",
                                    new Object[] {gme.getAt(), glyph});
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
                        Event ooe;
                        q.add(ooe = new OutOfCell(merged, g, cell, side));
                        LOGGER.log(Level.FINEST, "-> out of {0} of {2} at {1}",
                                new Object[] {side, ooe.getAt(), cell});
                    }
                }
                // update bookkeeping
                merged.alive = true; numAlive++;
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
                merging: while (next != null && next.getAt() < mergedAt) {
                    e = q.peek();
                    // we ignore this event if not all glyphs from it are alive anymore
                    for (Glyph glyph : e.getGlyphs()) {
                        if (!glyph.alive) {
                            q.discard();
                            step(step);
                            next = q.peek();
                            continue merging;
                        }
                    }
                    // we stop the nested merging if it does not involve our glyph
                    if (e.getType() == Type.MERGE &&
                            Utils.indexOf(next.getGlyphs(), merged) < 0) {
                        break;
                    }
                    // otherwise, handle the nested merge, or OUT_OF_CELL
                    e = q.poll();
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
                        merged.alive = false; numAlive--;
                        for (Glyph s : m.getGlyphs()) {
                            if (!wasMerged.contains(s)) {
                                inMerged.add(s);
                                // TODO: use multiMerge parameter here
                                mergedHC.alsoCreatedFrom(map.get(s));
                                s.alive = false; numAlive--;
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
                                if (glyph.alive) {
                                    Event gme;
                                    q.add(gme = new GlyphMerge(merged, glyph, g));
                                    LOGGER.log(Level.FINEST, "-> merge at {0} with {1}",
                                            new Object[] {gme.getAt(), glyph});
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
                                Event ooe;
                                q.add(ooe = new OutOfCell(merged, g, cell, side));
                                LOGGER.log(Level.FINEST, "-> out of {0} of {2} at {1}",
                                        new Object[] {side, ooe.getAt(), cell});
                            }
                        }
                        merged.alive = true; numAlive++;
                        map.put(merged, mergedHC);
                        break;
                    case OUT_OF_CELL:
                        handleOutOfCell((OutOfCell) e, map,
                                includeOutOfCell, q);
                        break;
                    }
                    step(step);
                    next = q.peek();
                }
                LOGGER.log(Level.FINE, "...DONE");
                break;
            case OUT_OF_CELL:
                handleOutOfCell((OutOfCell) e, map, includeOutOfCell, q);
                break;
            }
            step(step);
        }
        LOGGER.log(Level.FINE, "RETURN from AgglomerativeClustering#cluster()");
        LOGGER.log(Level.INFO, "created {0} events, handled {1} and discarded {2}; {3} events were never considered",
                new Object[] {q.insertions, q.deletions, q.discarded, q.insertions - q.deletions - q.discarded});
        LOGGER.log(Level.INFO, "took {0} ms (wall clock time)", new Object[] {System.currentTimeMillis() - time});
        return this;
    }


    private void handleOutOfCell(OutOfCell o, Map<Glyph, HierarchicalClustering> map,
            boolean includeOutOfCell, PriorityQueue<Event> q) {
        Glyph glyph = o.getGlyphs()[0];
        // possibly include the event
        if (includeOutOfCell &&
                Utils.Double.neq(map.get(glyph).getAt(), o.getAt())) {
            HierarchicalClustering hc = new HierarchicalClustering(glyph,
                    o.getAt(), map.get(glyph));
            map.put(glyph, hc);
        }
        double oAt = o.getAt();
        Side oppositeSide = o.getSide().opposite();
        // create merge events with the glyphs in the neighbors
        // we take the size of the glyph at that point in time into account
        double[] sideInterval = Side.interval(g.sizeAt(glyph, oAt).getBounds2D(), o.getSide());
        LOGGER.log(Level.FINER, "size at border is {0}", Arrays.toString(sideInterval));
        Set<QuadTree> neighbors = o.getCell().getNeighbors(o.getSide());
        LOGGER.log(Level.FINER, "growing into");
        for (QuadTree neighbor : neighbors) {
            LOGGER.log(Level.FINEST, "{0}", neighbor);
            if (!Utils.intervalsOverlap(Side.interval(
                    neighbor.getSide(oppositeSide), oppositeSide), sideInterval)) {
                LOGGER.log(Level.FINEST, "-> but not at this point in time, so ignoring");
                continue;
            }
            if (neighbor.getGlyphs().contains(glyph)) {
                LOGGER.log(Level.FINEST, "-> but was already in there, so ignoring");
                continue;
            }
            for (Glyph otherGlyph : neighbor.getGlyphs()) {
                if (otherGlyph.alive) {
                    Event gme;
                    q.add(gme = new GlyphMerge(glyph, otherGlyph, g));
                    LOGGER.log(Level.FINEST, "-> merge at {0} with {1}",
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
                    // only create an event when at least one neighbor on
                    // this side does not contain the glyph yet
                    boolean create = false;
                    Set<QuadTree> neighbors2 = neighbor.getNeighbors(side);
                    for (QuadTree neighbor2 : neighbors2) {
                        if (!neighbor2.getGlyphs().contains(glyph)) {
                            create = true;
                            break;
                        }
                    }
                    if (!create) {
                        continue;
                    }
                    // now, actually create an OUT_OF_CELL event
                    LOGGER.log(Level.FINEST, "-> out of {0} of {2} at {1}",
                            new Object[] {side, at, neighbor});
                    q.add(new OutOfCell(glyph, neighbor, side, at));
                }
            }
        }
    }

    /**
     * Executed right before going to the next iteration of the event handling
     * loop. Possibly pauses executiong, depending on parameter.
     *
     * @param step Whether execution should be paused.
     */
    private void step(boolean step) {
        if (step) {
            try {
                System.in.read();
            } catch (IOException e1) {
                // Well, that's weird. #ShouldNeverHappen #FamousLastWords
            }
        }
    }


    /**
     * {@link PriorityQueue} that keeps track of the number of insertions and
     * deletions into/from it. Can be asked for these stats too.
     */
    private static class Q extends PriorityQueue<Event> {

        private int insertions = 0;
        private int deletions = 0;
        private int discarded = 0;

        @Override
        public boolean add(Event e) {
            insertions++;
            return super.add(e);
        }

        public void discard() {
            discarded++;
            super.poll();
        }

        @Override
        public Event poll() {
            deletions++;
            return super.poll();
        }

    }

}
