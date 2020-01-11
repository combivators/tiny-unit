package net.tiny.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class StatisticsTest {

    private double c = 0.0f;

    private final double[] newDouble(int dim) {
        c +=1.0f;
        double[] values = new double[dim];
        for(int i=0; i<dim; i++) {
            values[i] = c + (double)i;
        }
        return values;
    }

    @Test
    public void testMath() {
        double[] data={5.0, 4.3, 8.7, 4.3, 8.2, 1.3, 9.1, 10.8};

        Statistics statistics = new Statistics();
        for(int i=0; i<data.length; i++) {
            statistics.push(data[i]);
        }
        assertEquals(8, statistics.count());
        assertEquals(1.3d, statistics.min());
        assertEquals(10.8d, statistics.max());
        assertEquals(6.600000000000001d, statistics.mean());
        assertEquals(6.4625d, statistics.average());
        assertEquals(5.0d, statistics.median());
        assertEquals(51.7d, statistics.sum());
        assertEquals(13.644309949884349d, statistics.log());
        assertEquals(406.04999999999995d, statistics.sqr());
        assertEquals(842652.5136119999d, statistics.product());
        assertEquals(8.99234375d, statistics.variance());
        assertEquals(2.998723686837452d, statistics.sdev());
        assertEquals(5.504350302794234d, statistics.geometricMean(), 0.00001d);

        System.out.println(statistics.toString());
    }

    @Test
    public void testOneDate() {
        Statistics statistics = new Statistics();
        statistics.push(5.0d);
        assertEquals(1, statistics.count());
        assertEquals(5.0d, statistics.min());
        assertEquals(5.0d, statistics.max());
        assertEquals(5.0d, statistics.mean());
        assertEquals(5.0d, statistics.average());
        assertEquals(5.0d, statistics.median());
        assertEquals(5.0d, statistics.sum());
        assertEquals(1.6094379124341003d, statistics.log());
        assertEquals(25.0d, statistics.sqr());
        assertEquals(5.0, statistics.product());
        assertEquals(0.0d, statistics.variance());
        assertEquals(0.0d, statistics.sdev());
        assertEquals(5.0d, statistics.geometricMean());

        System.out.println(statistics.toString());
    }

    @Test
    public void testMedian() {
        double[] data = {5.0, 4.3, 8.7, 6.2, 4.3, 8.2, 1.3, 9.1, 10.8};
        Statistics statistics = new Statistics();
        for(int i=0; i<data.length; i++) {
            statistics.push(data[i]);
        }
        assertEquals(5.0d, statistics.median());

        statistics = Statistics.load(newDouble(1000));
        System.out.println(statistics.toString());
    }

    @Test
    public void testNormalize() {
        double [] data={5.0,4.3,8.7,4.3,8.2,1.3,9.1,10.8};
        Statistics statistics = new Statistics();
        for(int i=0; i<data.length; i++) {
            statistics.push(data[i]);
        }
        double[] n = statistics.normalize();
        for(double d : n) {
            System.out.println(" " + d);
        }
    }
}
