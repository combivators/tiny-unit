package net.tiny.benchmark;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Base64;
import java.util.Random;
import java.util.function.Supplier;

/**
 *
 * 测试用多维数据集生成器
 * @param <T>
 */
public abstract class NumberGenerator<T extends Number> implements Supplier<T>{

    final static NumberGenerator<Float> genFloat = new NumberGenerator<Float>() {
        @Override
        public Float get() {
            return f += (rand.nextFloat() + 0.01f);
        }
    };

    final static NumberGenerator<Integer> genInteger = new NumberGenerator<Integer>() {
        @Override
        public Integer get() {
            return (0==bound) ? rand.nextInt() : rand.nextInt(bound);
        }
    };

    public static float createFloat() {
        return genFloat.get();
    }

    public static float[] createFloat(int capacity) {
        float[] array = new float[capacity];
        for (int i=0; i<capacity; i++) {
            array[i] = genFloat.get();
        }
        return array;
    }

    public static String floatKey(int capacity) {
        return encodeFloat(createFloat(capacity));
    }

    public static String encodeFloat(float[] array) {
        ByteBuffer buffer = ByteBuffer.allocate( array.length * 4 );
        buffer.asFloatBuffer()
              .put(array);
        return Base64.getEncoder().encodeToString(buffer.array());
    }


    public static float[] decodeFloat(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        FloatBuffer buffer = ByteBuffer.wrap(bytes)
                .order(ByteOrder.BIG_ENDIAN)
                .asFloatBuffer();
        float[] array = new float[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    public static int createInteger(int bound) {
        genInteger.bound = bound;
        return genInteger.get();
    }

    public static int[] createInteger(int bound, int capacity) {
        genInteger.bound = bound;
        int[] array = new int[capacity];
        for (int i=0; i<capacity; i++) {
            array[i] = genInteger.get();
        }
        return array;
    }

    public static String integerKey(int bound, int capacity) {
        return encodeInteger(createInteger(bound, capacity));
    }

    public static String encodeInteger(int[] array) {
        ByteBuffer buffer = ByteBuffer.allocate( array.length * 4 );
        buffer.asIntBuffer()
              .put(array);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public static int[] decodeInteger(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        IntBuffer buffer = ByteBuffer.wrap(bytes)
                .order(ByteOrder.BIG_ENDIAN)
                .asIntBuffer();
        int[] array = new int[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    public static void reset() {
        genFloat.clear();
    }

    protected Random rand;
    protected int bound = 0;
    protected int    i;
    protected long   l;
    protected float  f;
    protected double d;

    protected NumberGenerator() {
        clear();
    }

    void clear() {
        rand = new Random(System.currentTimeMillis());
        i = 0;
        l = 0L;
        f = 0.0f;
        d = 0.0d;
    }
}
