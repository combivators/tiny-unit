package net.tiny.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class BenchemarkerTest {

    @Test
    public void testStartStop() throws Exception {
        Benchmarker bench = new Benchmarker();
        bench.start();
        while (bench.loop()) {
            bench.trace("Test", System.out);
        }
        bench.stop();
        bench.metric("Test Result", System.out);
        assertEquals(Benchmarker.TOTAL, bench.count());

        bench.reset();
        bench.start(10000L, 1000L);
        while (bench.loop()) {
            bench.trace(System.out);
        }
        bench.stop();
        bench.metric(System.out);
        assertEquals(10000L, bench.count());
    }

    @Test
    public void testRun() throws Exception {
        Benchmarker bench = new Benchmarker();
        TestTask task = new TestTask();
        long start = System.nanoTime();
        //long start = System.currentTimeMillis();
        task.norm();
        long finish = System.nanoTime();
        //long finish = System.currentTimeMillis();
        long elapsed = finish - start;
        System.out.println("norm : " + elapsed + "ns");
        System.out.println();

        bench.start();
        task.norm();
        bench.stop();
        bench.metric(System.out);

        bench.reset();
        Method method = TestTask.class.getDeclaredMethod("norm");
        bench.run(task, method);
        bench.metric(System.out);
    }
    @Test
    public void testRunTarget() throws Exception {
        Benchmarker bench = new Benchmarker();
        TestTask task = new TestTask();
        Method method = TestTask.class.getDeclaredMethod("norm");
        bench.target(task, method)
            .run("UNIT", 10000, 1000);
        bench.metric(System.out);
    }

    class TestTask {
        private float c = 0.0f;

        private final float[] newFloat(int dim) {
            c +=1.0f;
            float[] values = new float[dim];
            for(int i=0; i<dim; i++) {
                values[i] = c + (float)i;
            }
            return values;
        }

        public float norm() throws InterruptedException {
            float[] data = newFloat(1000);
            float sum = 0.0f;
            for (float v : data) {
                sum += v * v;
            }
            return (float) Math.sqrt(sum);
        }
    }
}
