package datastructure.growfunction;

import java.awt.geom.Rectangle2D;
import java.util.List;

import datastructure.QuadTree;
import datastructure.Square;
import datastructure.events.OutOfCell.Side;

/**
 * Function determining how {@link Square Squares} should be scaled.
 */
public abstract class GrowFunction {

    /**
     * Returns at which zoom level a square touches the given side of the given
     * cell. The square is scaled using this {@link GrowFunction}.
     *
     * @param square Growing square.
     * @param cell Cell that square is assumed to be inside of, altough if not
     *            the time of touching is still correctly calculated.
     * @param side Side of cell for which calculation should be done.
     * @return Zoom level at which {@code square} touches {@code side} side of
     *         {@code cell}.
     */
    public abstract double exitAt(Square square, QuadTree cell, Side side);

    /**
     * Returns at which zoom level two squares will touch. Both squares are
     * scaled using this {@link GrowFunction}.
     *
     * @param a First square.
     * @param b Second square.
     * @return Zoom level at which {@code a} and {@code b} touch.
     */
    public abstract double intersectAt(Square a, Square b);

    /**
     * Returns at which zoom level a square touches a static rectangle. The
     * square is scaled using this {@link GrowFunction}.
     *
     * @param r Static rectangle.
     * @param s Growing square.
     * @return Zoom level at which {@code r} and {@code s} touch.
     */
    public abstract double intersectAt(Rectangle2D r, Square s);

    /**
     * Same as {@link #intersectAt(Rectangle2D, Square), just with different order
     * of parameters. This is a convenience function.
     */
    public double intersectAt(Square s, Rectangle2D r) {
        return intersectAt(r, s);
    }

    /**
     * Returns a rectangle representing the square at the given time stamp/zoom
     * level, according to this grow function.
     *
     * @param s Square to compute the size of.
     * @param at Time stamp or zoom level at which size must be computed.
     * @return A rectangle representing the square at time/zoom {@code at}.
     */
    public abstract Rectangle2D sizeAt(Square s, double at);

    public Rectangle2D[] sizesAt(double at, Square... squares) {
        Rectangle2D[] result = new Rectangle2D[squares.length];
        for (int i = 0; i < squares.length; ++i) {
            result[i] = sizeAt(squares[i], at);
        }
        return result;
    }

    public Rectangle2D[] sizesAt(double at, List<Square> squares) {
        return this.sizesAt(at, squares.toArray(new Square[0]));
    }

}
