package net.tiny.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

public class BenchmarkedThreadsTest {
    static Random rand = new Random();

    @Benchmark(threads = 10)
    public void testBenchmarkThreads() throws InterruptedException {
        final long delay = rand.nextInt((250 - 150) + 1) + 150;
        Thread.sleep(delay);
        assertTrue(true);
    }
}
