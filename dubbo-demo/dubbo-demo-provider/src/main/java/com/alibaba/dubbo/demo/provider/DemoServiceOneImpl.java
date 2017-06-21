package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.DemoServiceOne;
import com.alibaba.dubbo.demo.Person;

import java.util.List;

/**
 * Created by haoning1 on 2017/4/18.
 */
public class DemoServiceOneImpl implements DemoServiceOne{

    @Override
    public String get(String str) {
        return "hello world";
    }

    @Override
    public String sayHello(String name) {
        return name;
    }

}
