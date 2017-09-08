package datastructure.events;

import datastructure.QuadTree;
import datastructure.Square;

/**
 * An event occurs while {@link Square squares} stored in a {@link QuadTree} are
 * growing. This interface makes it easy to group all events in a single queue.
 */
public abstract class Event implements Comparable<Event> {

    /**
     * Different types of events are captured by this enumeration.
     */
    public enum Type {
        /**
         * Event type to be used when a square grew to one of the borders of a
         * cell it is associated with.
         */
        OUT_OF_CELL,
        /**
         * Event type to be used when two or more squares grew to touch.
         */
        MERGE
    };


    /**
     * Timestamp/zoom level at which the event occurs.
     */
    protected final double at;
    /**
     * Square(s) involved in the event.
     */
    protected final Square[] squares;


    /**
     * Construct an event that occurs at the given timestamp/zoom level.
     *
     * @param at Timestamp/zoom level at which event occurs.
     */
    public Event(double at, int squareCapacity) {
        this.at = at;
        this.squares = new Square[squareCapacity];
    }

    @Override
    public int compareTo(Event o) {
        return (int) Math.signum(at - o.at);
    }

    /**
     * Returns when the event occurs. This can be interpreted either as a
     * timestamp or as a zoom level.
     */
    public double getAt() {
        return at;
    }

    /**
     * Returns the number of squares involved in this event.
     */
    public int getSize() {
        return squares.length;
    }

    /**
     * Returns the square(s) involved in this event.
     */
    public Square[] getSquares() {
        return squares;
    }

    /**
     * Returns the {@link Type} of this {@link Event}.
     */
    public abstract Type getType();

}
