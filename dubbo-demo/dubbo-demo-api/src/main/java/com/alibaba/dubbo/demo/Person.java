package com.alibaba.dubbo.demo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by lzg on 2017/4/26.
 */
public class Person<T> implements Serializable{
    private T name;
    private Map<String,List<Integer>> map;

    public T getName() {
        return name;
    }

    public void setName(T name) {
        this.name = name;
    }

    public Map<String, List<Integer>> getMap() {
        return map;
    }

    public void setMap(Map<String, List<Integer>> map) {
        this.map = map;
    }
}
