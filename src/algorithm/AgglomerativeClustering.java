package algorithm;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.Glyph;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.events.Event;
import datastructure.events.Event.Type;
import datastructure.events.GlyphMerge;
import datastructure.events.OutOfCell;
import datastructure.events.OutOfCell.Side;
import datastructure.growfunction.GrowFunction;
import datastructure.queues.BucketingStrategy;
import datastructure.queues.MultiQueue;
import utils.Stat;
import utils.Utils;
import utils.Utils.Stats;
import utils.Utils.Timers;

public class AgglomerativeClustering {

    /**
     * Whether merge events are to be created for all pairs of glyphs, or only
     * the first one. In practice, creating only the first merge event appears
     * to result in clusterings free of overlap, but in theory overlap can
     * occur. Setting this to {@code true} implies a performance hit.
     *
     * Please note that {@link QuadTree#MAX_GLYPHS_PER_CELL} cannot be set to
     * high values when setting this to {@code true}, or you need to allocate
     * more memory to the clustering process for large data sets.
     */
    public static final boolean ROBUST = false;
    /**
     * When {@link #ROBUST} is {@code false}, this flag toggles behavior where
     * glyphs track which glyphs think they'll merge with them first. Merge
     * events are then updated for tracking glyphs, as glyphs merge.
     *
     * Same concern about {@link QuadTree#MAX_GLYPHS_PER_CELL} holds as for
     * {@link #ROBUST}, except for CPU time instead of memory.
     */
    public static final boolean TRACK = true;
    /**
     * Whether the event queue should be split into multiple queues, and when.
     */
    public static final BucketingStrategy QUEUE_BUCKETING =
            BucketingStrategy.NO_BUCKETING;


    private static final Logger LOGGER =
            Logger.getLogger(AgglomerativeClustering.class.getName());


