package org.example.map;

public class StationData {
    private int count;
    private double sum;

    public void incrementCount() {
        count++;
    }

    public void addToSum(double value) {
        sum += value;
    }

    public int getCount() {
        return count;
    }

    public double getSum() {
        return sum;
    }

    @Override
    public String toString() {
        return count + " - " + sum;
    }
}
