package com.alibaba.dubbo.common.serialize.support.kryo;

import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.utils.Assert;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
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
        kryo = KryoFactory.getInstance().get();
        this.input = input;
    }

    public KryoObjectInput(InputStream input) {
        this(new Input(input));
    }

    public boolean readBool() throws IOException {
//        return kryo.readObject(input, boolean.class);
        try {
            return input.readBoolean();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public byte readByte() throws IOException {
//        return kryo.readObject(input, byte.class);
//        return kryo.readObjectOrNull(input, byte.class, new DefaultSerializers.ByteSerializer());
        try {
            return input.readByte();
        }catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public short readShort() throws IOException {
//        return kryo.readObjectOrNull(input, short.class, new DefaultSerializers.ShortSerializer());
        try {
            return input.readShort();
        }catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public int readInt() throws IOException {
//        return kryo.readObjectOrNull(input, int.class, new DefaultSerializers.IntSerializer());
        try {
            return input.readInt();
        }catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public long readLong() throws IOException {
//        return kryo.readObjectOrNull(input, long.class, new DefaultSerializers.LongSerializer());
        try {
            return input.readLong();
        }catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public float readFloat() throws IOException {
//        return kryo.readObjectOrNull(input, float.class, new DefaultSerializers.FloatSerializer());
        try {
            return input.readFloat();
        }catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public double readDouble() throws IOException {
//        return kryo.readObjectOrNull(input, double.class, new DefaultSerializers.DoubleSerializer());
        try {
            return input.readDouble();
        }catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public String readUTF() throws IOException {
//        return kryo.readObjectOrNull(input, String.class, new DefaultSerializers.StringSerializer());
        try {
            return input.readString();
        }catch (KryoException e) {
            throw new IOException(e);
        }
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
        if (cls.equals(String.class))
            return (T) readUTF();
        return (T) kryo.readClassAndObject(input);
    }

    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        if (cls.equals(String.class))
            return (T) readUTF();
        return (T) readObject(cls);
    }

    public void flushBuffer () throws IOException {
        KryoFactory.getInstance().close(kryo);
    }
}
