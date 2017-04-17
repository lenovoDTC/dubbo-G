package com.alibaba.dubbo.common.serialize.support.kryo;

import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.utils.Assert;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

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
