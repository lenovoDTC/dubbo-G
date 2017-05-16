package com.alibaba.dubbo.common.serialize.support.kryo;

import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.utils.Assert;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by haoning1 on 2017/3/20.
 */
public class KryoObjectInput implements ObjectInput, KryoDataFlag {

    private final Kryo kryo;
    private final Input input;

    private static final byte[] EMPTY_BYTES = {};

    public KryoObjectInput(Input input) {
        Assert.notNull(input, "is NULL");
        kryo = new Kryo();
//        kryo.setReferences(false);
//        kryo.setRegistrationRequired(true);
//        kryo.register(byte[].class, new DefaultArraySerializers.ByteArraySerializer());
//        kryo.register(char[].class, new DefaultArraySerializers.CharArraySerializer());
//        kryo.register(short[].class, new DefaultArraySerializers.ShortArraySerializer());
//        kryo.register(int[].class, new DefaultArraySerializers.IntArraySerializer());
//        kryo.register(long[].class, new DefaultArraySerializers.LongArraySerializer());
//        kryo.register(float[].class, new DefaultArraySerializers.FloatArraySerializer());
//        kryo.register(double[].class, new DefaultArraySerializers.DoubleArraySerializer());
//        kryo.register(String[].class, new DefaultArraySerializers.StringArraySerializer());
//        kryo.register(Object[].class, new DefaultArraySerializers.ObjectArraySerializer(kryo, Object[].class));
//        kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
//        kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
//        kryo.register(Class.class, new DefaultSerializers.ClassSerializer());
//
//        kryo.register(Date.class, new DefaultSerializers.DateSerializer());
//        kryo.register(EnumSet.class, new DefaultSerializers.EnumSetSerializer());
//        kryo.register(Currency.class, new DefaultSerializers.CurrencySerializer());
//        kryo.register(StringBuffer.class, new DefaultSerializers.StringBufferSerializer());
//        kryo.register(StringBuilder.class, new DefaultSerializers.StringBuilderSerializer());
//        kryo.register(Collections.EMPTY_LIST.getClass(), new DefaultSerializers.CollectionsEmptyListSerializer());
//        kryo.register(Collections.EMPTY_MAP.getClass(), new DefaultSerializers.CollectionsEmptyMapSerializer());
//        kryo.register(Collections.EMPTY_SET.getClass(), new DefaultSerializers.CollectionsEmptySetSerializer());
//        kryo.register(Collections.singletonList(null).getClass(), new DefaultSerializers.CollectionsSingletonListSerializer());
//        kryo.register(Collections.singletonMap(null, null).getClass(), new DefaultSerializers.CollectionsSingletonMapSerializer());
//        kryo.register(Collections.singleton(null).getClass(), new DefaultSerializers.CollectionsSingletonSetSerializer());
//        kryo.register(TreeSet.class, new DefaultSerializers.TreeSetSerializer());
//        kryo.register(Collection.class, new CollectionSerializer());
//        kryo.register(TreeMap.class, new DefaultSerializers.TreeMapSerializer());
//        kryo.register(Map.class, new MapSerializer());
//        kryo.register(TimeZone.class, new DefaultSerializers.TimeZoneSerializer());
//        kryo.register(Calendar.class, new DefaultSerializers.CalendarSerializer());
//        kryo.register(Locale.class, new DefaultSerializers.LocaleSerializer());

        this.input = input;
    }

    public KryoObjectInput(InputStream input) {
        this(new Input(input));
    }

    public boolean readBool() throws IOException {
        return kryo.readObject(input, boolean.class);
    }

    public byte readByte() throws IOException {
//        return kryo.readObject(input, byte.class);
        return kryo.readObjectOrNull(input, byte.class);
    }

    public short readShort() throws IOException {
        return kryo.readObjectOrNull(input, short.class);
    }

    public int readInt() throws IOException {
        return kryo.readObjectOrNull(input, int.class);
    }

    public long readLong() throws IOException {
        return kryo.readObjectOrNull(input, long.class);
    }

    public float readFloat() throws IOException {
        return kryo.readObjectOrNull(input, float.class);
    }

    public double readDouble() throws IOException {
        return kryo.readObjectOrNull(input, double.class);
    }

    public String readUTF() throws IOException {
        return (String) kryo.readClassAndObject(input);
    }

    public byte[] readBytes() throws IOException {
        byte flag = input.readByte();
        switch (flag) {
            case OBJECT_NULL:
                return null;
            case OBJECT_DUMMY:
                return EMPTY_BYTES;
            case OBJECT_BYTES:
                int len = input.readInt();
                return input.readBytes(len);
            default:
                throw new IOException("Tag error, expect BYTES|BYTES_NULL|BYTES_EMPTY, but get " + flag);
        }
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return kryo.readClassAndObject(input);
    }

    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return (T) kryo.readClassAndObject(input);
    }

    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        return (T) kryo.readClassAndObject(input);
    }
}
