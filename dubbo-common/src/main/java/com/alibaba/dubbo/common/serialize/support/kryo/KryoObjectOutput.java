package com.alibaba.dubbo.common.serialize.support.kryo;

import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.utils.Assert;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by haoning1 on 2017/3/20.
 */
public class KryoObjectOutput implements ObjectOutput, KryoDataFlag {

    private final Kryo kryo;
    private final Output output;

    public KryoObjectOutput(Output output) {
        Assert.notNull(output, " is NULL");
        kryo = new Kryo();
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
