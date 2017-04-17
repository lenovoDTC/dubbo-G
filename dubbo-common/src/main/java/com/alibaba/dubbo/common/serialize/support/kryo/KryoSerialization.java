package com.alibaba.dubbo.common.serialize.support.kryo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by haoning1 on 2017/3/20.
 */
public class KryoSerialization implements Serialization {
    public byte getContentTypeId() {
        return 8;
    }

    public String getContentType() {
        return "x-application/kryo";
    }

    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        return new KryoObjectOutput(output);
    }

    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        return new KryoObjectInput(input);
    }
}
