package utils;

public class Stat {

    private int n;
    private double average;
    private double min;
    private double max;
    private double sum;


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

    public void record(double value) {
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
