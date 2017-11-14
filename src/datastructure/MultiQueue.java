package datastructure;

import java.util.PriorityQueue;

import datastructure.events.Event;
import utils.Utils.Stats;
import utils.Utils.Timers;

/**
 * {@link PriorityQueue} that keeps track of the number of insertions and
 * deletions into/from it. Can be asked for these stats too.
 *
 * This queue can also split itself into a variable number of queues of bounded
 * size. It acts as a single queue to users at all times.
 */
public class MultiQueue extends PriorityQueue<Event> {

    private static final int INSERTION = 0;
    private static final int DELETION = 1;
    private static final int DISCARD = 2;


    /**
     * Counts in the order insertions, deletions, discards.
     */
    private int[] counts;
    /**
     * Number in queue chain. First queue has ID 0, then 1, ...
     */
    private int id;
    /**
     * Pointer to next queue, when split.
     */
    private MultiQueue next;
    /**
     * Pointer to previous queue, if there is one.
     */
    private MultiQueue prev;
    /**
     * Threshold for when a split should occur. After that, events are only
     * accepted into this queue when there is room, other events are forwarded
     * to the next queue (which can again forward).
     */
    private int splitAt;


    /**
     * Construct a {@link MultiQueue} that has at most {@code splitAt} elements.
     *
     * @param splitAt When at least {@code 1}, this queue will split into
     *            smaller queues each having at most this many elements.
     */
    public MultiQueue(int splitAt) {
        this(splitAt, null);
    }

    private MultiQueue(int splitAt, MultiQueue previous) {
        this.counts = (previous == null ? new int[3] : null);
        this.id = (previous == null ? 0 : previous.id + 1);
        this.next = null;
        this.prev = previous;
        this.splitAt = splitAt;
    }


    /**
     * Returns the number of deletions from this queue.
     */
    public int getDeletions() {
        return getCount(DELETION);
    }

    /**
     * Returns the number of elements that was discarded.
     */
    public int getDiscarded() {
        return getCount(DISCARD);
    }

    /**
     * Returns the number of elements that was added.
     */
    public int getInsertions() {
        return getCount(INSERTION);
    }


    @Override
    public boolean add(Event e) {
        count(INSERTION);
        Timers.start("queue operations");
        // need to split?
        if (super.size() == splitAt) {
            // not split yet? do so!
            if (next == null) {
                next = new MultiQueue(splitAt, this);
                return next.add(e);
            }
            // already split, delegate
            return next.add(e);
        }
        // not split yet, or element can go here anyway?
        boolean t = super.add(e);
        Timers.stop("queue operations");
        Stats.record("queue size", size());
        Stats.record(e.getType().toString(), 1);
        return t;
    }

    public void discard() {
        count(DISCARD);
        poll(" discarded");
    }

    @Override
    public Event peek() {
        Event e = super.peek();
        if (next != null) {
            Event f = next.peek();
            if (f.getAt() < e.getAt()) {
                e = f;
            }
        }
        return e;
    }

    @Override
    public Event poll() {
        count(DELETION);
        return poll(" handled");
    }

    @Override
    public int size() {
        return id * splitAt + sizeNoPrev();
    }


    private void count(int index) {
        if (counts == null) {
            prev.count(index);
        } else {
            counts[index]++;
        }
    }

    private int getCount(int index) {
        if (counts == null) {
            return prev.getCount(index);
        }
        return counts[index];
    }

    private Event poll(String type) {
        Timers.start("queue operations");
        Event e = peek();
        poll(e);
        Timers.stop("queue operations");
        Stats.record("queue size", size());
        Stats.record(e.getType().toString() + type, 1);
        return e;
    }

    private void poll(Event e) {
        if (super.peek() == e) {
            super.poll();
            return;
        }
        // the way we found `e` means that there must be a `next` now
        next.poll(e);
    }

    private int sizeNoPrev() {
        return super.size() + (next == null ? 0 : next.sizeNoPrev());
    }

}