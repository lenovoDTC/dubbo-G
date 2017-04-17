package com.alibaba.dubbo.common.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.easymock.internal.matchers.Null;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by haoning1 on 2017/3/22.
 */
public class KryoTest {
    private static Kryo kryo = new Kryo();;
    public static void main (String[] args) {
//        testStandard();
//        testStringArray();
//        testString();
        testBytes();
    }

    private static void testMap () {
        ByteArrayOutputStream out = null;
        Output output = null;
        byte[] bytes = null;

        try {
            out = new ByteArrayOutputStream();
            output = new Output(out, 1024);
            Map<String, String> map = new HashMap<String, String>(2);
            map.put("a", "1");
//            kryo.writeClassAndObject(output, map);
            kryo.writeObject(output, map);
            bytes = output.toBytes();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != out) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                }
            }
            if (null != output) {
                output.close();
                output = null;
            }
        }

        Input input = new Input(bytes, 0, 1024);

//        Map<String, String> map1 = (Map<String, String>) kryo.readClassAndObject(input);
        Map<String, String> map1 = (Map<String, String>) kryo.readObject(input, HashMap.class);
        System.out.println(map1.get("a"));
    }

    private static void testStandard () {
        ByteArrayOutputStream out = null;
        Output output = null;
        byte[] bytes = null;
        try {
            out = new ByteArrayOutputStream();
            output = new Output(out, 1024);
            long l = 1000l;
//            kryo.writeObjectOrNull(output, l, Long.class);
            kryo.writeClassAndObject(output, l);
            bytes = output.toBytes();
            System.out.println(bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != out) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                }
            }
            if (null != output) {
                output.close();
                output = null;
            }
        }

        Input input = new Input(bytes, 0, 1024);

        long l1 = kryo.readObjectOrNull(input, long.class);
        System.out.println(l1);
    }

    private static void testString () {
        byte[] bytes = null;
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); Output output = new Output(out, 1024);) {
            String str = "sssssssssss";
//            kryo.writeObjectOrNull(output, str, String.class);
            kryo.writeClassAndObject(output, str);
            bytes = output.toBytes();
            System.out.println(bytes.length);
        } catch (Exception e) {

        }

        try(Input input = new Input(bytes, 0, 1024);) {
            String str1 = (String)kryo.readClassAndObject(input);
//            String str1 = kryo.readObject(input, String.class);
            System.out.println(str1);
        } catch (Exception e) {

        }
    }

    private static void testStringArray () {
        byte[] bytes = null;
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); Output output = new Output(out, 1024);) {
            String[] str = new String[] {"a", "b"};
//            kryo.writeObjectOrNull(output, str, String.class);
//            kryo.writeClassAndObject(output, str);
            kryo.writeObjectOrNull(output, str, String[].class);
            bytes = output.toBytes();
            System.out.println(bytes.length);
        } catch (Exception e) {

        }

        try(Input input = new Input(bytes, 0, 1024);) {
//            String[] str1 = (String[])kryo.readClassAndObject(input);
            String[] str1 = kryo.readObject(input, String[].class);
            System.out.println(str1[0]);
        } catch (Exception e) {

        }
    }

    private static void testBytes() {
        byte[] bytes = null;
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); Output output = new Output(out, 1024);) {
            byte[] byteArray = new byte[] {1,2,3,4};
//            kryo.writeObjectOrNull(output, str, String.class);
//            kryo.writeClassAndObject(output, str);
//            kryo.writeObjectOrNull(output, str, String[].class);
            output.writeBytes(byteArray);
            bytes = output.toBytes();
            System.out.println(bytes.length);
        } catch (Exception e) {

        }

        try(Input input = new Input(bytes, 0, 1024);) {
//            String[] str1 = (String[])kryo.readClassAndObject(input);
            byte[] byteArray1 = kryo.readObject(input, byte[].class);
//            byte[] byteArray1 = new byte[4];
//            input.readBytes(byteArray1);
            System.out.println(byteArray1[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
