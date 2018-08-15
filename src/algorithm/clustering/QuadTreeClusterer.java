package algorithm.clustering;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import algorithm.FirstMergeRecorder;
import datastructure.Glyph;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.events.Event;
import datastructure.events.Event.Type;
import datastructure.events.GlyphMerge;
import datastructure.events.OutOfCell;
import datastructure.events.OutOfCell.Side;
import datastructure.events.UncertainGlyphMerge;
import datastructure.growfunction.GrowFunction;
import datastructure.queues.MultiQueue;
import utils.Constants.B;
import utils.Constants.D;
import utils.Constants.E;
import utils.Constants.I;
import utils.Stat;
import utils.Utils;
import utils.Utils.Stats;
import utils.Utils.Timers;
import utils.Utils.Timers.Units;

public class QuadTreeClusterer extends Clusterer {

    private static final Logger LOGGER = (B.LOGGING_ENABLED.get() ?
            Logger.getLogger(Clusterer.class.getName()) : null);


    /**
     * Single object that is used to easily find merge events to be added.
     */
    private FirstMergeRecorder rec;


    /**
     * {@inheritDoc}
     */
    public QuadTreeClusterer(QuadTree tree) {
        super(tree);
        this.rec = null;
    }


    @Override
    public Clusterer cluster(GrowFunction g,
            boolean includeOutOfCell, boolean step) throws InterruptedException {
        // for debugging only: checking the number of glyphs/entities
        boolean checkTotal = (B.CHECK_NUMBER_REPRESENTED.get() && LOGGER != null);
        int totalGlyphs = 0, totalEntities = 0;
        Set<Glyph> seenGlyphs = null;
        if (checkTotal) {
            seenGlyphs = new HashSet<>();
        }
        Level defaultLevel = (LOGGER == null ? null : LOGGER.getLevel());

        if (LOGGER != null) {
            LOGGER.log(Level.FINER, "ENTRY into AgglomerativeClustering#cluster()");
            LOGGER.log(Level.FINE, "clustering using {0} strategy", Utils.join(" + ",
                    (B.ROBUST.get() ? "ROBUST" : ""),
                    (B.TRACK.get() && !B.ROBUST.get() ? "TRACK" : ""),
                    (!B.ROBUST.get() && !B.TRACK.get() ? "FIRST MERGE ONLY" : ""),
                    E.QUEUE_BUCKETING.get().toString()));
            LOGGER.log(Level.FINE, "using the {0} grow function", g.getName());
            LOGGER.log(Level.FINE, "QuadTree has {0} nodes and height {1}, having "
                    + "at most {2} glyphs per cell and cell size at least {3}",
                    new Object[] {tree.getSize(), tree.getTreeHeight(),
                    I.MAX_GLYPHS_PER_CELL.get(), D.MIN_CELL_SIZE.getString()});
            if (LOGGER.isLoggable(Level.FINE)) {
                int n = 0;
                int c = 0;
                for (QuadTree leaf : tree.getLeaves()) {
                    Stats.record("glyphs per cell", leaf.getGlyphs().size());
                    for (Glyph glyph : leaf.getGlyphs()) {
                        n += glyph.getN();
                        c++;
                        if (checkTotal) {
                            seenGlyphs.add(glyph);
                        }
                    }
                }
                Stats.record("total # works", n);
                totalGlyphs = c;
                totalEntities = n;
            }
        }
        if (B.TIMERS_ENABLED.get()) {
            Timers.start("clustering");
        }
        // construct a queue, put everything in there - 10x number of glyphs
        // appears to be a good estimate for needed capacity without bucketing
        MultiQueue q = new MultiQueue(E.QUEUE_BUCKETING.get(),
                10 * Utils.size(tree.iteratorGlyphsAlive()));
        // also create a result for each glyph, and a map to find them
        Map<Glyph, HierarchicalClustering> map = new HashMap<>();
        // then create a single object that is used to find first merges
        rec = FirstMergeRecorder.getInstance(g);
        // group temporary and shared variables together in one object to reduce
        // the number of parameters to #handleGlyphMerge
        GlobalState state = new GlobalState(map);
        // start recording merge events
        Rectangle2D rect = tree.getRectangle();
        for (QuadTree leaf : tree.getLeaves()) {
            Glyph[] glyphs = leaf.getGlyphs().toArray(new Glyph[0]);
            for (int i = 0; i < glyphs.length; ++i) {
                // add events for when two glyphs in the same cell touch
                LOGGER.log(Level.FINEST, glyphs[i].toString());
                rec.from(glyphs[i]);
                if (B.TIMERS_ENABLED.get())
                    Timers.start("first merge recording 1");
                rec.record(glyphs, i + 1, glyphs.length);
                if (B.TIMERS_ENABLED.get())
                    Timers.stop("first merge recording 1");
                rec.addEventsTo(q, LOGGER);

                // add events for when a glyph grows out of its cell
                for (Side side : Side.values()) {
                    // only create an event when it is not a border of the root
                    if (!Utils.onBorderOf(leaf.getSide(side), rect)) {
                        // now, actually create an OUT_OF_CELL event
                        glyphs[i].record(new OutOfCell(glyphs[i], g, leaf, side));
                    }
                }
                glyphs[i].popOutOfCellInto(q, LOGGER);

                // create clustering leaves for all glyphs, count them as alive
                map.put(glyphs[i], new HierarchicalClustering(glyphs[i], 0));
                state.numAlive++;
                state.glyphSize.record(glyphs[i].getN());
                if (!glyphs[i].isAlive()) {
                    if (LOGGER != null) {
                        LOGGER.log(Level.SEVERE, "unexpected dead glyph in input");
                    }
                    return null;
                }
            }
        }
        if (LOGGER != null)
            LOGGER.log(Level.FINE, "created {0} events initially, for {1} glyphs",
                    new Object[] {q.size(), state.numAlive});
        // merge glyphs until no pairs to merge remain
        Event e;
        while ((e = getNextEvent(q, state)) != null) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            // log on a slightly higher urgency level when one of the glyphs is tracked
            if (LOGGER != null) {
                if (LOGGER.getLevel().intValue() > Level.FINER.intValue()) {
                    for (Glyph glyph : e.getGlyphs()) {
                        if (glyph.track) {
                            LOGGER.setLevel(Level.FINEST);
                            break;
                        }
                    }
                }
                // log about handling this event
                LOGGER.log(Level.FINER, "handling {0} at {1} involving",
                        new Object[] {e.getType(), e.getAt()});
                for (Glyph glyph : e.getGlyphs()) {
                    LOGGER.log(Level.FINER, "{0}", glyph);
                }
            }
            // depending on the type of event, handle it appropriately
            switch (e.getType()) {
            case MERGE:
                // determine whether one of the glyphs is tracked
                boolean track = false;
                for (Glyph glyph : e.getGlyphs()) {
                    track = track || glyph.track;
                }
                // check if one of the glyphs is big; if so, handle separately
                Glyph big = null;
                boolean bigBig = false;
                for (Glyph glyph : e.getGlyphs()) {
                    if (glyph.isBig()) {
                        if (big == null) {
                            big = glyph;
                        } else {
                            // if there is more than one big glyph involved,
                            // handle it normally
                            big = null;
                            bigBig = true;
                            break;
                        }
                    }
                }

                // handle the merge either normally, or as a big glyph merge
                if (big == null) {
                    if (bigBig) {
                        Stats.count("merge big/big");
                    } else {
                        Stats.count("merge small/small");
                    }
                    handleGlyphMerge(g, (GlyphMerge) e, state, q, track);
                } else {
                    Stats.count("merge small/big");
                    handleBigGlyphMerge(g, (GlyphMerge) e, state, q, track);
                }
                break;
            case OUT_OF_CELL:
                if (B.TIMERS_ENABLED.get())
                    Timers.start("out of cell event processing");
                handleOutOfCell(g, (OutOfCell) e, map, includeOutOfCell, q);
                if (B.TIMERS_ENABLED.get())
                    Timers.stop("out of cell event processing");
                break;
            }
            step(step);

            // check ourselves, conditionally
            if (checkTotal) {
                LOGGER.log(Level.SEVERE, "step");
                int n = 0;
                int c = 0;
                Set<Glyph> seenPreviously = new HashSet<>(seenGlyphs);
                Set<Glyph> seenNowGlyphs = new HashSet<>();
                for (QuadTree leaf : tree.getLeaves()) {
                    for (Glyph glyph : leaf.getGlyphsAlive()) {
                        if (!seenNowGlyphs.contains(glyph)) {
                            n += glyph.getN();
                            c++;
                            seenNowGlyphs.add(glyph);
                        }
                    }
                }
                seenGlyphs = new HashSet<>(seenNowGlyphs);
                if (n != totalEntities) {
                    if (n < totalEntities) {
                        LOGGER.log(Level.SEVERE, "Houston, we have a problem "
                                + "... Lost {0} works (had {1} glyphs, have "
                                + "{2} now).", new Object[] {totalEntities - n,
                                        totalGlyphs, c});
                    } else {
                        LOGGER.log(Level.SEVERE, "Houston, we have a problem "
                                + "... Gained {0} works (had {1} glyphs, have "
                                + "{2} now).", new Object[] {n - totalEntities,
                                        totalGlyphs, c});
                    }
                    if (c < totalGlyphs) {
                        seenPreviously.removeAll(seenNowGlyphs);
                        seenNowGlyphs = seenPreviously;
                        LOGGER.log(Level.SEVERE, "The following glyphs are missing:");
                    } else {
                        seenNowGlyphs.removeAll(seenPreviously);
                        LOGGER.log(Level.SEVERE, "The following glyphs are added:");
                    }
                    for (Glyph glyph : seenNowGlyphs) {
                        LOGGER.log(Level.SEVERE, "→ {0}, in cell(s)", glyph);
                        for (QuadTree cell : glyph.getCells()) {
                            LOGGER.log(Level.SEVERE, "     {0}", cell);
                        }
                    }
                    LOGGER.log(Level.SEVERE, "Timestamp of {1} is {0}.",
                            new Object[] {e.getAt(), e.getType()});
                    return null;
                }
                totalGlyphs = c;
            }
            // reset higher log level for tracked glyphs, if applicable
            if (LOGGER != null)
                LOGGER.setLevel(defaultLevel);
        }
        if (LOGGER != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                Stats.record("total # works", result.getGlyph().getN());
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
                Stats.remove(tn);
                Stats.remove(tn + " handled");
                Stats.remove(tn + " discarded");
            }
            LOGGER.log(Level.FINE, "events were stored in {0} queue(s)", q.getNumQueues());
            LOGGER.log(Level.FINE, "QuadTree has {0} nodes and height {1} now",
                    new Object[] {tree.getSize(), tree.getTreeHeight()});
            Stats.logAll(LOGGER);
        }
        if (B.TIMERS_ENABLED.get())
            Timers.logAll(LOGGER);
        if (LOGGER != null)
            LOGGER.log(Level.FINER, "RETURN from AgglomerativeClustering#cluster()");
        return this;
    }

    @Override
    public String getName() {
        return "QuadTree Clusterer";
    }


    /**
     * Find glyphs that overlap the given glyph at the given timestamp/zoom level,
     * and create merge events for those instances. Add those merge events to the
     * given priority queue.
     *
     * The merge events created by this function are with {@code null} instead of
     * with {@code with}, for convenience reasons in the nested merge loop. That
     * is, the `merged` glyph changes (the object), even though the conceptual
     * glyph does not. Representing with {@code null} fixes that problem.
     *
     * @param g GrowFunction to use for deciding when glyphs touch.
     * @param with Glyph to check overlap with.
     * @param at Timestamp/zoom level at which overlap must be checked.
     * @param addTo Queue to add merge events to.
     * @param bigGlyphs List of big glyphs currently alive.
     * @return Whether any overlap was found at all.
     */
    private boolean findOverlap(GrowFunction g, Glyph with, double at,
            PriorityQueue<GlyphMerge> addTo, List<Glyph> bigGlyphs) {
        boolean foundOverlap = false;

        // check glyphs in cells of the given glyph
        double bAt; // before `at`, used to store time/zoom level of found merges
        for (QuadTree cell : tree.getLeaves(with, at, g)) {
            for (Glyph glyph : cell.getGlyphsAlive()) {
                if ((bAt = g.intersectAt(with, glyph)) <= at) {
                    foundOverlap = true;
                    addTo.add(new GlyphMerge(null, glyph, bAt));
                }
            }
        }

        // also check big glyphs separately
        for (Glyph big : bigGlyphs) {
            if (big != with && (bAt = g.intersectAt(with, big)) <= at) {
                foundOverlap = true;
                addTo.add(new GlyphMerge(null, big, bAt));
            }
        }

        return foundOverlap;
    }

    /**
     * Returns the first event that will happen. Normally, this is the head of
     * the given {@link MultiQueue} (modulo discarded events). However, the queues
     * of {@linkplain GlobalState#bigGlyphs big glyphs} are also checked.
     *
     * @return The next event to occur, or {@code null} if there are no more
     *         events to handle or only a single alive glyph left.
     */
    private Event getNextEvent(MultiQueue q, GlobalState s) {
        if (s.numAlive <= 1) {
            return null;
        }

        // check the queue
        Event event = null;
        Event queueEvent = null;
        findQueueEvent: while (!q.isEmpty()) {
            event = q.peek();
            // we ignore out of cell events for non-leaf cells
            if (event.getType() == Type.OUT_OF_CELL &&
                    !((OutOfCell) event).getCell().isLeaf()) {
                q.discard();
                continue;
            }
            // we ignore this event if not all glyphs from it are alive anymore
            for (Glyph glyph : event.getGlyphs()) {
                if (!glyph.isAlive()) {
                    q.discard();
                    continue findQueueEvent;
                }
            }
            event = queueEvent = q.peek();
            break;
        }

        // check the big glyphs
        Glyph bigGlyph = null;
        for (Glyph big : s.bigGlyphs) {
            LOGGER.log(Level.FINER, "searching for uncertain merge on {0}", big);
            UncertainGlyphMerge bEvt = big.peekUncertain();
            LOGGER.log(Level.FINER, "found {0}", bEvt);
            if (event == null || (bEvt != null && bEvt.getAt() < event.getAt())) {
                event = bEvt.getGlyphMerge();
                bigGlyph = big;
            }
        }

        // if we are going with the queue event, remove it from the queue
        // otherwise remove it from the queue of the glyph it came from
        if (event != null) {
            if (event == queueEvent) {
                q.poll();
            } else {
                bigGlyph.pollUncertain();
            }
        }

        return event;
    }

    private void handleBigGlyphMerge(GrowFunction g, GlyphMerge m,
            GlobalState s, MultiQueue q, boolean track) {
        if (B.TIMERS_ENABLED.get()) {
            Timers.start("[merge event processing] big");
        }

        // process the merge and all merges that it causes
        Glyph merged = processNestedMerges(g, m, s, q, track);

        // is the merged glyph not big anymore?
        if (!merged.isBig()) {
            // update queues of big glyphs
            for (Glyph big : s.bigGlyphs) {
                big.record(new GlyphMerge(big, merged, g).uncertain());
            }

            // record merge events and out of cell events
            recordEventsForGlyph(merged, m.getAt(), g, q);
        } else {
            // we can adopt the merge events if it was a simple big/small merge
            // otherwise we need to rebuild from scratch
            if (s.mergedBigGlyph) {
                initializeBigGlyphEvents(merged, g, s);
            } else {
                // update merge events
                for (Glyph glyph : m.getGlyphs()) {
                    if (glyph.isBig()) {
                        merged.adoptUncertainMergeEvents(glyph, m);
                    }
                }
            }
        }

        // update bookkeeping
        recordGlyphAndStats(merged, s, q, track);

        if (B.TIMERS_ENABLED.get()) {
            Timers.stop("[merge event processing] big");
        }
    }

    private void handleGlyphMerge(GrowFunction g, GlyphMerge m,
            GlobalState s, MultiQueue q, boolean track) {
        // process the merge and all merges that it causes
        Glyph merged = processNestedMerges(g, m, s, q, track);

        // if the glyph became big now, it has not been inserted into the QuadTree
        // we need to initialize its queue in that case
        if (merged.isBig()) {
            initializeBigGlyphEvents(merged, g, s);
        } else {
            // update queues of big glyphs
            for (Glyph big : s.bigGlyphs) {
                big.record(new GlyphMerge(big, merged, g).uncertain());
            }

            // record merge events and out of cell events
            recordEventsForGlyph(merged, m.getAt(), g, q);
        }

        // update bookkeeping
        recordGlyphAndStats(merged, s, q, track);
    }

    private void handleOutOfCell(GrowFunction g, OutOfCell o,
            Map<Glyph, HierarchicalClustering> map, boolean includeOutOfCell,
            PriorityQueue<Event> q) {
        Glyph glyph = o.getGlyphs()[0];
        // possibly include the event
        if (includeOutOfCell &&
                Utils.Double.neq(map.get(glyph).getAt(), o.getAt())) {
            HierarchicalClustering hc = new HierarchicalClustering(glyph,
                    o.getAt(), map.get(glyph));
            map.put(glyph, hc);
        }
        // handle orphaned cells
        QuadTree cell = o.getCell().getNonOrphanAncestor();
        if (o.getCell() != cell) {
            // if the event was for an internal border of this non-orphan cell,
            // we don't have to add merge events anymore
            if (!Utils.onBorderOf(o.getCell().getSide(o.getSide()),
                    cell.getRectangle())) {
                // we do need to add an event for when this glyph grows out of
                // the non-orphan cell, because that has not been done yet
                glyph.record(new OutOfCell(glyph, g, cell, o.getSide()));
                glyph.popOutOfCellInto(q, LOGGER);
                return; // nothing to be done anymore
            }
        }

        // because of the above check for the border being on the actual border of
        // the non-orphaned cell, the timestamp is exactly the same, so we do not
        // need to (re)calculate it
        double oAt = o.getAt();
        Side oppositeSide = o.getSide().opposite();
        // create merge events with the glyphs in the neighbors
        // we take the size of the glyph at that point in time into account
        double[] sideInterval = Side.interval(
                g.sizeAt(glyph, oAt, g.getCompressionLevel(glyph)).getBounds2D(),
                o.getSide());
        if (LOGGER != null)
            LOGGER.log(Level.FINER, "size at border is {0}", Arrays.toString(sideInterval));
        // Copy the set of neighbors returned, as the neighbors may in fact change
        // while the out of cell event is being handled; inserting the glyph into
        // the neighboring cells can cause a split to occur and the neighbors to
        // update. All of that is avoided by making a copy now.
        List<QuadTree> neighbors = new ArrayList<>(cell.getNeighbors(o.getSide()));
        if (LOGGER != null)
            LOGGER.log(Level.FINEST, "growing out of {1} of {0} into",
                    new Object[] {o.getCell(), o.getSide()});
        for (QuadTree neighbor : neighbors) {
            if (LOGGER != null)
                LOGGER.log(Level.FINEST, "{0}", neighbor);

            // ensure that glyph actually grows into this neighbor
            if (!Utils.intervalsOverlap(Side.interval(
                    neighbor.getSide(oppositeSide), oppositeSide), sideInterval)) {
                if (LOGGER != null)
                    LOGGER.log(Level.FINEST, "→ but not at this point in time, so ignoring");
                continue;
            }

            // ensure that glyph was not in this cell yet
            if (neighbor.getGlyphs() != null && neighbor.getGlyphs().contains(glyph)) {
                if (LOGGER != null)
                    LOGGER.log(Level.FINEST, "→ but was already in there, so ignoring");
                // there might still be other interesting events for this glyph
                glyph.popOutOfCellInto(q, LOGGER);
                continue;
            }

            // register glyph in cell(s) it grows into
            neighbor.insert(glyph, oAt, g);

            // split cell if necessary, to maintain maximum glyphs per cell
            List<QuadTree> grownInto;
            if (neighbor.getGlyphs() != null &&
                    neighbor.getGlyphs().size() > I.MAX_GLYPHS_PER_CELL.get()) {
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
                // this step is currently not implemented
                // 4. continue with making events in appropriate cells instead
                //    of `neighbor` or all glyphs associated with `neighbor`
                grownInto = neighbor.getLeaves(glyph, oAt, g);
                if (LOGGER != null && LOGGER.isLoggable(Level.FINE)) {
                    for (QuadTree in : neighbor.getLeaves()) {
                        Stats.record("glyphs per cell",
                                in.getGlyphsAlive().size());
                    }
                }
            } else {
                grownInto = neighbor.getLeaves();
            }

            rec.from(glyph);
            for (QuadTree in : grownInto) {
                // create merge events with glyphs in the cells the glyph grows
                // into - we must do this to get correctness
                Timers.start("first merge recording 4");
                rec.record(in.getGlyphs());
                Timers.stop("first merge recording 4");

                // create out of cell events for the cells the glyph grows into,
                // but only when they happen after the current event
                for (Side side : o.getSide().opposite().others()) {
                    double at = g.exitAt(glyph, in, side);
                    if (at >= oAt) {
                        // only create an event when at least one neighbor on
                        // this side does not contain the glyph yet
                        boolean create = false;
                        List<QuadTree> neighbors2 = in.getNeighbors(side);
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
                        if (LOGGER != null)
                            LOGGER.log(Level.FINEST, "→ out of {0} of {2} at {1}",
                                    new Object[] {side, at, in});
                        glyph.record(new OutOfCell(glyph, in, side, at));
                    }
                }
            }
            glyph.popOutOfCellInto(q, LOGGER);
            rec.addEventsTo(q, LOGGER);
        }
    }

    /**
     * Given a big glyph, create uncertain merge events with all other glyphs in
     * the QuadTree, and other big glyphs if there are any.
     */
    private void initializeBigGlyphEvents(Glyph glyph, GrowFunction g,
            GlobalState s) {
        // other big glyphs
        for (Glyph big : s.bigGlyphs) {
            glyph.record(new GlyphMerge(glyph, big, g).uncertain());
        }

        // non-big glyphs
        for (Glyph small : Utils.iterable(tree.iteratorGlyphsAlive())) {
            glyph.record(new GlyphMerge(glyph, small, g).uncertain());
        }
    }

    /**
     * Given a merge event, see if performing it would cause more merges, and
     * process those repeatedly until no overlap remains. This function also
     * has glyphs that tracked any merged glyphs update who they track, and
     * inserts the merged glyph into the QuadTree. When the merging of glyphs
     * causes cells of the QuadTree to join, then new merge events are created
     * in those joined cells as well.
     */
    private Glyph processNestedMerges(GrowFunction g, GlyphMerge m,
            GlobalState s, MultiQueue q, boolean track) {
        if (B.TIMERS_ENABLED.get()) {
            Timers.start("[merge event processing] total");
            if (track) {
                Timers.start("[merge event processing] total (track)");
            }
        }
        s.nestedMerges.add(m);
        s.mergedBigGlyph = false;
        // create a merged glyph and ensure that the merged glyph does not
        // overlap other glyphs at this time - repeat until no more overlap
        Glyph merged = null;
        HierarchicalClustering mergedHC = null;
        double mergedAt = m.getAt();

        if (B.TIMERS_ENABLED.get()) {
            Timers.start("[merge event processing] nested merges");
        }
        do {
            nestedMerge: while (!s.nestedMerges.isEmpty()) {
                m = s.nestedMerges.poll();

                // check that all glyphs in the merge are still alive
                for (Glyph glyph : m.getGlyphs()) {
                    if (glyph != null && !glyph.isAlive()) {
                        continue nestedMerge;
                    }
                }

                if (LOGGER != null) {
                    LOGGER.log(Level.FINEST, "handling nested " + m);
                }

                // create a merged glyph, update clustering
                if (mergedHC == null) {
                    merged = new Glyph(m.getGlyphs());
                    mergedHC = new HierarchicalClustering(merged,
                        mergedAt, Utils.map(m.getGlyphs(), s.map,
                        s.createdFromTmp));
                } else {
                    mergedHC.alsoCreatedFrom(s.map.get(m.getGlyphs()[1]));
                    merged = new Glyph(merged, m.getGlyphs()[1]);
                    mergedHC.setGlyph(merged);
                    if (m.getGlyphs()[1].isBig()) {
                        s.mergedBigGlyph = true;
                        Stats.count("merge nested big");
                    } else {
                        Stats.count("merge nested small");
                    }
                }

                // mark merged glyphs as dead
                for (Glyph glyph : m.getGlyphs()) {
                    // we skip the `merged` glyph, see `#findOverlap`
                    if (glyph == null || !glyph.isAlive()) {
                        continue;
                    }
                    glyph.perish(); s.numAlive--; s.glyphSize.unrecord(glyph.getN());
                    if (glyph.isBig()) {
                        s.bigGlyphs.remove(glyph);
                    }
                    // copy the set of cells the glyph is in currently, because we
                    // are about to change that set and don't want to deal with
                    // ConcurrentModificationExceptions...
                    for (QuadTree cell : new ArrayList<>(glyph.getCells())) {
                        if (cell.removeGlyph(glyph, mergedAt)) {
                            // handle merge events (later, see below)
                            s.orphanedCells.add(cell);
                            // out of cell events are handled when they
                            // occur, see #handleOutOfCell
                        }
                    }
                    // update merge events of glyphs that tracked merged glyphs
                    if (B.TRACK.get() && !B.ROBUST.get()) {
                        for (Glyph tracker : glyph.trackedBy) {
                            if (!s.trackersNeedingUpdate.contains(tracker)) {
                                s.trackersNeedingUpdate.add(tracker);
                            }
                        }
                    }
                }

                if (LOGGER != null) {
                    LOGGER.log(Level.FINEST, "→ merged glyph is {0}", merged);
                }
            }
        } while (findOverlap(g, merged, mergedAt, s.nestedMerges, s.bigGlyphs));
        if (B.TIMERS_ENABLED.get()) {
            Timers.stop("[merge event processing] nested merges");
            Timers.start("[merge event processing] merge events in joined cells");
        }
        // handle adding merge events in joined cells
        s.orphanedCells.stream()
                .map((cell) -> cell.getNonOrphanAncestor())
                .distinct()
                .forEach((cell) -> {
                    if (B.TIMERS_ENABLED.get())
                        Timers.start("record all pairs");
                    rec.recordAllPairs(cell.getNonOrphanAncestor(), q, LOGGER);
                    if (B.TIMERS_ENABLED.get())
                        Timers.stop("record all pairs");
                });
        s.orphanedCells.clear();
        if (B.TIMERS_ENABLED.get()) {
            Timers.stop("[merge event processing] merge events in joined cells");
            Timers.start("[merge event processing] tracker updating");
        }
        // update merge events of glyphs that tracked merged glyphs
        if (B.TRACK.get() && !B.ROBUST.get()) {
            for (Glyph orphan : s.trackersNeedingUpdate) {
                if (orphan.isAlive()) {
                    Stats.record("orphan cells", orphan.getCells().size());
                    if (!orphan.popMergeInto(q, LOGGER)) {
                        rec.from(orphan);
                        if (B.TIMERS_ENABLED.get())
                            Timers.start("first merge recording 2");
                        for (QuadTree cell : orphan.getCells()) {
                            rec.record(cell.getGlyphs());
                        }
                        if (B.TIMERS_ENABLED.get())
                            Timers.stop("first merge recording 2");
                        rec.addEventsTo(q, LOGGER);
                    }
                }
            }
            s.trackersNeedingUpdate.clear();
        }
        if (B.TIMERS_ENABLED.get()) {
            Timers.stop("[merge event processing] tracker updating");
            Timers.start("[merge event processing] merged glyph insert");
        }
        // add new glyph to QuadTree cell(s)
        merged.setBig(s.glyphSize, g);
        if (!merged.isBig()) {
            tree.insert(merged, mergedAt, g);
            if (LOGGER != null) {
                LOGGER.log(Level.FINER, "inserted merged glyph into {0} cells",
                        merged.getCells().size());
            }
        }
        if (B.TIMERS_ENABLED.get()) {
            Timers.stop("[merge event processing] merged glyph insert");
        }

        // eventually, the last merged glyph is the root
        s.map.put(merged, mergedHC);
        result = mergedHC;

        return merged;
    }

    private void recordGlyphAndStats(Glyph merged, GlobalState s, MultiQueue q,
            boolean track) {
        merged.participate(); s.numAlive++; s.glyphSize.record(merged.getN());
        if (merged.isBig()) {
            Stats.record("merged cells big glyphs", merged.getCells().size());
            Stats.record("glyphs around big glyphs",
                    merged.getCells().stream().mapToInt((cell) ->
                        cell.getGlyphsAlive().size()).sum());
            s.bigGlyphs.add(merged);
            Stats.record("number of big glyphs", s.bigGlyphs.size());
        }

        if (B.TIMERS_ENABLED.get()) {
            Timers.stop("[merge event processing] total");
            if (track) {
                Timers.stop("[merge event processing] total (track)");
            }
        }
        if (D.TIME_MERGE_EVENT_AGGLOMERATIVE.get() > 0 &&
                B.LOGGING_ENABLED.get()) {
            Stats.count("merge event handling");
            double elapsed = Timers.in(Timers.elapsing("clustering"),
                    Units.SECONDS);
            if (elapsed - s.lastDumpedMerges >=
                    D.TIME_MERGE_EVENT_AGGLOMERATIVE.get()) {
                s.lastDumpedMerges = elapsed;
                LOGGER.log(Level.FINE, "processed {0} merge events after "
                        + "{1} seconds", new Object[] {
                        Stats.get("[count] merge event handling").getN(),
                        elapsed});
                LOGGER.log(Level.FINER, "inserted {0} events",
                        q.getInsertions());
                LOGGER.log(Level.FINER, "discarded {0} events",
                        q.getDiscarded());
                LOGGER.log(Level.FINER, "deleted {0} events",
                        q.getDeletions());
            }
        }
    }

    /**
     * Given a freshly created glyph originating from a merge, loop over the
     * QuadTree cells of that glyph and record out of cell events for all.
     * In the same loop, find merges as well, using the global {@link #rec}.
     */
    private void recordEventsForGlyph(Glyph merged, double at,
            GrowFunction g, MultiQueue q) {
        if (B.TIMERS_ENABLED.get())
            Timers.start("[merge event processing] merge event recording");
        // create events with remaining glyphs
        // (we always have to loop over cells here, `merged` has just
        //  been created and thus hasn't recorded merge events yet)
        rec.from(merged);
        Stats.record("merged cells", merged.getCells().size());
        for (QuadTree cell : merged.getCells()) {
            if (B.TIMERS_ENABLED.get())
                Timers.start("first merge recording 3");
            rec.record(cell.getGlyphs());
            if (B.TIMERS_ENABLED.get())
                Timers.stop("first merge recording 3");
            // create out of cell events
            for (Side side : Side.values()) {
                // only create an event when at least one neighbor on
                // this side does not contain the merged glyph yet
                boolean create = false;
                if (B.TIMERS_ENABLED.get())
                    Timers.start("neighbor finding");
                List<QuadTree> neighbors = cell.getNeighbors(side);
                if (B.TIMERS_ENABLED.get())
                    Timers.stop("neighbor finding");
                for (QuadTree neighbor : neighbors) {
                    if (!neighbor.getGlyphs().contains(merged)) {
                        create = true;
                        break;
                    }
                }
                if (!create) {
                    continue;
                }
                // now, actually create an OUT_OF_CELL event, but only
                // if the event is still about to happen
                OutOfCell ooe = new OutOfCell(merged, g, cell, side);
                if (ooe.getAt() > at) {
                    merged.record(ooe);
                    if (LOGGER != null)
                        LOGGER.log(Level.FINEST, "→ out of {0} of {2} at {1}",
                                new Object[] {side, ooe.getAt(), cell});
                }
            }
        }
        merged.popOutOfCellInto(q, LOGGER);
        rec.addEventsTo(q, LOGGER);
        if (B.TIMERS_ENABLED.get())
            Timers.stop("[merge event processing] merge event recording");
    }

    /**
     * Executed right before going to the next iteration of the event handling
     * loop. Possibly pauses executiong, depending on parameter.
     *
     * @param step Whether execution should be paused.
     */
    private void step(boolean step) {
        if (B.STATS_ENABLED.get()) {
            Stats.record("QuadTree cells", Utils.size(tree.iterator()));
            Stats.record("QuadTree leaves", tree.getLeaves().size());
            Stats.record("QuadTree height", tree.getTreeHeight());
        }

        if (step) {
            try {
                System.in.read();
            } catch (IOException e1) {
                // Well, that's weird. #ShouldNeverHappen #FamousLastWords
            }
        }
    }


    /**
     * Object that is used to easily share state between
     * {@link QuadTreeClusterer#cluster(GrowFunction, boolean, boolean) cluster} and
     * {@link QuadTreeClusterer#handleGlyphMerge(GrowFunction, GlyphMerge,
     * PriorityQueue) handleGlyphMerge}.
     */
    private static class GlobalState {

        // we have a queue for nested merges, and a temporary array that is reused,
        // and two sets that are reused somewhere deep in the algorithm
        private PriorityQueue<GlyphMerge> nestedMerges = new PriorityQueue<>();
        private HierarchicalClustering[] createdFromTmp = new HierarchicalClustering[2];
        private List<Glyph> trackersNeedingUpdate = new ArrayList<>();
        private List<QuadTree> orphanedCells = new ArrayList<>();
        // see Constants.D#TIME_MERGE_EVENT_AGGLOMERATIVE
        private double lastDumpedMerges = Timers.in(Timers.elapsed("clustering"), Units.SECONDS);
        // mapping from glyphs to (currently) highest level nodes in resulting clustering
        private Map<Glyph, HierarchicalClustering> map;
        // finally, create an indication of which glyphs still participate
        private int numAlive = 0;
        // statistic for sizes of currently alive glyphs
        private Stat glyphSize = new Stat();
        // list of alive big glyphs; these are not in the QuadTree and thus tracked separately
        private List<Glyph> bigGlyphs = new ArrayList<>(2);
        // used as output parameter of #processNestedMerges to indicate if a big glyph was merged
        private boolean mergedBigGlyph;

        private GlobalState(Map<Glyph, HierarchicalClustering> map) {
            this.map = map;
        }

    }

}
