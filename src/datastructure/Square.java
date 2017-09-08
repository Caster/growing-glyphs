package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A square starts as a point and then grows at a given speed.
 */
public class Square {

    /**
     * X-coordinate of the center of the square.
     */
    private double x;
    /**
     * Y-coordinate of the center of the square.
     */
    private double y;
    /**
     * Number of entities represented by the square.
     */
    private int n;
    /**
     * Set of QuadTree cells that this square intersects.
     */
    private Set<QuadTree> cells;


    /**
     * Construct a new square at the given coordinates, with given growing speed.
     *
     * @param x X-coordinate of the center of the square.
     * @param y Y-coordinate of the center of the square.
     * @param n Number of entities represented by the square.
     * @throws IllegalArgumentException When n < 1.
     */
    public Square(double x, double y, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be at least 1");
        }
        this.x = x;
        this.y = y;
        this.n = n;
        this.cells = new HashSet<>();
    }

    /**
     * Construct a new square that has its center at the weighted average of the
     * centers of the given squares, and the sum of their weights.
     *
     * @param squares Squares to construct a new square out of.
     */
    public Square(Iterable<Square> squares) {
        this(0, 0, 1);
        this.n = 0;
        for (Square square : squares) {
            this.x += square.x * square.n;
            this.y += square.y * square.n;
            this.n += square.n;
        }
        this.x /= this.n;
        this.y /= this.n;
    }

    /**
     * @see #Square(Iterable)
     */
    public Square(Square... squares) {
        this(Arrays.asList(squares));
    }


    /**
     * Record another cell intersecting the square.
     *
     * @param cell Cell to be added.
     */
    public void addCell(QuadTree cell) {
        cells.add(cell);
    }

    /**
     * Returns all recorded cells intersecting the square.
     */
    public Set<QuadTree> getCells() {
        return cells;
    }

    /**
     * Returns the number of entities represented by the square.
     */
    public int getN() {
        return n;
    }

    /**
     * Returns the X-coordinate of the center of the square.
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the Y-coordinate of the center of the square.
     */
    public double getY() {
        return y;
    }

    /**
     * Returns at which zoom level this square touches a static rectangle, given a
     * specific {@link GrowFunction} to be used for scaling the square.
     *
     * @param rect Rectangle to determine intersection with.
     * @param g Function to be used for scaling the square.
     */
    public double intersects(Rectangle2D rect, GrowFunction g) {
        return g.intersectAt(this, rect);
    }

    /**
     * Returns at which zoom level this square touches another square, given a
     * specific {@link GrowFunction} to be used for scaling both squares.
     *
     * @param that Square to determine intersection with.
     * @param g Function to be used for scaling both squares.
     */
    public double intersects(Square that, GrowFunction g) {
        return g.intersectAt(this, that);
    }

    /**
     * Record a cell no longer intersecting the square.
     *
     * @param cell Cell to be removed.
     */
    public void removeCell(QuadTree cell) {
        cells.remove(cell);
    }

    @Override
    public String toString() {
        return String.format("Square [x = %.2f, y = %.2f, n = %d]", x, y, n);
    }

}
