package datastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import datastructure.growfunction.GrowFunction;
import gui.GlyphShape;

public class HierarchicalClustering implements Comparable<HierarchicalClustering> {

    private static final Logger LOGGER =
            Logger.getLogger(HierarchicalClustering.class.getName());


    private Glyph glyph;
    private final double at;
    private List<HierarchicalClustering> createdFrom;
    private HierarchicalClustering mergedInto;


    /**
     * Create a (node of a) hierarchical clustering that represents a glyph and
     * the glyphs it was created from. The clustering records at which point in
     * time/zooming the merge happened as well.
     *
     * @param glyph Glyph that was created from a merge.
     * @param at Time or zoom level at which the merge happened.
     * @param createdFrom One or more glyphs that were merged into {@code glyph}.
     *            It is also possible to construct a hierarchical clustering of a
     *            single glyph by omitting this parameter.
     */
    public HierarchicalClustering(Glyph glyph, double at,
            HierarchicalClustering... createdFrom) {
        this.glyph = glyph;
        this.at = at;
        if (createdFrom.length == 0) {
            this.createdFrom = null;
        } else {
            this.createdFrom = new ArrayList<>(Arrays.asList(createdFrom));
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
            createdFrom = new ArrayList<>(2);
        }
        if (!createdFrom.contains(from)) {
            createdFrom.add(from);
        }
        from.mergeInto(this);
    }

    @Override
    public int compareTo(HierarchicalClustering that) {
        return (int) Math.signum(this.at - that.at);
    }

    public double getAt() {
        return at;
    }

    public List<HierarchicalClustering> getCreatedFrom() {
        return createdFrom;
    }

    public HierarchicalClustering getMergedInto() {
        return mergedInto;
    }

    public Glyph getGlyph() {
        return glyph;
    }

    public void mergeInto(HierarchicalClustering node) {
        this.mergedInto = node;
    }

    public void setGlyph(Glyph glyph) {
        this.glyph = glyph;
    }

    @Override
    public String toString() {
        return toString("", true, true, -1);
    }

