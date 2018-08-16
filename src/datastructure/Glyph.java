package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import algorithm.clustering.QuadTreeClusterer;
import algorithm.glyphgenerator.BlowUp;
import datastructure.events.Event;
import datastructure.events.GlyphMerge;
import datastructure.events.OutOfCell;
import datastructure.events.UncertainGlyphMerge;
import datastructure.growfunction.CompressionThreshold;
import datastructure.growfunction.CompressionThreshold.Threshold;
import datastructure.growfunction.GrowFunction;
import datastructure.queues.UncertainQueue;
import utils.Constants;
import utils.Constants.B;
import utils.Constants.D;
import utils.Constants.I;
import utils.MutableOptional;
import utils.Stat;
import utils.Utils;
import utils.Utils.Stats;

/**
 * A glyph starts as a point and then grows at a given speed.
 */
public class Glyph {

    /**
     * Compression threshold that should be used for this glyph. This is determined
     * by {@link CompressionThreshold}, and cached on the glyph.
     */
    public MutableOptional<Threshold> threshold;
    /**
     * Whether this glyph is of special interest. Used for debugging.
     */
    public boolean track;
    /**
     * Used by the clustering algorithm to track which glyphs think they'll merge
     * with {@code this} glyph before merging with any other glyph.
     */
    public List<Glyph> trackedBy;

    /**
     * X-coordinate of the center of the glyph.
     */
    private double x;
    /**
     * Y-coordinate of the center of the glyph.
     */
    private double y;
    /**
     * Number of entities represented by the glyph.
     */
    private int n;
    /**
     * Used by clustering algorithm to track which glyphs are still of interest.
     * Not set: 0, alive: 1, perished: 2.
     */
    private int alive;
    /**
     * Whether this glyph represents {@linkplain Constants.D#BIG_GLYPH_FACTOR
     * more entities than the average glyph} at the time it is constructed.
     */
    private boolean big;
    /**
     * Set of QuadTree cells that this glyph intersects.
     */
    private final List<QuadTree> cells;
    /**
     * Uncertain events involving this glyph: only initialized and used in case
     * this is a big glyph. See ... for details.
     */
    private UncertainQueue uncertainMergeEvents;
    /**
     * The uncertain merge events can be adopted by another big glyph. This in
     * fact means that the glyph has merged and grown. This pointer will point
     * to that glyph, so that the "bigger version" of this glyph can be found.
     */
    private Glyph adoptedBy;
    /**
     * Events involving this glyph. Only one event is actually in the event
     * queue, others are added only when that one is popped from the queue.
     */
    private final Queue<GlyphMerge> mergeEvents;
    /**
     * Events involving this glyph. Only one event is actually in the event
     * queue, others are added only when that one is popped from the queue.
     */
    private final Queue<OutOfCell> outOfCellEvents;


    /**
     * Construct a new glyph at the given coordinates, with given growing speed.
     * The constructed glyph is not alive.
     *
     * @param x X-coordinate of the center of the glyph.
     * @param y Y-coordinate of the center of the glyph.
     * @param n Number of entities represented by the glyph.
     * @throws IllegalArgumentException When n < 1.
     */
    public Glyph(double x, double y, int n) {
        this(x, y, n, false);
    }

