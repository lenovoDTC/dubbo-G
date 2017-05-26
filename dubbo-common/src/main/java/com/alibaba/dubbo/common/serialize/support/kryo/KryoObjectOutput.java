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
        kryo = KryoFactory.getInstance().get();
        this.output = output;
    }

    public KryoObjectOutput(OutputStream os) {
        this(new Output(os));
    }

    public void writeObject(Object obj) throws IOException {
        if (obj instanceof String)
            writeUTF((String) obj);
        else
            kryo.writeClassAndObject(output, obj);
    }

    public void writeBool(boolean v) throws IOException {
//        kryo.writeObjectOrNull(output, v, boolean.class);
//        kryo.writeObjectOrNull(output, v, new DefaultSerializers.BooleanSerializer());
        output.writeBoolean(v);
    }

    public void writeByte(byte v) throws IOException {
//        kryo.writeObjectOrNull(output, v, new DefaultSerializers.ByteSerializer());
        output.writeByte(v);
    }

    public void writeShort(short v) throws IOException {
//        kryo.writeObjectOrNull(output, v, new DefaultSerializers.ShortSerializer());
        output.writeShort(v);
    }

    public void writeInt(int v) throws IOException {
//        kryo.writeObjectOrNull(output, v, new DefaultSerializers.IntSerializer());
        output.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
//        kryo.writeObjectOrNull(output, v, new DefaultSerializers.LongSerializer());
        output.writeLong(v);
    }

    public void writeFloat(float v) throws IOException {
//        kryo.writeObjectOrNull(output, v, new DefaultSerializers.FloatSerializer());
        output.writeFloat(v);
    }

    public void writeDouble(double v) throws IOException {
//        kryo.writeObjectOrNull(output, v, new DefaultSerializers.DoubleSerializer());
        output.writeDouble(v);
    }

    public void writeUTF(String v) throws IOException {
//        kryo.writeObject(output, v, new DefaultSerializers.StringSerializer());
        output.writeString(v);
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
        KryoFactory.getInstance().close(kryo);
        output.close();
    }
}
