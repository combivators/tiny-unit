package net.tiny.benchmark;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public class BenchmarkExtension
    implements BeforeAllCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

    private static final Namespace NAMESPACE = Namespace.create("net", "tiny", "BenchmarkExtension");

    // EXTENSION POINTS
    @Override
    public void beforeAll(ExtensionContext context) {
        Benchmarker launcher = new Benchmarker();
        storeNowAsLaunchTime(context, getStoreKey(context, LaunchTimeKey.CLASS), launcher);
        launcher.start();
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        Benchmark benchmark = getBenchmarkAnnotation(context);
        if(null == benchmark)
            return;
        Benchmarker launcher = new Benchmarker();
        // Set Test target
        launcher.target(context.getRequiredTestInstance(), context.getRequiredTestMethod());
        storeNowAsLaunchTime(context, getStoreKey(context, LaunchTimeKey.TEST), launcher);

        //warmup loop
        if (benchmark.warmup() > 0) {
            if (benchmark.trace())
                launcher.run("warmup", benchmark.warmup());
            else
                launcher.run(benchmark.warmup());
        }
        //即使预热指定为0，其实第一次Method的执行是作为预热执行的。
    }

    //压力测试对象的Method先要被预热执行一次，实际测试结果是在afterTestExecution执行中取得的。
    @Override
    public void afterTestExecution(ExtensionContext context) {
        Benchmark benchmark = getBenchmarkAnnotation(context);
        if(null == benchmark)
            return;
        Benchmarker launcher =
                context.getStore(NAMESPACE).remove(getStoreKey(context, LaunchTimeKey.TEST), Benchmarker.class);
        //Measure loop
        if (benchmark.measure() > 0) {
            if (benchmark.trace())
                launcher.run("measure", benchmark.measure(), benchmark.threads());
            else
                launcher.run(benchmark.measure(), benchmark.threads());
            final int t = benchmark.threads() > 1 ? benchmark.threads() : 1;
            report(String.format("'%d threads, %d times, Avg %s'", t, benchmark.measure(), launcher.average(benchmark.measure())),
                    context, launcher);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        Benchmarker launcher =
                context.getStore(NAMESPACE).remove(getStoreKey(context, LaunchTimeKey.CLASS), Benchmarker.class);
        launcher.stop();
        report("Test container", context, launcher);
    }

    // HELPER
    private static void storeNowAsLaunchTime(ExtensionContext context, String key, Benchmarker launcher) {
        context.getStore(NAMESPACE).put(key, launcher);
    }

    private static void report(String unit, ExtensionContext context, Benchmarker launcher) {
        String message = launcher.metric(context.getDisplayName());
        context.publishReportEntry(unit, message);
    }

    private static String getStoreKey(ExtensionContext context, LaunchTimeKey key) {
        String storedKey = key.name();
        switch(key) {
        case CLASS:
            storedKey = context.getRequiredTestClass().getName();
            break;
        case TEST:
            storedKey = context.getRequiredTestInstance().getClass().getSimpleName();
            storedKey = storedKey.concat(".")
                                 .concat(context.getRequiredTestMethod().getName());
            break;
        }
        return storedKey;
    }

    private static Benchmark getBenchmarkAnnotation(ExtensionContext context) {
        try {
            return context.getElement()
                    .filter(el -> el.isAnnotationPresent(Benchmark.class))
                    .get()
                    .getAnnotation(Benchmark.class);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private enum LaunchTimeKey {
        CLASS, TEST
    }
}
