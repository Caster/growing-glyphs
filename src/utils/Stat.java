package utils;

public class Stat {

    private int n;
    private double average;
    private double sum;


    public Stat(double value) {
        this.n = 1;
        this.average = value;
        this.sum = value;
    }

    public double getAverage() {
        return average;
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
    }

}