    public String toString(String indent, boolean showCreatedFrom,
            boolean showCreatedFromRecursively, int limit) {
        String moreIndent = indent + "  ";

        StringBuilder sb = new StringBuilder(indent);
        sb.append(getClass().getName());
        sb.append("[\n");
        sb.append(moreIndent);
        sb.append(glyph.toString());
        sb.append(" at ");
        sb.append(at);
        if (createdFrom == null) {
            sb.append("\n");
        } else {
            sb.append(String.format(" from (%d)\n", createdFrom.size()));
            if (showCreatedFrom) {
                Collections.sort(createdFrom, new Comparator<HierarchicalClustering>() {
                    @Override
                    public int compare(HierarchicalClustering hc1, HierarchicalClustering hc2) {
                        int d = (int) Math.signum(hc2.getAt() - hc1.getAt());
                        if (d == 0) {
                            return hc2.getGlyph().getN() - hc1.getGlyph().getN();
                        }
                        return d;
                    }
                });
                int i = 0;
                for (HierarchicalClustering hc : createdFrom) {
                    sb.append(hc.toString(moreIndent, showCreatedFromRecursively,
                            showCreatedFromRecursively, limit));
                    if (++i == limit)
                        break;
                }
                if (i < createdFrom.size()) {
                    sb.append(moreIndent);
                    sb.append("... (" + (createdFrom.size() - i) + " more)\n");
                }
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
        private List<HierarchicalClustering> curr;
        /**
         * A view can make half steps, wherein the situation right before a merge
         * is shown. This indicates if a half step forward is taken (true) or not.
         */
        private boolean halfStep;
        private boolean countingSteps;
        private boolean ignoreSteps;
        private int c; // current step
        private int n; // total number of steps
        private boolean logging;
        private JSlider syncWith;
        private ChangeListener cl;


        public View(HierarchicalClustering clustering) {
            this.logging = false;
            this.syncWith = null;
            this.cl = null;
            if (clustering == null) {
                throw new NullPointerException();
            }

            // create set of current nodes
            this.curr = new ArrayList<>();
            this.curr.add(clustering);

            // create queue for previous nodes
            // this code has a reversed ordering, as the events that happened last
            // should be undone first, when going back
            this.prev = new PriorityQueue<>(new Comparator<HierarchicalClustering>() {
                @Override
                public int compare(HierarchicalClustering o1, HierarchicalClustering o2) {
                    return o2.compareTo(o1);
                }
            });
            if (clustering.createdFrom != null) {
                this.prev.add(clustering);
            }

            // create queue for next nodes
            this.next = new PriorityQueue<>();
            if (clustering.mergedInto != null) {
                this.next.add(clustering.mergedInto);
            }

            // keep undoing merges until we are at the start of the clustering
            this.n = 1;
            this.countingSteps = true;
            this.halfStep = false;
            start();
            this.countingSteps = false;
            this.c = 1;
            this.ignoreSteps = false;

            // log current state
            this.logging = true;
            LOGGER.log(Level.FINE, "initialized {0}", getClass().getName());
            if (LOGGER.isLoggable(Level.FINE)) {
                logCurrentState();
            }
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

        public int getStep() {
            return c;
        }

        public double getAt() {
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
            return maxAt;
        }

        public GlyphShape[] getGlyphs(GrowFunction g) {
            GlyphShape[] result = new GlyphShape[curr.size()];
            int i = 0;
            double at = getAt();
            for (HierarchicalClustering node : curr) {
                result[i++] = new GlyphShape(node.glyph, at, g);
            }
            return result;
        }

        /**
         * Move the view to the end of the clustering, meaning only a single
         * cluster will be viewed.
         */
        public void end() {
            boolean oldLogging = logging;
            logging = false;
            while (!next.isEmpty()) {
                next();
            }
            logging = oldLogging;
        }

        /**
         * Move the view to the next step, meaning that one merge is performed.
         */
        public void next() {
            if (!halfStep && next.size() > 0 &&
                    (next.peek().createdFrom == null ||
                    next.peek().createdFrom.size() > 1)) {
                halfStep = true;
                step(1);
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
            if (!curr.contains(node)) {
                curr.add(node);
            }
            if (node.mergedInto != null && !next.contains(node.mergedInto)) {
                next.add(node.mergedInto);
            }
            step(1);
        }

        /**
         * {@link #next() Move the view to the next step}, if there is a next
         * step. Returns whether a step was performed.
         */
        public boolean nextIfPossible() {
            int currC = c;
            if (c < n) {
                next();
            }
            return (currC != c);
        }

        /**
         * Move the view to the previous step, undoing one merge.
         */
        public void previous() {
            if (halfStep) {
                halfStep = false;
                step(-1);
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
                if (!curr.contains(from)) {
                    curr.add(from);
                }
                // we only add nodes created from others to the prev queue,
                // and also only nodes that have been created at a time >= 0
                if (from.createdFrom != null && from.at >= 0) {
                    prev.add(from);
                }
            }
            halfStep = true;
            step(-1);
        }

        /**
         * {@link #previous() Move the view to the previous step}, if there is a
         * previous step. Returns whether a step was performed.
         */
        public boolean previousIfPossible() {
            int currC = c;
            if (c > 1) {
                previous();
            }
            return (currC != c);
        }

        public void setChangeListener(ChangeListener listener) {
            cl = listener;
        }

        /**
         * Move the view to the start of the clustering, meaning every entity
         * will be disjoint.
         */
        public void start() {
            boolean oldLogging = logging;
            logging = false;
            while (!prev.isEmpty()) {
                previous();
            }
            previous(); // undo half step, possibly
            logging = oldLogging;
        }

        /**
         * Enables the given slider and sets its bounds and current value. Update
         * slider value when the view changes, and will change view when the
         * slider value changes.
         *
         * @param slider Slider to synchronize with.
         */
        public void syncWith(JSlider slider) {
            slider.setEnabled(true);
            slider.setMinimum(1);
            slider.setMaximum(n);
            slider.setValue(c);
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    stepTo(syncWith.getValue());
                }
            });
            syncWith = slider;
        }


        /**
         * Log the current state of the view.
         */
        private void logCurrentState() {
            LOGGER.log(Level.FINE, "current state");
            LOGGER.log(Level.FINER, "{0}half step", (halfStep ? "" : "no "));
            LOGGER.log(Level.FINER, "current size: {0}", curr.size());
            if (LOGGER.isLoggable(Level.FINEST)) {
                for (HierarchicalClustering node : curr) {
                    LOGGER.log(Level.FINEST, "curr node {0} at {1}",
                            new Object[] {node.getGlyph(), node.getAt()});
                }
            }

            if (prev.size() == 0) {
                LOGGER.log(Level.FINER, "no prev");
            } else {
                LOGGER.log(Level.FINER, "prev ({0}):\n\n{1}",
                        new Object[] {prev.size(), prev.peek()});
                if (LOGGER.isLoggable(Level.FINEST)) {
                    for (HierarchicalClustering node : prev) {
                        LOGGER.log(Level.FINEST, "prev node {0} at {1}",
                                new Object[] {node.getGlyph(), node.getAt()});
                    }
                }
            }

            if (next.size() == 0) {
                LOGGER.log(Level.FINER, "no next");
            } else {
                LOGGER.log(Level.FINER, "next ({0}):\n\n{1}",
                        new Object[] {next.size(), next.peek()});
                if (LOGGER.isLoggable(Level.FINEST)) {
                    for (HierarchicalClustering node : next) {
                        LOGGER.log(Level.FINEST, "next node {0} at {1}",
                                new Object[] {node.getGlyph(), node.getAt()});
                    }
                }
            }
        }

        /**
         * Count a step, when counting is enabled.
         * Log the state, when logging is enabled.
         */
        private void step(int delta) {
            c += delta;
            if (!ignoreSteps && syncWith != null) {
                syncWith.setValue(c);
            }
            if (countingSteps) {
                n++;
            }
            if (logging && LOGGER.isLoggable(Level.FINE)) {
                logCurrentState();
            }
        }

        /**
         * Step until the current step index equals the given one. Stops at
         * extreme values (1 and {@code n}).
         *
         * @param step Step index to move towards.
         */
        private void stepTo(int step) {
            ignoreSteps = true;
            int lastC = c;
            while (c != step) {
                if (c < step) {
                    next();
                    if (c == n) {
                        break;
                    }
                } else {
                    previous();
                    if (c == 1) {
                        break;
                    }
                }
                if (c == lastC) {
                    if (c < step) {
                        c = n;
                    } else {
                        c = 1;
                    }
                    break;
                }
                lastC = c;
            }
            if (cl != null) {
                cl.stateChanged(new ChangeEvent(this));
            }
            ignoreSteps = false;
        }

    }

}
