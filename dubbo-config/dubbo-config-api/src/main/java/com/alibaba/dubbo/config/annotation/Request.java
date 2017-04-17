package com.alibaba.dubbo.config.annotation;

import java.lang.annotation.*;

/**
 * Created by haoning1 on 2017/3/31.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Request {
    enum Method {
        GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
    }

    enum Version {
        HTTP_1,HTTP_2
    }

    String name() default "";

    String[] value() default {};

    Method[] method() default {};

    //Version[] version() default Version.HTTP_1;

    String[] headers() default {};


}
