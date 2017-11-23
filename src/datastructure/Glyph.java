package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import algorithm.AgglomerativeClustering;
import algorithm.FirstMergeRecorder;
import datastructure.events.Event;
import datastructure.events.GlyphMerge;
import datastructure.events.OutOfCell;
import datastructure.growfunction.GrowFunction;

/**
 * A glyph starts as a point and then grows at a given speed.
 */
public class Glyph {

    /**
     * Number of merge events that a glyph will record at most. This is not
     * strictly enforced by the glyph itself, but should be respected by the
     * {@link FirstMergeRecorder} and other code that records merges.
     *
     * More merges can be recorded with a glyph when many merges occur at the
     * exact same time.
     */
    public static final int MAX_MERGES_TO_RECORD = 3;


    /**
     * Used by clustering algorithm to track which glyphs are still of interest.
     */
    public boolean alive;
    /**
     * Used by the clustering algorithm to track which glyphs think they'll merge
     * with {@code this} glyph before merging with any other glyph.
     */
    public Set<Glyph> trackedBy;

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
     * Set of QuadTree cells that this glyph intersects.
     */
    private Set<QuadTree> cells;
    /**
     * Events involving this glyph. Only one event is actually in the event
     * queue, others are added only when that one is popped from the queue.
     */
    private Queue<GlyphMerge> mergeEvents;
    /**
     * Events involving this glyph. Only one event is actually in the event
     * queue, others are added only when that one is popped from the queue.
     */
    private Queue<OutOfCell> outOfCellEvents;


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
     * Construct a new glyph at the given coordinates, with given growing speed.
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
        this.alive = alive;
        if (AgglomerativeClustering.TRACK) {
            this.trackedBy = new HashSet<>(16);
        } else {
            this.trackedBy = null;
        }
        this.x = x;
        this.y = y;
        this.n = n;
        this.cells = new HashSet<>();
        this.mergeEvents = new PriorityQueue<>(MAX_MERGES_TO_RECORD);
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
        cells.add(cell);
    }

    /**
     * Returns all recorded cells intersecting the glyph.
     */
    public Set<QuadTree> getCells() {
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
     * Add the next event, if any, to the given queue. This will add the first
     * {@link #record(GlyphMerge) recorded} event to the given queue.
     *
     * @param q Event queue to add {@link GlyphMerge} to.
     * @param l Logger to log events to. Can be {@code null}.
     * @return Whether an event was popped into the queue.
     */
    public boolean popMergeInto(Queue<Event> q, Logger l) {
        if (!mergeEvents.isEmpty()) {
            GlyphMerge merge = mergeEvents.poll();
            q.add(merge);
            Glyph with = merge.getOther(this);
            if (AgglomerativeClustering.TRACK) {
                with.trackedBy.add(this);
            }
            if (l != null) {
                l.log(Level.FINEST, "-> merge at {0} with {1}",
                        new Object[] {merge.getAt(), with});
            }
            return true;
        }
        return false;
    }

    /**
     * Add the next event, if any, to the given queue. This will add the first
     * {@link #record(OutOfCell) recorded} event to the given queue.
     *
     * @param q Event queue to add {@link OutOfCell} to.
     * @return Whether an event was popped into the queue.
     */
    public boolean popOutOfCellInto(Queue<Event> q) {
        if (!outOfCellEvents.isEmpty()) {
            q.add(outOfCellEvents.poll());
            return true;
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

    @Override
    public String toString() {
        return String.format("glyph [x = %.2f, y = %.2f, n = %d]", x, y, n);
    }

}
