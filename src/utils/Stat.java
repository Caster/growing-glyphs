package utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Stat {

    private int n;
    private double average;
    private double min;
    private double max;
    private double sum;


    public Stat() {
        this.n = 0;
    }

    public Stat(double value) {
        this.n = 1;
        this.average = this.min = this.max = this.sum = value;
    }

    public double getAverage() {
        return average;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public int getN() {
        return n;
    }

    public double getSum() {
        return sum;
    }

    public void log(Logger logger, String name) {
        if (min == max) {
            logger.log(Level.FINE, "{0} was {1} and did not change over {2} "
                    + "measurement{3}", new Object[] {
                    name, String.format("%,13.2f", max), n, (n == 1 ? "" : "s")});
        } else {
            logger.log(Level.FINE, "{0} was {1} on average and always between "
                    + "{2} and {3}, over {4} measurement{5}", new Object[] {
                    name, String.format("%,13.2f", average), min, max, n,
                    (n == 1 ? "" : "s")});
        }
    }

    public void logCount(Logger logger, String name) {
        logger.log(Level.FINE, "{0} occurred {1}", new Object[] {name,
                (n == 1 ? "once" : n + " times")});
    }

    public void record(double value) {
        if (n == 0) {
            this.n = 1;
            this.average = this.min = this.max = this.sum = value;
            return;
        }
        average += (value - average) / ++n;
        sum += value;
        if (value > max) {
            max = value;
        }
        if (value < min) {
            min = value;
        }
    }

}
