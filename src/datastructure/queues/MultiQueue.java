package datastructure.queues;

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
 *
 * @see BucketingStrategy
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
     * Initial capacity of the queue.
     */
    private int initialCapacity;
    /**
     * Pointer to next queue, when split.
     */
    private MultiQueue next;
    /**
     * Start (inclusive) of timestamp range of this queue, when using
     * {@link BucketingStrategy#ON_TIMESTAMP}. Cached for efficiency.
     */
    private double rangeStart;
    /**
     * End (exclusive) of timestamp range of this queue, when using
     * {@link BucketingStrategy#ON_TIMESTAMP}. Cached for efficiency.
     */
    private double rangeEnd;
    /**
     * Pointer to first queue of all queues.
     */
    private MultiQueue root;
    /**
     * Threshold for when a split should occur.
     */
    private BucketingStrategy splitAt;


    /**
     * Construct a {@link MultiQueue} that has at most {@code splitAt} elements.
     *
     * @param splitAt Strategy for splitting. See {@link BucketingStrategy} for
     *            details about the various strategies.
     * @param capacity Initial capacity of the queue.
     */
    public MultiQueue(BucketingStrategy splitAt, int capacity) {
        this(splitAt, null, capacity);
    }

    private MultiQueue(BucketingStrategy splitAt, MultiQueue previous) {
        this(splitAt, previous, previous.initialCapacity);
    }

    private MultiQueue(BucketingStrategy splitAt, MultiQueue previous, int capacity) {
        super(capacity);
        if (splitAt != BucketingStrategy.NO_BUCKETING &&
                splitAt.getThreshold() == null) {
            throw new RuntimeException(splitAt + " strategy needs to have a "
                    + "threshold set");
        }

        this.counts = (previous == null ? new int[3] : null);
        this.id = (previous == null ? 0 : previous.id + 1);
        this.initialCapacity = capacity;
        this.next = null;
        this.rangeStart = Double.NaN;
        this.rangeEnd = Double.NaN;
        this.root = (previous == null ? this : previous.root);
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

    public int getNumQueues() {
        MultiQueue q = this;
        while (q.next != null) {
            q = q.next;
        }
        return q.id + 1;
    }


    @Override
    public boolean add(Event e) {
        count(INSERTION);
        if (this == root) {
            Timers.start("queue operations");
        }
        // need to split?
        switch (splitAt) {
        case ON_SIZE:
            if (super.size() == splitAt.getThresholdI()) {
                return addToNext(e);
            }
            break;
        case ON_TIMESTAMP:
            if (!isTimestampInRange(e)) {
                return addToNext(e);
            }
            break;
        default:
            // no splitting needed
            break;
        }
        // not split yet, or element can go here anyway?
        boolean t = super.add(e);
        if (this == root) {
            Timers.stop("queue operations");
            Stats.record(e.getType().toString(), 1);
        }
        return t;
    }

    public void discard() {
        count(DISCARD);
        poll(" discarded");
    }

    @Override
    public Event peek() {
        Event e = super.peek();
        // when bucketing on timestamp, we can stop as soon as we find something
        if (e != null && splitAt == BucketingStrategy.ON_TIMESTAMP) {
            return e;
        }
        // try to find something better in next queue(s)
        if (next != null) {
            Event f = next.peek();
            if (e == null || (f != null && f.getAt() < e.getAt())) {
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
        if (splitAt == BucketingStrategy.ON_SIZE) {
            return id * splitAt.getThresholdI() + sizeNoPrev();
        }
        return root.sizeNoPrev();
    }


    /**
     * Delegate adding an event to the next queue. Split queue if necessary.
     */
    private boolean addToNext(Event e) {
        if (next == null) {
            next = new MultiQueue(splitAt, this);
        }
        return next.add(e);
    }

    private void count(int index) {
        root.counts[index]++;
    }

    private int getCount(int index) {
        return root.counts[index];
    }

    private boolean isTimestampInRange(Event e) {
        double at = e.getAt();
        if (Double.isNaN(rangeStart)) {
            double growth = splitAt.getGrowthD();
            double gId = Math.pow(growth, id);
            rangeStart = splitAt.getThresholdD() * ((1 - gId) / (1 - growth));
            rangeEnd = rangeStart + splitAt.getThresholdD() * gId;
            if (rangeEnd > splitAt.getLimitD()) {
                rangeEnd = Double.POSITIVE_INFINITY;
            }
        }
        return (rangeStart <= at && at < rangeEnd);
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