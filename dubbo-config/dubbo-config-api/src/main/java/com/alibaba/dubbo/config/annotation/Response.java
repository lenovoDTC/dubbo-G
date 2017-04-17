package com.alibaba.dubbo.config.annotation;

/**
 * Created by haoning1 on 2017/3/31.
 */
public @interface Response {
    String[] headers() default {"application/json"};
}
