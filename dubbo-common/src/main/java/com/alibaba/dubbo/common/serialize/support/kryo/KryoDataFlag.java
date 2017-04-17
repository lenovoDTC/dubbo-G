package com.alibaba.dubbo.common.serialize.support.kryo;

/**
 * Created by haoning1 on 2017/3/30.
 */
public interface KryoDataFlag {
    // prefix three bits
    byte VARINT = 0, OBJECT = (byte) 0x80;
    byte OBJECT_BYTES = OBJECT | 3;
    // object constants
    byte OBJECT_NULL = OBJECT | 20, OBJECT_DUMMY = OBJECT | 21;
}
