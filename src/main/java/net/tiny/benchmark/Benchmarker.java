package net.tiny.benchmark;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class Benchmarker {

    public static final long TOTAL = (long)2e6; //2,000,000
    private static final long TRACE_LENGTH = 100000L;
    private static final double NANO_PER_MILL = (double)1000000.0d;   //1.0E6;

    Statistics statistics = new Statistics();
    long total;
    long traces = TRACE_LENGTH;
    long count = 0L;
    long startTime;
    long startNanoTime;
    long msTime;
    long nanoTime;

    long stopTime;
    long stopNanoTime;
    long sec;
    long nsec;
    long ntime; // nano second
    double time; // second
    long lost;

    Supplier<Object> target;

    public void reset() {
        statistics.clear();;
        count = 0;
        time = 0.0d;
        ntime = 0L;
        lost = 0L;
    }

    public void start(long total) {
        start(total, TRACE_LENGTH);
    }

    public void start(long total, long traces) {
        assert(total > 0L);
        this.total = total;
        this.traces = traces;
        reset();
        this.startTime = System.currentTimeMillis();
        this.startNanoTime = System.nanoTime();
        this.msTime = startTime;
        this.nanoTime = startNanoTime;
    }
    public void start() {
        start(TOTAL);
    }
    public void restart() {
        this.startTime = System.currentTimeMillis();
        this.startNanoTime = System.nanoTime();
    }
    public void stop() {
        this.stopTime = System.currentTimeMillis();
        this.stopNanoTime = System.nanoTime();
        sec = (stopTime - startTime) / 1000L;
        nsec = stopNanoTime - startNanoTime;
        if (nsec < 0) {
            sec -= 1;
            nsec += 1000000000L;
        }
        time += (double)sec + (double)nsec / 1000000000.0;
        ntime += sec * 1000000000L + nsec - lost;
    }

    public void stop(PrintStream out) {
        stop();
        metric("Summary", out);
    }

    public boolean loop() {
        return count++ < total;
    }
    public long count() {
        return count-1;
    }
    public void trace(PrintStream out) {
        trace("trace", out);
    }

    public void trace(String prefix, PrintStream out) {
        if (((count + 1) % traces) == 0) {
            final long now = System.nanoTime();
            final long t = now - nanoTime;
            final double k = kips((double)t, traces);
            if(null != out && prefix != null) {
                out.println(String.format("%s[%07d]\t%s\t%.3fK/s", prefix, count, elapsed(t), k));
            }
            statistics.push(k);
            lost += (System.nanoTime() - now);
            msTime = System.currentTimeMillis();
            nanoTime = System.nanoTime();
        }
    }

    public void trace() {
        if (((count + 1) % traces) == 0) {
            final long now = System.nanoTime();
            final long t = now - nanoTime;
            //statistics.push((double)t);
            statistics.push(kips((double)t, traces));
            msTime = System.currentTimeMillis();
            nanoTime = System.nanoTime();
            lost += (System.nanoTime() - now);
        }
    }

    public void metric(PrintStream out) {
        metric("Summary", out);
    }

    public void metric(String prefix, PrintStream out) {
        out.println(metric(prefix));
    }

    public String metric(String prefix) {
        double num = (double)traces;
        if(count() <= 0) {
            num = 1.0d;
            count = 2;
        }
        if(statistics.count() == 0) {
            statistics.push(kips((double)ntime, num));
        }
        return String.format("%s %s count:%d lost:%s",
                prefix, summary(num), count(), elapsed(lost));
    }

    public String elapsed() {
        return elapsed(ntime);
    }

    public String average(int times) {
        return elapsed(ntime/(long)times);
    }

    private String summary(double num) {
        //KIPS (Kilo Instructions Per Second))
        double kips = statistics.mean();
        //MIPS (Million Instructions Per Second) 1K/s ÷ 1000
        double mips = kips / 1000.0d;
        double p = 1.0d / statistics.mean(); // An operation elapsed millis
        return String.format("ETA:%s MIPS:%.3f %.3fms/per min:%.3fK/s max:%.3fK/s avg:%.3fK/s mean:%.3fK/s",
                elapsed(ntime), mips, p,
                statistics.min(),
                statistics.max(),
                statistics.average(),
                statistics.geometricMean());
    }

    private double kips(double nano, double num) {
        return num * NANO_PER_MILL / nano; // 1 millis run operations
    }

    private String elapsed(long nano) {
        long s = nano / 1000000000L;
        long ms = (nano - (s * 1000000000L)) / 1000000L;
        long ns = nano - (s * 1000000000L) - (ms * 1000000L);
        StringBuilder sb = new StringBuilder();
        if(s > 0) sb.append(s + "s");
        if(ms > 0) {
            if(sb.length() > 0) sb.append(" ");
            sb.append(ms + "ms");
        }
        if(ns > 0) {
            if(sb.length() > 0) sb.append(" ");
            sb.append(ns + "ns");
        }
        if(sb.length() == 0) {
            sb.append("0ms");
        }
        return sb.toString();
    }

    public Benchmarker target(Object obj, Method method, Object... args) {
        target = new Supplier<Object>() {
            @Override
            public Object get() {
                try {
                    return method.invoke(obj);
                } catch (Exception e) {
                    return e;
                }
            }
        };
        return this;
    }

    public Benchmarker target(Supplier<Object> target) {
        this.target = target;
        return this;
    }

    public void run(long loop) {
        run(loop, 0);
    }
    public void run(long loop, int threads) {
        run(null, loop, Math.max(loop/10L, 1L), threads);
    }
    public void run(String prefix, long loop) {
        run(prefix, loop, Math.max(loop/10L, 1L), 0);
    }
    public void run(String prefix, long loop, int threads) {
        run(prefix, loop, Math.max(loop/10L, 1L), threads);
    }

    public void run(String prefix, long loop, long per, int threads) {
        if(target == null)
            throw new IllegalArgumentException("");
        if (threads > 1) {
            exec(target, prefix, loop, per, threads);
        } else {
            exec(target, prefix, loop, per);
        }
    }

    public void run(Object obj, Method method, Object... args) {
        run(null, obj, method, args);
    }

    public void run(String prefix, Object obj, Method method, Object... args) {
        exec(new Supplier<Object>() {
            @Override
            public Object get() {
                try {
                    return method.invoke(obj);
                } catch (Exception e) {
                    return e;
                }
            }
        }, prefix, 1L, 1L);
    }

    private void exec(Supplier<Object> supplier, String prefix, long loop, long per) {
        start(loop, per);
        runIt(supplier, prefix);
        stop();
    }

    private void exec(Supplier<Object> supplier, String prefix, long loop, long per, int threads) {
        final CountDownLatch latch = new CountDownLatch(threads);
        start(loop, per);
        for (int j = 0; j < threads; j++) {
            new Thread(new Runnable() {
                public void run() {
                    runIt(supplier, prefix);
                    latch.countDown();
                }
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException ignore) {
        }
        stop();
    }

    private void runIt(Supplier<Object> supplier, String prefix) {
        if(null != prefix) {
            while (loop()) {
                supplier.get();
                trace(prefix, System.out);
            }
        } else {
            while (loop()) {
                supplier.get();
                trace();
            }
        }
    }
}
