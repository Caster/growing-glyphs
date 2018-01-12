package utils;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.Utils.Timers;
import utils.Utils.Timers.Units;

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
     * {@link #stop() Stop} this timer and log the {@link #getElapsedTotal()
     * total elapsed time} to the given logger instance, at level
     * {@link Level#FINE}.
     *
     * @param logger Logger to log to.
     * @param name Name of event that was timed.
     */
    public void log(Logger logger, String name) {
        log(logger, name, Level.FINE);
    }

    /**
     * {@link #stop() Stop} this timer and log the {@link #getElapsedTotal()
     * total elapsed time} to the given logger instance.
     *
     * @param logger Logger to log to.
     * @param name Name of event that was timed.
     * @param level Level to log at.
     */
    public void log(Logger logger, String name, Level level) {
        stop();
        if (logger != null) {
            logger.log(level, "{0} took {1} seconds (wall clock time{2})",
                new Object[] {name, String.format("%5.2f", Timers.in(
                        totalElapsed, Units.SECONDS)),
                (count == 1 ? "" : String.format(", %s timings",
                NumberFormat.getIntegerInstance().format(count)))});
        } else {
            System.out.println(String.format(
                    "%1$s took %2$5.3f seconds (wall clock time%3$s",
                    name, Timers.in(totalElapsed, Units.SECONDS),
                    (count == 1 ? "" : String.format(", %s timings",
                            NumberFormat.getIntegerInstance().format(count)))
                ));
        }
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
