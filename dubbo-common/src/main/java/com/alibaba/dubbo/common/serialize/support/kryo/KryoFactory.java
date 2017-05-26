package com.alibaba.dubbo.common.serialize.support.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.*;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.HashMap;

/**
 * Created by haoning1 on 2017/3/22.
 */
public class KryoFactory {
    private KryoPool pool;
    private com.esotericsoftware.kryo.pool.KryoFactory factory;

    private static KryoFactory instance = new KryoFactory();

    private KryoFactory () {
        factory = new com.esotericsoftware.kryo.pool.KryoFactory() {
            @Override
            public Kryo create() {
                Kryo kryo = new Kryo();
                kryo.setReferences(false);
                kryo.setRegistrationRequired(false);
                kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
                kryo.setDefaultSerializer(TaggedFieldSerializer.class);
//        kryo.register(byte[].class, new DefaultArraySerializers.ByteArraySerializer());
//        kryo.register(char[].class, new DefaultArraySerializers.CharArraySerializer());
//        kryo.register(short[].class, new DefaultArraySerializers.ShortArraySerializer());
//        kryo.register(int[].class, new DefaultArraySerializers.IntArraySerializer());
//        kryo.register(long[].class, new DefaultArraySerializers.LongArraySerializer());
//        kryo.register(float[].class, new DefaultArraySerializers.FloatArraySerializer());
//        kryo.register(double[].class, new DefaultArraySerializers.DoubleArraySerializer());
                kryo.register(String[].class, new DefaultArraySerializers.StringArraySerializer());
//        kryo.register(Object[].class, new DefaultArraySerializers.ObjectArraySerializer(kryo, Object[].class));
//        kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
//        kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
                kryo.register(Class.class, new DefaultSerializers.ClassSerializer());
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
                kryo.register(HashMap.class, new MapSerializer());
//        kryo.register(TimeZone.class, new DefaultSerializers.TimeZoneSerializer());
//        kryo.register(Calendar.class, new DefaultSerializers.CalendarSerializer());
//        kryo.register(Locale.class, new DefaultSerializers.LocaleSerializer());
//                kryo.register(Throwable.class, new JavaSerializer());
                return kryo;
            }
        };
        pool = new KryoPool.Builder(factory).build();
    }

    public static KryoFactory getInstance () {
        return instance;
    }

    public Kryo get () {
        return pool.borrow();
    }

    public void close (Kryo kryo) {
        pool.release(kryo);
    }
}
