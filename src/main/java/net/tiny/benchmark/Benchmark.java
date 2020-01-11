package net.tiny.benchmark;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.concurrent.TimeUnit;

@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Tag("Benchmark")
@Test
@ExtendWith(BenchmarkExtension.class)
public @interface Benchmark {
    int warmup() default 0; //即使预热指定为0，其实第一次Method的执行是作为预热执行的。
    //measure指定小于1时，只做一般的@Test，测试结果是空
    int measure()  default 1; //实际上多次测试结果是在afterTestExecution执行中取得的。
    int threads()  default 0;
    boolean trace() default false;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