    /**
     * Tree with {@link Glyph glyphs} that need clustering.
     */
    private QuadTree tree;
    private GrowFunction g;
    /**
     * Single object that is used to easily find merge events to be added.
     */
    private FirstMergeRecorder rec;
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
        this.rec = null;
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
     * @param includeOutOfCell Whether events caused by a glyph growing out of
     *            a cell should be included in the resulting clustering.
     * @param step Whether processing should be paused after every event.
     * @return A reference to the clustering instance, for chaining.
     */
    public AgglomerativeClustering cluster(boolean includeOutOfCell,
            boolean step) {
        LOGGER.log(Level.FINER, "ENTRY into AgglomerativeClustering#cluster()");
        LOGGER.log(Level.FINE, "clustering using {0} strategy", Utils.join(" + ",
                (ROBUST ? "ROBUST" : ""), (TRACK ? "TRACK" : ""),
                (!ROBUST && !TRACK ? "FIRST MERGE ONLY" : ""),
                QUEUE_BUCKETING.toString()));
        LOGGER.log(Level.FINE, "QuadTree has {0} nodes and height {1}, having "
                + "at most {2} glyphs per cell and cell size at least {3}",
                new Object[] {tree.getSize(), tree.getTreeHeight(),
                QuadTree.MAX_GLYPHS_PER_CELL, QuadTree.MIN_CELL_SIZE});
        if (LOGGER.isLoggable(Level.FINE)) {
            for (QuadTree leaf : tree.getLeaves()) {
                Stats.record("glyphs per cell", leaf.getGlyphs().size());
            }
        }
        Timers.start("clustering");
        // construct a queue, put everything in there
        MultiQueue q = new MultiQueue(QUEUE_BUCKETING);
        // also create a result for each glyph, and a map to find them
        Map<Glyph, HierarchicalClustering> map = new HashMap<>();
        // then create a single object that is used to find first merges
        rec = new FirstMergeRecorder(g);
        // finally, create an indication of which glyphs still participate
        int numAlive = 0;
        Rectangle2D rect = tree.getRectangle();
        for (QuadTree leaf : tree.getLeaves()) {
            Glyph[] glyphs = leaf.getGlyphs().toArray(new Glyph[0]);
            for (int i = 0; i < glyphs.length; ++i) {
                // add events for when two glyphs in the same cell touch
                rec.from(glyphs[i]);
                Timers.start("first merge recording");
                rec.record(glyphs, i + 1, glyphs.length);
                Timers.stop("first merge recording");
                rec.addEventsTo(q);

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
            // we ignore out of cell events for non-leaf cells
            if (e.getType() == Type.OUT_OF_CELL &&
                    !((OutOfCell) e).getCell().isLeaf()) {
                q.discard();
                continue queue;
            }
            // we ignore this event if not all glyphs from it are alive anymore
            for (Glyph glyph : e.getGlyphs()) {
                if (!glyph.alive) {
                    q.discard();
                    continue queue;
                }
            }
            e = q.poll();
            LOGGER.log(Level.FINER, "handling {0} at {1} involving",
                    new Object[] {e.getType(), e.getAt()});
            for (Glyph glyph : e.getGlyphs()) {
                LOGGER.log(Level.FINER, "{0}", glyph);
            }
            // depending on the type of event, handle it appropriately
            switch (e.getType()) {
            case MERGE:
                Timers.start("merge event processing");
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
                        cell.removeGlyph(glyph);
                    }
                    // update merge events of glyphs that tracked merged glyphs
                    if (TRACK && !ROBUST) {
                        Iterator<Glyph> it = glyph.trackedBy.iterator();
                        while (it.hasNext()) {
                            Glyph orphan = it.next();
                            if (orphan.alive) {
                                rec.from(orphan);
                                for (QuadTree cell : orphan.getCells()) {
                                    rec.record(cell.getGlyphs());
                                }
                                rec.addEventsTo(q, LOGGER);
                            } else {
                                it.remove();
                            }
                        }
                    }
                }
                // create events with remaining glyphs
                rec.from(merged);
                for (QuadTree cell : merged.getCells()) {
                    Timers.start("first merge recording");
                    rec.record(cell.getGlyphs());
                    Timers.stop("first merge recording");
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
                rec.addEventsTo(q, LOGGER);
                // update bookkeeping
                merged.alive = true; numAlive++;
                map.put(merged, mergedHC);
                // eventually, the last merged glyph is the root
                result = mergedHC;
                LOGGER.log(Level.FINER, "CHECK FOR MORE MERGES...");
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
                    // we ignore out of cell events for non-leaf cells
                    if (e.getType() == Type.OUT_OF_CELL &&
                            !((OutOfCell) e).getCell().isLeaf()) {
                        q.discard();
                        step(step);
                        next = q.peek();
                        continue merging;
                    }
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
                    LOGGER.log(Level.FINEST, "handling {0} at {1} involving",
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
                        // at this point, because this is a nested merge, no other
                        // glyphs have had time to track `merged` yet, so no need
                        // to do anything even if `TRACK && !ROBUST`
                        for (QuadTree cell : merged.getCells()) {
                            cell.removeGlyph(merged);
                        }
                        for (Glyph glyph : m.getGlyphs()) {
                            if (!wasMerged.contains(glyph)) {
                                inMerged.add(glyph);
                                mergedHC.alsoCreatedFrom(map.get(glyph));
                                glyph.alive = false; numAlive--;
                                for (QuadTree cell : glyph.getCells()) {
                                    cell.removeGlyph(glyph);
                                }
                                // the remark above about `merged` does not hold for `glyph`,
                                // so update merge events of glyphs that tracked `glyph`
                                if (TRACK && !ROBUST) {
                                    Iterator<Glyph> it = glyph.trackedBy.iterator();
                                    while (it.hasNext()) {
                                        Glyph orphan = it.next();
                                        if (orphan.alive) {
                                            rec.from(orphan);
                                            for (QuadTree cell : orphan.getCells()) {
                                                rec.record(cell.getGlyphs());
                                            }
                                            rec.addEventsTo(q, LOGGER);
                                        } else {
                                            it.remove();
                                        }
                                    }
                                }
                            }
                        }
                        map.remove(merged); // it's as if this glyph never existed
                        // create updated merged glyph
                        merged = new Glyph(inMerged);
                        mergedHC.setGlyph(merged);
                        // add new glyph to QuadTree cell(s)
                        tree.insert(merged, m.getAt(), g);
                        // create events with remaining glyphs
                        rec.from(merged);
                        for (QuadTree cell : merged.getCells()) {
                            Timers.start("first merge recording");
                            rec.record(cell.getGlyphs());
                            Timers.stop("first merge recording");
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
                        rec.addEventsTo(q, LOGGER);
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
                LOGGER.log(Level.FINER, "...DONE");
                Timers.stop("merge event processing");
                break;
            case OUT_OF_CELL:
                Timers.start("out of cell event processing");
                handleOutOfCell((OutOfCell) e, map, includeOutOfCell, q);
                Timers.stop("out of cell event processing");
                break;
            }
            step(step);
        }
        LOGGER.log(Level.FINE, "created {0} events, handled {1} and discarded "
                + "{2}; {3} events were never considered",
                new Object[] {q.getInsertions(), q.getDeletions(),
                q.getDiscarded(), q.getInsertions() - q.getDeletions() -
                q.getDiscarded()});
        for (Event.Type t : Event.Type.values()) {
            String tn = t.toString();
            Stat s = Stats.get(tn);
            LOGGER.log(Level.FINE, "→ {1} {0}s ({2} handled, {3} discarded)", new Object[] {
                tn, s.getSum(), Stats.get(tn + " handled").getSum(),
                Stats.get(tn + " discarded").getSum()});
        }
        LOGGER.log(Level.FINE, "events were stored in {0} queues", q.getNumQueues());
        LOGGER.log(Level.FINE, "QuadTree has {0} nodes and height {1} now",
                new Object[] {tree.getSize(), tree.getTreeHeight()});
        Timers.log("clustering", LOGGER);
        Timers.log("queue operations", LOGGER);
        Timers.log("first merge recording", LOGGER);
        Timers.log("set to array", LOGGER);
        Stats.log("queue size", LOGGER);
        Stats.log("glyphs per cell", LOGGER);
        Timers.log("merge event processing", LOGGER);
        Timers.log("out of cell event processing", LOGGER);
        LOGGER.log(Level.FINER, "RETURN from AgglomerativeClustering#cluster()");
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
        LOGGER.log(Level.FINEST, "growing into");
        for (QuadTree neighbor : neighbors) {
            LOGGER.log(Level.FINEST, "{0}", neighbor);

            // ensure that glyph actually grows into this neighbor
            if (!Utils.intervalsOverlap(Side.interval(
                    neighbor.getSide(oppositeSide), oppositeSide), sideInterval)) {
                LOGGER.log(Level.FINEST, "-> but not at this point in time, so ignoring");
                continue;
            }

            // ensure that glyph was not in this cell yet
            if (neighbor.getGlyphs().contains(glyph)) {
                LOGGER.log(Level.FINEST, "-> but was already in there, so ignoring");
                continue;
            }

            // register glyph in cell(s) it grows into
            neighbor.insert(glyph, oAt, g);

            // split cell if necessary, to maintain maximum glyphs per cell
            Set<QuadTree> grownInto;
            if (neighbor.getGlyphs().size() > QuadTree.MAX_GLYPHS_PER_CELL) {
                // 1. split and move glyphs in cell to appropriate leaf cells
                //    (this may split the cell more than once!)
                neighbor.split(oAt, g);
                // 2. invalidate out of cell events with `neighbor`
                //    → done by discarding such events as they exit the queue
                //      (those events work on non-leaf cells; detectable)
                // 3. invalidate merge events across cell boundaries
                //    → not strictly needed; this may result in having multiple
                //      merge events for the same pair of glyphs, but once the
                //      first one is handled, the others are discarded
                // TODO: check if this makes things more efficient in some form
                // 4. continue with making events in appropriate cells instead
                //    of `neighbor` or all glyphs associated with `neighbor`
                grownInto = neighbor.getLeaves(glyph, oAt, g);
                if (LOGGER.isLoggable(Level.FINE)) {
                    for (QuadTree in : neighbor.getLeaves()) {
                        Stats.record("glyphs per cell",
                                in.getGlyphsAlive().size());
                    }
                }
            } else {
                grownInto = new HashSet<>(1);
                grownInto.add(neighbor);
            }

            rec.from(glyph);
            for (QuadTree in : grownInto) {
                // create merge events with other glyphs in the cells the glyph
                // grows into, even when they happen before the current one
                // (those events will immediately be handled after this one)
                Timers.start("first merge recording");
                rec.record(in.getGlyphs());
                Timers.stop("first merge recording");

                // create out of cell events for the cells the glyph grows into,
                // but only when they happen after the current event
                for (Side side : o.getSide().opposite().others()) {
                    double at = g.exitAt(glyph, in, side);
                    if (at >= oAt) {
                        // only create an event when at least one neighbor on
                        // this side does not contain the glyph yet
                        boolean create = false;
                        Set<QuadTree> neighbors2 = in.getNeighbors(side);
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
                                new Object[] {side, at, in});
                        q.add(new OutOfCell(glyph, in, side, at));
                    }
                }
            }
            rec.addEventsTo(q, LOGGER);
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

}
