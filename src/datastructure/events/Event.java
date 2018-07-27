package datastructure.events;

import datastructure.QuadTree;
import datastructure.Glyph;

/**
 * An event occurs while {@link Glyph glyphs} stored in a {@link QuadTree} are
 * growing. This interface makes it easy to group all events in a single queue.
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 * Events are compared on the time they occur, but different events may occur
 * at the exact same time.
 */
public abstract class Event implements Comparable<Event> {

    /**
     * Different types of events are captured by this enumeration.
     */
    public enum Type {
        /**
         * Event type to be used when a glyph grew to one of the borders of a
         * cell it is associated with.
         */
        OUT_OF_CELL(10),
        /**
         * Event type to be used when two or more glyphs grew to touch.
         */
        MERGE(0);


        private String cache = null;
        /**
         * Priority of the type, used when ordering events in a queue. Events are
         * ordered by the time they occur, but when they have the same timestamp,
         * events with lower priority are ordered before those with high priority.
         */
        private int priority;


        private Type(int priority) {
            this.priority = priority;
        }

        @Override
        public String toString() {
            if (cache == null) {
                cache = super.toString().toLowerCase().replaceAll("_", " ") +
                    " event";
            }
            return cache;
        }
    };


    /**
     * Timestamp/zoom level at which the event occurs.
     */
    protected final double at;
    /**
     * Glyph(s) involved in the event.
     */
    protected final Glyph[] glyphs;


    /**
     * Construct an event that occurs at the given timestamp/zoom level.
     *
     * @param at Timestamp/zoom level at which event occurs.
     */
    public Event(double at, int glyphCapacity) {
        this.at = at;
        this.glyphs = new Glyph[glyphCapacity];
    }

    @Override
    public int compareTo(Event that) {
        int diff = (int) Math.signum(this.at - that.at);
        if (diff != 0) {
            return diff;
        }
        return this.getType().priority - that.getType().priority;
    }

    /**
     * Returns when the event occurs. This can be interpreted either as a
     * timestamp or as a zoom level.
     */
    public double getAt() {
        return at;
    }

    /**
     * Returns the number of glyphs involved in this event.
     */
    public int getSize() {
        return glyphs.length;
    }

    /**
     * Returns the glyph(s) involved in this event.
     */
    public Glyph[] getGlyphs() {
        return glyphs;
    }

    /**
     * Returns the {@link Type} of this {@link Event}.
     */
    public abstract Type getType();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getType().toString());
        sb.append(" at ");
        sb.append(at);
        sb.append(" involving [");
        boolean first = true;
        for (Glyph glyph : glyphs) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(glyph.toString());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

}
