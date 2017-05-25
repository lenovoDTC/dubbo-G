package com.alibaba.dubbo.demo;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by haoning1 on 2017/5/25.
 */
public class Person<T> implements Serializable{
    private T name;
    private Map<String, Integer> map;

    public T getName() {
        return name;
    }

    public void setName(String T) {
        this.name = name;
    }

    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(Map<String, Integer> map) {
        this.map = map;
    }
}
