package utils;

public class Timer {

    private int count;
    private boolean running;
    private long started;
    private long totalElapsed;

    /**
     * Construct a new timer, and immediately start it.
     */
    public Timer() {
        this.count = 0;
        this.totalElapsed = 0;
        start();
    }


    /**
     * Returns how much time passed since this timer was last started.
     */
    public long getElapsed() {
        return Utils.Timers.now() - started;
    }

    /**
     * Returns how much time has passed between all start and stop events on
     * this timer. When the timer is currently running, that time is <em>not</em>
     * included in this value.
     */
    public long getElapsedTotal() {
        return totalElapsed;
    }

    /**
     * Returns how many times this timer was stopped.
     */
    public int getNumCounts() {
        return count;
    }

    /**
     * Start the timer. Starting a running timer has no effect.
     */
    public void start() {
        if (running) {
            return;
        }
        started = Utils.Timers.now();
        running = true;
    }

    /**
     * Stop the timer, record time passed since last start event. Stopping a
     * stopped timer has no effect.
     */
    public void stop() {
        if (!running) {
            return;
        }
        totalElapsed += getElapsed();
        count++;
        running = false;
    }

}