    /**
     * Construct a new glyph at the given coordinates, with given weight.
     *
     * @param x X-coordinate of the center of the glyph.
     * @param y Y-coordinate of the center of the glyph.
     * @param n Number of entities represented by the glyph.
     * @param alive Whether the glyph is marked alive or not.
     * @throws IllegalArgumentException When n < 1.
     */
    public Glyph(double x, double y, int n, boolean alive) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be at least 1");
        }
        this.threshold = new MutableOptional<>();
        this.track = false;
        if (B.TRACK.get() && !B.ROBUST.get()) {
            this.trackedBy = new ArrayList<>();
        } else {
            this.trackedBy = null;
        }
        this.x = x;
        this.y = y;
        this.n = n;
        this.alive = (alive ? 1 : 0);
        this.big = false;
        this.cells = new ArrayList<>();
        this.uncertainMergeEvents = null;
        this.adoptedBy = null;
        this.mergeEvents = new PriorityQueue<>(I.MAX_MERGES_TO_RECORD.get());
        this.outOfCellEvents = new PriorityQueue<>();
    }

    /**
     * Construct a new glyph that has its center at the weighted average of the
     * centers of the given glyphs, and the sum of their weights.
     *
     * @param glyphs glyphs to construct a new glyph out of.
     */
    public Glyph(Iterable<Glyph> glyphs) {
        this(0, 0, 1);
        this.n = 0;
        for (Glyph glyph : glyphs) {
            this.x += glyph.x * glyph.n;
            this.y += glyph.y * glyph.n;
            this.n += glyph.n;
            this.track = (this.track || glyph.track);
        }
        this.x /= this.n;
        this.y /= this.n;
    }

    /**
     * @see #Glyph(Iterable)
     */
    public Glyph(Glyph... glyphs) {
        this(Arrays.asList(glyphs));
    }


    /**
     * Record another cell intersecting the glyph.
     *
     * @param cell Cell to be added.
     */
    public void addCell(QuadTree cell) {
        if (!cells.contains(cell)) {
            cells.add(cell);
        }
    }

    /**
     * Given some {@linkplain #isBig() big} glyph, adopt the uncertain merge
     * events of that glyph onto this one. This will clear the queue of uncertain
     * merge events on {@code bigGlyph}.
     *
     * @param bigGlyph Glyph of which to adopt the uncertain merge events.
     * @param event The merge event that caused the need for adoption.
     */
    public void adoptUncertainMergeEvents(Glyph bigGlyph, GlyphMerge event) {
        if (!isBig() || !bigGlyph.isBig()) {
            throw new RuntimeException("both this glyph and bigGlyph must be big");
        }
        if (uncertainMergeEvents.size() > 0) {
            throw new RuntimeException("can only adopt when it has no events yet");
        }

        UncertainQueue old = uncertainMergeEvents;
        uncertainMergeEvents = bigGlyph.uncertainMergeEvents;
        uncertainMergeEvents.updateAlpha(event);
        bigGlyph.uncertainMergeEvents = old;
        bigGlyph.adoptedBy = this;
    }

    /**
     * Returns this glyph, or if it is big and its uncertain merge events have
     * been {@linkplain #adoptUncertainMergeEvents(Glyph) adopted}, the glyph
     * that adopted them, or the glyph that adopted them from that glyph, et
     * cetera, until a glyph is found that did not have its events adopted yet.
     */
    public Glyph getAdoptivePrimalParent() {
        if (adoptedBy == null) {
            return this;
        }
        return adoptedBy.getAdoptivePrimalParent();
    }

    /**
     * Returns all recorded cells intersecting the glyph.
     */
    public List<QuadTree> getCells() {
        return cells;
    }

    /**
     * Returns the number of entities represented by the glyph.
     */
    public int getN() {
        return n;
    }

    /**
     * Returns the X-coordinate of the center of the glyph.
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the Y-coordinate of the center of the glyph.
     */
    public double getY() {
        return y;
    }

    /**
     * Hash only the location of the glyph, for performance reasons.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Returns whether this glyph and the given one share both X- and Y-
     * coordinates. This is checked using
     * {@link Utils.Double#eq(double, double)}, so with an epsilon.
     *
     * @param that Glyph to consider.
     */
    public boolean hasSamePositionAs(Glyph that) {
        return (Utils.Double.eq(this.x, that.x) &&
                Utils.Double.eq(this.y, that.y));
    }

    /**
     * Returns whether this glyph is still taking part in the clustering process.
     */
    public boolean isAlive() {
        return (alive == 1);
    }

    /**
     * Returns whether this glyph is considered to be a big glyph, meaning that
     * at the time of its construction, it was representing more than {@link
     * Constants.D#BIG_GLYPH_FACTOR} times the average number of entities.
     *
     * <p>Glyphs initially are not big, but can be determined to be big when they
     * {@link #participate(Stat)} later - this is true for merged glyphs in the
     * {@link QuadTreeClusterer}.
     */
    public boolean isBig() {
        return big;
    }

    /**
     * Implement strict equality.
     */
    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }

    /**
     * Returns at which zoom level this glyph touches a static rectangle, given a
     * specific {@link GrowFunction} to be used for scaling the glyph.
     *
     * @param rect Rectangle to determine intersection with.
     * @param g Function to be used for scaling the glyph.
     */
    public double intersects(Rectangle2D rect, GrowFunction g) {
        return g.intersectAt(this, rect);
    }

    /**
     * Returns at which zoom level this glyph touches another glyph, given a
     * specific {@link GrowFunction} to be used for scaling both glyphs.
     *
     * @param that glyph to determine intersection with.
     * @param g Function to be used for scaling both glyphs.
     */
    public double intersects(Glyph that, GrowFunction g) {
        return g.intersectAt(this, that);
    }

    /**
     * Marks this glyph as alive: participating in the clustering process.
     */
    public void participate() {
        if (this.alive == 1) {
            throw new RuntimeException("having a participating glyph participate");
        }
        if (this.alive == 2) {
            throw new RuntimeException("cannot bring a perished glyph back to life");
        }
        this.alive = 1;
    }

    /**
     * Returns the first merge event that will occur with this big glyph, or
     * {@code null} if there is none remaining.
     */
    public UncertainGlyphMerge peekUncertain() {
        return uncertainMergeEvents.peek();
    }

    /**
     * Marks this glyph as not alive: no longer participating in the clustering
     * process.
     */
    public void perish() {
        this.alive = 2;
    }

    /**
     * Same as {@link #peekUncertain()}, but actually removes that event from
     * the internal queue it is stored in.
     */
    public UncertainGlyphMerge pollUncertain() {
        return uncertainMergeEvents.poll();
    }

    /**
     * Add the next event, if any, to the given queue. This will add the first
     * {@link #record(GlyphMerge) recorded} event to the given queue.
     *
     * @param q Event queue to add {@link GlyphMerge} to.
     * @param l Logger to log events to. Can be {@code null}.
     * @return Whether an event was popped into the queue.
     */
    public boolean popMergeInto(Queue<Event> q, Logger l) {
        if (this.big) {
            throw new RuntimeException("big glyphs don't pop merge events "
                    + "into the shared queue");
        }

        // try to pop a merge event into the queue as long as the previously
        // recorded merge is with a glyph that is still alive... give up as
        // soon as no recorded events remain
        while (!mergeEvents.isEmpty()) {
            GlyphMerge merge = mergeEvents.poll();
            Glyph with = merge.getOther(this);
            if (!with.isAlive() || with.isBig()) {
                continue; // try the next event
            }
            q.add(merge);
            if (B.TRACK.get() && !B.ROBUST.get()) {
                if (!with.trackedBy.contains(this)) {
                    with.trackedBy.add(this);
                }
            }
            if (l != null) {
                l.log(Level.FINEST, "→ merge at {0} with {1}",
                        new Object[] {merge.getAt(), with});
            }
            // we found an event and added it to the queue, return
            return true;
        }
        // no recorded events remain, we cannot add an event
        return false;
    }

    /**
     * Add the next event, if any, to the given queue. This will add the first
     * {@link #record(OutOfCell) recorded} event to the given queue.
     *
     * @param q Event queue to add {@link OutOfCell} to.
     * @param l Logger to log events to, can be {@code null}.
     * @return Whether an event was popped into the queue.
     */
    public boolean popOutOfCellInto(Queue<Event> q, Logger l) {
        if (this.big) {
            throw new RuntimeException("big glyphs don't pop out of cell events "
                    + "into the shared queue");
        }

        boolean added = false;
        while (!outOfCellEvents.isEmpty()) {
            OutOfCell o = outOfCellEvents.poll();
            if (l != null) {
                l.log(Level.FINEST, "popping {0} into the queue", o);
            }
            q.add(o);
            added = true;
            if (!B.ROBUST.get()) {
                return true;
            }
        }
        if (B.ROBUST.get() && added) {
            return true;
        }
        if (l != null) {
            l.log(Level.FINEST, "no out of cell event to pop");
        }
        return false;
    }

    /**
     * Acknowledge that the given event will happen.
     *
     * @param event Event involving this glyph.
     */
    public void record(GlyphMerge event) {
        mergeEvents.add(event);
    }

    /**
     * Acknowledge that the given event will happen.
     *
     * @param event Event involving this glyph.
     */
    public void record(UncertainGlyphMerge event) {
        uncertainMergeEvents.add(event);
    }

    /**
     * Acknowledge that the given event will happen.
     *
     * @param event Event involving this glyph.
     */
    public void record(OutOfCell event) {
        outOfCellEvents.add(event);
    }

    /**
     * Record a cell no longer intersecting the glyph.
     *
     * @param cell Cell to be removed.
     */
    public void removeCell(QuadTree cell) {
        cells.remove(cell);
    }

    /**
     * Update whether this glyph is big using the given statistic.
     *
     * @param glyphSize Statistic covering number of entities represented by
     *            other glyphs, used to determine if this glyph {@link #isBig()}.
     * @param g GrowFunction that is used to {@linkplain UncertainGlyphMerge#
     *            computeAt(GrowFunction) compute} when events happen if this
     *            glyph turns out to be a big one.
     */
    public void setBig(Stat glyphSize, GrowFunction g) {
        if (!B.BIG_GLYPHS.get()) {
            return;
        }

        this.big = (this.n > glyphSize.getAverage() * D.BIG_GLYPH_FACTOR.get());
        Stats.count("glyph was big when it participated", this.big);

        // if the glyph is big, initialize uncertain merge event tracking
        if (this.big) {
            this.uncertainMergeEvents = new UncertainQueue(g);
        }
    }

    /**
     * Change number of entities represented by this glyph. Should not normally
     * be used, only used by {@link BlowUp} at the moment.
     *
     * @param n New number of entities.
     */
    public void setN(int n) {
        this.n = n;
    }

    @Override
    public String toString() {
        return String.format("glyph [x = %.2f, y = %.2f, n = %d]", x, y, n);
    }

}
