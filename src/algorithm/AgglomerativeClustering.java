package algorithm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import datastructure.GrowFunction;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.Square;
import datastructure.Utils;
import datastructure.events.Event;
import datastructure.events.OutOfCell;
import datastructure.events.OutOfCell.Side;
import datastructure.events.SquareMerge;

public class AgglomerativeClustering {

    /**
     * Tree with {@link Square squares} that need clustering.
     */
    private QuadTree tree;
    private GrowFunction g;
    /**
     * Resulting clustering.
     */
    private HierarchicalClustering result;


    /**
     * Initialize algorithm for clustering growing squares on the given QuadTree.
     *
     * @param tree Tree with squares to be clustered.
     * @param g Function to use for determining the size of squares.
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
     * @param includeOutOfCell Whether events caused by a square growing out of
     *            a cell should be included in the resulting clustering.
     * @return A reference to the clustering instance, for chaining.
     */
    public AgglomerativeClustering cluster(boolean multiMerge,
            boolean includeOutOfCell) {
        // construct a queue, put everything in there
        PriorityQueue<Event> q = new PriorityQueue<>();
        // also create a result for each square, and a map to find them
        Map<Square, HierarchicalClustering> map = new HashMap<>();
        // finally, create an indication of which squares still participate
        Set<Square> alive = new HashSet<>();
        for (QuadTree leaf : tree.leaves()) {
            Square[] squares = leaf.getSquares().toArray(new Square[0]);
            for (int i = 0; i < squares.length; ++i) {
                // add events for when two squares in the same cell touch
                for (int j = i + 1; j < squares.length; ++j) {
                    q.add(new SquareMerge(squares[i], squares[j], g));
                }

                // add events for when a square grows out of its cell
                for (Side side : Side.values()) {
                    q.add(new OutOfCell(squares[i], g, leaf, side));
                }

                // create clustering leaves for all squares, mark them as alive
                map.put(squares[i], new HierarchicalClustering(squares[i], 0));
                alive.add(squares[i]);
            }
        }
        // merge squares until no pairs to merge remain
        queue: while (!q.isEmpty() && alive.size() > 1) {
            Event e = q.poll();
            // we ignore this event if not all squares from it are alive anymore
            for (Square square : e.getSquares()) {
                if (!alive.contains(square)) {
                    continue queue;
                }
            }
            // depending on the type of event, handle it appropriately
            switch (e.getType()) {
            case MERGE:
                SquareMerge m = (SquareMerge) e;
                double mergedAt = m.getAt();
                // create a merged square
                Square merged = new Square(m.getSquares());
                HierarchicalClustering mergedHC = new HierarchicalClustering(merged,
                        mergedAt, Utils.map(m.getSquares(), map,
                                new HierarchicalClustering[m.getSize()]));
                // add new square to QuadTree cell(s)
                tree.insert(merged, mergedAt, g);
                // mark merged squares as dead
                for (Square square : m.getSquares()) {
                    alive.remove(square);
                }
                // create events with remaining squares
                for (QuadTree cell : merged.getCells()) {
                    for (Square square : cell.getSquares()) {
                        if (alive.contains(square)) {
                            q.add(new SquareMerge(merged, square, g));
                        }
                    }
                }
                // update bookkeeping
                alive.add(merged);
                map.put(merged, mergedHC);
                // eventually, the last merged square is the root
                result = mergedHC;
                if (!multiMerge) {
                    continue queue;
                }
                // keep merging while next square overlaps before current event
                Event next = q.peek();
                Set<Square> inMerged = null; // keep track of squares that are merged
                Set<Square> wasMerged = null; // keep track of intermediate merges
                if (next != null) {
                    inMerged = new HashSet<>(m.getSize() * 2);
                    inMerged.addAll(Arrays.asList(m.getSquares()));
                    wasMerged = new HashSet<>(m.getSize());
                }
                merging: while (next != null && next.getAt() < mergedAt &&
                        Utils.indexOf(next.getSquares(), merged) >= 0) {
                    e = q.poll();
                    // we ignore this event if not all squares from it are alive anymore
                    for (Square square : e.getSquares()) {
                        if (!alive.contains(square)) {
                            continue merging;
                        }
                    }
                    switch (e.getType()) {
                    case MERGE:
                        m = (SquareMerge) e;
                        // previous merged square was no good, let it die and update
                        wasMerged.add(merged);
                        alive.remove(merged);
                        for (Square s : m.getSquares()) {
                            if (!wasMerged.contains(s)) {
                                inMerged.add(s);
                                mergedHC.alsoCreatedFrom(map.get(s));
                                alive.remove(s);
                            }
                        }
                        map.remove(merged); // it's as if this square never existed
                        // create updated merged square
                        merged = new Square(inMerged);
                        mergedHC.setSquare(merged);
                        // add new square to QuadTree cell(s)
                        tree.insert(merged, m.getAt(), g);
                        // create events with remaining squares
                        for (QuadTree cell : merged.getCells()) {
                            for (Square square : cell.getSquares()) {
                                if (alive.contains(square)) {
                                    q.add(new SquareMerge(merged, square, g));
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
                    Square square = e.getSquares()[0];
                    HierarchicalClustering hc = new HierarchicalClustering(square,
                            e.getAt(), map.get(square));
                    map.put(square, hc);
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
