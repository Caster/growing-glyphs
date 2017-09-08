package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class HierarchicalClustering implements Comparable<HierarchicalClustering> {

    private Square square;
    private final double at;
    private Set<HierarchicalClustering> createdFrom;
    private HierarchicalClustering mergedInto;

    /**
     * Create a (node of a) hierarchical clustering that represents a square and
     * the squares it was created from. The clustering records at which point in
     * time/zooming the merge happened as well.
     *
     * @param square Square that was created from a merge.
     * @param at Time or zoom level at which the merge happened.
     * @param createdFrom One or more squares that were merged into {@code square}.
     *            It is also possible to construct a hierarchical clustering of a
     *            single square by omitting this parameter.
     */
    public HierarchicalClustering(Square square, double at,
            HierarchicalClustering... createdFrom) {
        if (at < 0) {
            throw new IllegalArgumentException();
        }

        this.square = square;
        this.at = at;
        if (createdFrom.length == 0) {
            this.createdFrom = null;
        } else {
            this.createdFrom = new HashSet<>(createdFrom.length);
            this.createdFrom.addAll(Arrays.asList(createdFrom));
        }
        this.mergedInto = null;

        if (this.createdFrom != null) {
            for (HierarchicalClustering node : this.createdFrom) {
                node.mergeInto(this);
            }
        }
    }

    public void alsoCreatedFrom(HierarchicalClustering from) {
        if (createdFrom == null) {
            createdFrom = new HashSet<>(2);
        }
        createdFrom.add(from);
        from.mergeInto(this);
    }

    @Override
    public int compareTo(HierarchicalClustering that) {
        return (int) Math.signum(this.at - that.at);
    }

    public double getAt() {
        return at;
    }

    public HierarchicalClustering getMergedInto() {
        return mergedInto;
    }

    public Square getSquare() {
        return square;
    }

    public void mergeInto(HierarchicalClustering node) {
        this.mergedInto = node;
    }

    public void setSquare(Square square) {
        this.square = square;
    }

    @Override
    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        String moreIndent = indent + "  ";

        StringBuilder sb = new StringBuilder(indent);
        sb.append(getClass().getName());
        sb.append("[\n");
        sb.append(moreIndent);
        sb.append(square.toString());
        sb.append(" at ");
        sb.append(at);
        if (createdFrom == null) {
            sb.append("\n");
        } else {
            sb.append(" from \n");
            for (HierarchicalClustering hc : createdFrom) {
                sb.append(hc.toString(moreIndent));
            }
        }
        sb.append(indent);
        sb.append("]\n");
        return sb.toString();
    }


    /**
     * A view onto a clustering is an easy way to navigate through the various
     * events that created the clustering. Useful for debugging.
     *
     * A view has a set of current nodes of a {@link HierarchicalClustering} and
     * maintains priority queues of (1) these current nodes and (2) their parents.
     * This way, merges can be performed and undone step by step.
     */
    public static class View {

        private Queue<HierarchicalClustering> next;
        private Queue<HierarchicalClustering> prev;
        private Set<HierarchicalClustering> curr;
        /**
         * A view can make half steps, wherein the situation right before a merge
         * is shown. This indicates if a half step forward is taken (true) or not.
         */
        private boolean halfStep = false;
        private boolean countingSteps;
        private int n;


        public View(HierarchicalClustering clustering) {
            if (clustering == null) {
                throw new NullPointerException();
            }
            this.curr = new HashSet<>();
            this.curr.add(clustering);
            this.prev = new PriorityQueue<>();
            if (clustering.createdFrom != null) {
                this.prev.add(clustering);
            }
            this.next = new PriorityQueue<>();
            if (clustering.mergedInto != null) {
                this.next.add(clustering.mergedInto);
            }

            this.n = 0;
            this.countingSteps = true;
            start();
            this.countingSteps = false;
        }

        /**
         * Returns the number of steps that can be taken between start and end.
         */
        public int getNumberOfSteps() {
            return n;
        }

        public int getSize() {
            return curr.size();
        }

        public Rectangle2D[] getSquares(GrowFunction g) {
            Rectangle2D[] result = new Rectangle2D[curr.size()];
            int i = 0;
            double maxAt = Double.NEGATIVE_INFINITY;
            if (halfStep) {
                maxAt = next.peek().at;
            } else {
                for (HierarchicalClustering node : curr) {
                    if (node.at > maxAt) {
                        maxAt = node.at;
                    }
                }
            }
            for (HierarchicalClustering node : curr) {
                result[i++] = g.sizeAt(node.square, maxAt);
            }
            return result;
        }

        /**
         * Move the view to the end of the clustering, meaning only a single
         * cluster will be viewed.
         */
        public void end() {
            while (!next.isEmpty()) {
                next();
            }
        }

        /**
         * Move the view to the next step, meaning that one merge is performed.
         */
        public void next() {
            if (!halfStep && next.size() > 0 &&
                    (next.peek().createdFrom == null ||
                    next.peek().createdFrom.size() > 1)) {
                halfStep = true;
                step();
                return;
            }
            HierarchicalClustering node = next.poll();
            if (node == null) {
                return;
            }
            halfStep = false;
            if (node.createdFrom != null) {
                for (HierarchicalClustering from : node.createdFrom) {
                    curr.remove(from);
                    prev.remove(from);
                }
                prev.add(node);
            }
            curr.add(node);
            if (node.mergedInto != null && !next.contains(node.mergedInto)) {
                next.add(node.mergedInto);
            }
            step();
        }

        /**
         * Move the view to the previous step, undoing one merge.
         */
        public void previous() {
            if (halfStep) {
                halfStep = false;
                step();
                return;
            }
            HierarchicalClustering node = prev.poll();
            if (node == null) {
                return;
            }
            curr.remove(node);
            if (node.mergedInto != null) {
                next.remove(node.mergedInto);
            }
            next.add(node);
            // only nodes that have a createdFrom should ever be added to prev
            // hence, no null check is needed
            for (HierarchicalClustering from : node.createdFrom) {
                curr.add(from);
                if (from.createdFrom != null) {
                    prev.add(from);
                }
            }
            if (prev.size() > 0 && (prev.peek().createdFrom == null ||
                    prev.peek().createdFrom.size() > 1)) {
                halfStep = true;
            }
            step();
        }

        /**
         * Move the view to the start of the clustering, meaning every entity
         * will be disjoint.
         */
        public void start() {
            while (!prev.isEmpty()) {
                previous();
            }
        }


        /**
         * Count a step, when counting is enabled.
         */
        private void step() {
            if (countingSteps) {
                n++;
            }
        }

    }

}
