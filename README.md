# Tiny Unit: 一个基于JUnit5可进行网络，性能压力以及数据库的综合测试工具
## 设计目的
 - 提供一个简便的在UnitTest阶段对实装Class和Method进行各种综合测试时方法。
 - 对Java类进行性能压力测试的方法。
 - 对数据服务进行综合测试的方法。

## 性能压力测试功能
 - 输出性能压力测试的分析结果。
 - 使用@Benchmark注解简单绑定一个已有的TestCase。


###1. Simple benchmark a exited test case
```java
import net.tiny.benchmark.Benchmark;

public class SampleTest {

    @Benchmark(warmup=10, measure=1000, trace=true)
    public void testTask() throws Exception {
        //Here is your test code
    }
}
```

###2. Benchmark your all your test cases
```java
import net.tiny.benchmark.Benchmark;

@Benchmark
public class SampleTest {
    @Test
    public void testTask() throws Exception {
        //Here is your test code
    }
}
```

###3. Include benchmark in your code
```java
import net.tiny.benchmark.Benchmarker;

public class SampleTest {

    public void testTask() throws Exception {
        Benchmarker bench = new Benchmarker();
        bench.start(10000L, 1000L);
        while (bench.loop()) {
            //Here is your test code
            bench.trace(System.out);
        }
        bench.stop(System.out);
    }
}
```

```bash
Summary ETA:66ms 459889ns MIPS:0.241 0.004ms/per min:51.520K/s max:395.396K/s avg:237.786K/s mean:197.736K/s count:10000 lost:4ms 665661ns
```


## 数据库访问测试功能
 - 自动启动内置H2数据库引擎。
 - 提供DataSource参数接口。
 - 提供JPA参数接口。
 - 提供DBUnit简便的调用。
 - 使用@Database注解简单绑定一个已有的TestCase。


###2. Simple JDBC test for a exited test case
```java
import net.tiny.unit.Database;

@Database(db="test", port=9002
  ,trace=true
  ,imports="src/test/resources/data/imports"
  ,startScripts= {"create table xx_log (id BIGINT NOT NULL AUTO_INCREMENT, content CLOB, PRIMARY KEY (id));"}
  ,stopScripts= {"drop table xx_log;"}
  )
public class SampleTest {

    @ParameterizedTest
    @ArgumentsSource(DataSourceProvider.class)
    public void testTask(DataSource ds) throws Exception {
        Connection conn = ds.getConnection();
        //Here is your test code

        conn.commit();
        conn.close();
    }
}
```


###2. Simple JPA test for a exited test case
```java
import net.tiny.unit.Database;

@Database(jpa=true,
  persistence="persistence.properties"
  ,trace=true
  ,startScripts= {"create sequence log_sequence increment by 1 start with 1;"}
  ,stopScripts= {"drop sequence log_sequence;"}
  )
public class SampleTest {

    @ParameterizedTest
    @ArgumentsSource(EntityManagerProvider.class)
    public void testTask(EntityManager em) throws Exception {
        EntityTransaction trx = em.getTransaction();
        trx.begin();
        //Here is your test code

        trx.commit();
    }
}
```


##More Detail, See The Samples

---
Email   : wuweibg@gmail.com
