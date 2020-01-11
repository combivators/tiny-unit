package net.tiny.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@Benchmark
public class BenchmarkClassTest {

    @Test
    public void benchmarkedOne() throws InterruptedException {
     Thread.sleep(100);
     assertTrue(true);
    }

    @Test
    public void benchmarkedTwo() throws InterruptedException {
     Thread.sleep(50);
     assertTrue(true);
    }
}
