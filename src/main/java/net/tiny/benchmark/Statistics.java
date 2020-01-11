package net.tiny.benchmark;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.stream.Stream;

public class Statistics {

    private long count;
    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;
    private double sum = 0.0d;
    private double squares = 0.0d;
    private double last;
    private double avg;
    private double log = 0.0d;
    private double prod = 1.0d;
    private double mind = Double.MAX_VALUE;
    private double med = 0.0d;
    private ArrayDeque<Double> values = new ArrayDeque<>();

    public void push(double x) {
        sum += x;
        squares += x * x;
        min = ((x < min) ? x : min);
        max = ((x > max) ? x : max);
        last = x;
        ++count;
        avg = sum / count;
        final double d = Math.abs(x - avg);
        if (mind > d) {
            mind = d;
            med = x;
        }
        log += Math.log(x);
        prod *= x;
        values.add(x);
        // If the sum of squares exceeds Long.MAX_VALUE, this means the
        // value has overflowed; reset the state back to zero and start again.
        // All previous calculations are lost.  (Better as all doubles?)
        if (squares < 0L) {
            clear();
        }
    }

    public long count() {
        return count;
    }
    public double last() {
        return last;
    }
    //最小
    public double min() {
        return min;
    }
    //最大
    public double max() {
        return max;
    }
    //合计
    public double sum() {
        return sum;
    }
    public double sqr() {
        return squares;
    }
    //合计
    public double log() {
        return log;
    }
    public double product() {
        return prod;
    }
    //平均值
    public double mean() {
        if(count > 3)
            return (sum - max - min) / (count - 2);
        else
            return avg;
    }
    public double average() {
        return avg;
    }
    //几何平均
    public double geometricMean() {
        return Math.pow(product(), 1.0d/count);
    }
    //分散
    public double variance() {
        double var = 0.0d;
        for(double a :values) {
            double d = a - avg;
            var += d * d;
        }
        return var / (double)count;
    }
    //标准偏差
    public double sdev() {
        return Math.sqrt(variance());
    }
    //中间值
    public double median() {
        return med;
     }

    //正规化
    public double[] normalize() {
        double[] array = new double[values.size()];
        Double[] all = values.toArray(new Double[values.size()]);
        double d = sdev();
        double ave = average();
        for(int i=0; i<values.size(); i++) {
            array[i] = (all[i] - ave) / d;
        }
        return array;
    }

    public void clear() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        sum = 0.0d;
        squares = 0.0d;
        count = 0L;
        last = 0.0d;
        values.clear();
    }

    @Override
    public String toString() {
        final NumberFormat format = new DecimalFormat("0.###");
        return "{count=" + count +
                ", sum=" + sum +
                ", min=" + min +
                ", max=" + max +
                ", last=" + last +
                ", squares=" + format.format(squares) +
                ", mean=" + format.format(mean()) +
                ", sdev=" + format.format(sdev()) +
                '}';
    }

    public static Statistics load(double[] data) {
        Statistics statistics = new Statistics();
        for(double v : data)
            statistics.push(v);
        return statistics;
    }

    public static Statistics load(Stream<Double> stream) {
        Statistics statistics = new Statistics();
        stream.forEach(v -> statistics.push(v));
        stream.close();
        return statistics;
    }
}
