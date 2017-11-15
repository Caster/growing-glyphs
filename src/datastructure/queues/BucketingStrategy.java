package datastructure.queues;

import java.util.PriorityQueue;

import utils.Utils;

public enum BucketingStrategy {
    /**
     * Using this strategy in a {@link MultiQueue} means that the queue will
     * never split and actually be a single {@link PriorityQueue}.
     */
    NO_BUCKETING("NO BUCKETING"),
    /**
     * A {@link MultiQueue} using this strategy will split into smaller queues
     * each having at most the {@link #getThreshold() set threshold} many
     * elements in them.
     */
    ON_SIZE("BUCKETING ON SIZE", 10_000),
    /**
     * A {@link MultiQueue} using this strategy will split into a queue for
     * every timestamp range of size {@link #getThreshold() threshold}. For
     * example, setting the threshold to {@code 2} will create a queue for all
     * events happening in {@code [0, 2)}, one for {@code [2, 4)}, et cetera.
     */
    ON_TIMESTAMP("BUCKETING ON TIMESTAMP", 1e-6, 1.2, 3e-5);


    private Number growing;
    private String humanReadable;
    private Number limit;
    private Number threshold;


    private BucketingStrategy(String hr) {
        this(hr, null, null, null);
    }

    private BucketingStrategy(String hr, Number threshold) {
        this(hr, threshold, null, null);
    }

    private BucketingStrategy(String hr, Number threshold, Number growing, Number limit) {
        this.growing = growing;
        this.humanReadable = hr;
        this.limit = limit;
        this.threshold = threshold;
    }


    /**
     * Returns the value of the growth base as {@link #growing(Number) set} before.
     */
    public Number getGrowth() {
        return growing;
    }

    /**
     * Returns the {@code double} value of the {@link #getGrowth() growth}.
     */
    public double getGrowthD() {
        return growing.doubleValue();
    }

    /**
     * Returns the {@code int} value of the {@link #getGrowth() growth}.
     */
    public int getGrowthI() {
        return growing.intValue();
    }

    /**
     * Returns the value of the limit as {@link #upto(Number) set} before.
     */
    public Number getLimit() {
        return limit;
    }

    /**
     * Returns the {@code double} value of the {@link #getLimit() limit}.
     */
    public double getLimitD() {
        return limit.doubleValue();
    }

    /**
     * Returns the {@code int} value of the {@link #getLimit() limit}.
     */
    public int getLimitI() {
        return limit.intValue();
    }

    /**
     * Returns the value of the threshold as {@link #namely(Number) set} before.
     */
    public Number getThreshold() {
        return threshold;
    }

    /**
     * Returns the {@code double} value of the {@link #getThreshold() threshold}.
     */
    public double getThresholdD() {
        return threshold.doubleValue();
    }

    /**
     * Returns the {@code int} value of the {@link #getThreshold() threshold}.
     */
    public int getThresholdI() {
        return threshold.intValue();
    }

    /**
     * Changes the value of the growth base.
     *
     * @param growing Value for the growth base.
     * @return A self reference, for chaining.
     */
    public BucketingStrategy growing(Number growing) {
        this.growing = growing;
        return this;
    }

    /**
     * Changes the value of the threshold.
     *
     * @param threshold Value for the threshold.
     * @return A self reference, for chaining.
     */
    public BucketingStrategy namely(Number threshold) {
        this.threshold = threshold;
        return this;
    }

    @Override
    public String toString() {
        String format = "%1$s";
        if (threshold != null || growing != null || limit != null) {
            format += " (";
            format += Utils.join("; ",
                    (threshold == null ? "" : "threshold %2$.8f"),
                    (growing == null ? "" : "growth %3$.8f"),
                    (limit == null ? "" : "limit %4$.8f")
                );
            format += ")";
        }
        return String.format(format, humanReadable,
                (threshold == null ? 0 : threshold.doubleValue()),
                (growing == null ? 0 : growing.doubleValue()),
                (limit == null ? 0 : limit.doubleValue()))
                .replaceAll(",?0+([;)])", "$1");
    }

    /**
     * Changes the value of the limit.
     *
     * @param limit Value for the limit.
     * @return A self reference, for chaining.
     */
    public BucketingStrategy upto(Number limit) {
        this.limit = limit;
        return this;
    }

}
