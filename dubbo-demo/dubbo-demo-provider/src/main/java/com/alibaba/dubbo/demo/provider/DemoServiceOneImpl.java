package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.DemoServiceOne;

/**
 * Created by haoning1 on 2017/4/18.
 */
public class DemoServiceOneImpl implements DemoServiceOne{

    @Override
    public String get(String str) {
        return "hello world";
    }
}
