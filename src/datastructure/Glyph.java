package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import algorithm.AgglomerativeClustering;
import datastructure.growfunction.GrowFunction;

/**
 * A glyph starts as a point and then grows at a given speed.
 */
public class Glyph {

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
            this.trackedBy = new HashSet<>(1);
        } else {
            this.trackedBy = null;
        }
        this.x = x;
        this.y = y;
        this.n = n;
        this.cells = new HashSet<>();
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
