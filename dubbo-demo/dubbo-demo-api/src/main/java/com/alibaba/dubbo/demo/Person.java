package com.alibaba.dubbo.demo;

import java.util.List;

/**
 * Created by lzg on 2017/5/10.
 */
public class Person<T> {
    private String name;
    private T age;

    public T getAge() {
        return age;
    }

    public void setAge(T age) {
        this.age = age;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
