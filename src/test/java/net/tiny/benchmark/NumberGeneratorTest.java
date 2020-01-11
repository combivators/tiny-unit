package net.tiny.benchmark;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class NumberGeneratorTest {

    @Test
    public void testIntegerGenerator() throws Exception {
        for (int i=0; i<5000; i++) {
            int r = NumberGenerator.createInteger(1000);
            assertTrue( (r >= 0 && r <= 1000) );
        }

        int[] array = NumberGenerator.createInteger(100, 128);
        assertEquals(128, array.length);
        for (int i=0; i<array.length; i++) {
            assertTrue( (array[i] >= 0 && array[i] <= 100) );
        }

        String encoded = NumberGenerator.encodeInteger(array);
        int[] copied = NumberGenerator.decodeInteger(encoded);
        assertArrayEquals(array, copied);
        assertEquals(128, copied.length);
        for (int i=0; i<copied.length; i++) {
            assertTrue( (copied[i] >= 0 && copied[i] <= 100) );
        }
        System.out.println(encoded.length() + " vs " + (array.length * 4)); //128维整数 684(Encoded) > 512(Integer)
    }

    @Test
    public void testFloatGenerator() throws Exception {
        System.out.println("Generator Float");
        for (int i=0; i<10; i++) {
            System.out.println(String.format("%.3f", NumberGenerator.createFloat()));
        }
        NumberGenerator.reset();
        float[] array = NumberGenerator.createFloat(50);
        assertEquals(50, array.length);
        String encoded = NumberGenerator.encodeFloat(array);
        float[] copied = NumberGenerator.decodeFloat(encoded);
        assertArrayEquals(array, copied);
        assertEquals(50, copied.length);
        System.out.println(encoded.length() + " vs " + (array.length * 4)); //50维的浮点数据 268(Encoded) > 200(Float)
    }

    @Test
    public void testBenchmarkeFloatGenerator() throws Exception {
        NumberGenerator.reset();
        Benchmarker bench = new Benchmarker();
        bench.start();
        for (int i=0; i<10000000; i++) {
            NumberGenerator.createFloat(50);
        }
        bench.stop();
        bench.metric(System.out); //1000万件 : 12秒
        NumberGenerator.reset();
    }

}
