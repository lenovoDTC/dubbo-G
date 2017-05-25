package com.alibaba.dubbo.common.serialize.support.fst;

import com.alibaba.dubbo.common.serialize.ObjectInput;
import org.nustaq.serialization.FSTObjectInput;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Created by haoning1 on 2017/5/18.
 */
public class FstObjectInput implements ObjectInput {

    private FSTObjectInput input;

    public FstObjectInput(InputStream inputStream) {
        input = FstFactory.getDefaultFactory().getObjectInput(inputStream);
    }

    public boolean readBool() throws IOException {
        return input.readBoolean();
    }

    public byte readByte() throws IOException {
        return input.readByte();
    }

    public short readShort() throws IOException {
        return input.readShort();
    }

    public int readInt() throws IOException {
        return input.readInt();
    }

    public long readLong() throws IOException {
        return input.readLong();
    }

    public float readFloat() throws IOException {
        return input.readFloat();
    }

    public double readDouble() throws IOException {
        return input.readDouble();
    }

    public byte[] readBytes() throws IOException {
        int len = input.readInt();
        if (len < 0) {
            return null;
        } else if (len == 0) {
            return new byte[]{};
        } else {
            byte[] b = new byte[len];
            input.readFully(b);
            return b;
        }
    }

    @Override
    public void flushBuffer() throws IOException {

    }

    public String readUTF() throws IOException {
        return input.readUTF();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return input.readObject();
    }


    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> clazz) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> clazz, Type type) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }
}
