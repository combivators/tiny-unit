package net.tiny.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class BenchmarkedTest {

    @Test
    public void testNormalCase() throws InterruptedException {
        Thread.sleep(10);
    }

    @Benchmark
    public void benchmarked() throws InterruptedException {
        Thread.sleep(100);
    }

    @Benchmark
    public void benchmarkedTwice() throws InterruptedException {
        Thread.sleep(200);
        assertTrue(true);
    }

    private TestTask task = new TestTask();

    @Benchmark(warmup=10, measure=1000, trace=true)
    public void testTask() throws InterruptedException {
        task.norm();
    }


    static class TestTask {
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
