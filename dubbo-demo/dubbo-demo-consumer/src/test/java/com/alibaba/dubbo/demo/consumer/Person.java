package com.alibaba.dubbo.demo.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.ByteArraySerializer;
import javafx.beans.binding.ObjectExpression;
import javassist.bytecode.ByteArray;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Created by haoning1 on 2017/3/16.
 */
public class Person {
    private String name;
    private String age;
    private String[] z;
    private List<String>[] c;
    private Person1[] p;

    public String[] getZ() {
        return z;
    }

    public void setZ(String[] z) {
        this.z = z;
    }

    public String getName() {
        return name;
    }

    public String getAge() {
        return age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(String age) {
        this.age = age;
    }
}
