package com.alibaba.dubbo.common.serialize.support.kryo;

import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.utils.Assert;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by haoning1 on 2017/3/20.
 */
public class KryoObjectOutput implements ObjectOutput, KryoDataFlag {

    private final Kryo kryo;
    private final Output output;

    public KryoObjectOutput(Output output) {
        Assert.notNull(output, " is NULL");
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
        this.output = output;
    }

    public KryoObjectOutput(OutputStream os) {
        this(new Output(os));
    }

    public void writeObject(Object obj) throws IOException {
        kryo.writeClassAndObject(output, obj);
    }

    public void writeBool(boolean v) throws IOException {
        kryo.writeObjectOrNull(output, v, boolean.class);
    }

    public void writeByte(byte v) throws IOException {
        kryo.writeObjectOrNull(output, v, byte.class);
    }

    public void writeShort(short v) throws IOException {
        kryo.writeObjectOrNull(output, v, short.class);
    }

    public void writeInt(int v) throws IOException {
        kryo.writeObjectOrNull(output, v, int.class);
    }

    public void writeLong(long v) throws IOException {
        kryo.writeObjectOrNull(output, v, long.class);
    }

    public void writeFloat(float v) throws IOException {
        kryo.writeObjectOrNull(output, v, float.class);
    }

    public void writeDouble(double v) throws IOException {
        kryo.writeObjectOrNull(output, v, double.class);
    }

    public void writeUTF(String v) throws IOException {
        kryo.writeClassAndObject(output, v);
    }

    public void writeBytes(byte[] v) throws IOException {
        if (v == null) {
            output.writeByte(OBJECT_NULL);
        } else if (v.length == 0) {
            output.writeByte(OBJECT_DUMMY);
        } else {
            output.writeByte(OBJECT_BYTES);
            output.writeInt(v.length);
            output.write(v, 0, v.length);
        }

    }

    public void writeBytes(byte[] v, int off, int len) throws IOException {
        //output.write(v, off, len);
        if (v == null) {
            output.writeByte(OBJECT_NULL);
        } else if (v.length == 0) {
            output.writeByte(OBJECT_DUMMY);
        } else {
            output.writeByte(OBJECT_BYTES);
            output.writeInt(v.length);
            output.write(v, off, len);
        }
    }

    public void flushBuffer() throws IOException {
        //output.flush();
        output.close();
    }
}
