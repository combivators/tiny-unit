package net.tiny.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BenchmarkedEachTest {

    static long start;

    @BeforeEach
    public void beforeEach() throws Exception {
        start = System.currentTimeMillis();
        System.out.println("[beforeEach] " + start);
    }

    @AfterEach
    public void afterEach() throws Exception {
        final long end = System.currentTimeMillis();
        System.out.println("[afterEach] Takes time " + (end - start) + "ms");
    }

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
}
