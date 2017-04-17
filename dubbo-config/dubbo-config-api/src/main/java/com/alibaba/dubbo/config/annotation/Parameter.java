package com.alibaba.dubbo.config.annotation;

import java.lang.annotation.*;

/**
 * Created by haoning1 on 2017/4/5.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Parameter {

    enum Type {
        Byte, Short, Int, Long, Float, Double, Boolean, Char, String, JSONArray, JSONString
    }

    String value() default "";

    boolean required() default true;

    String desc () default "";

    Type type() default Type.String;

}
