package com.alibaba.dubbo.remoting.http;

import java.lang.reflect.Type;

/**
 * Created by haoning1 on 2017/4/6.
 */
public class ParameterMeta {
    private String name;
    private String realname;
    private boolean required = true;
    private String desc = "";
    private String parameterType;
    private String type;
    private int index;
    //    private String[] genericType;
//    private Class<?> parameterClass;
//    private Class<?>[] genericClass;
    private Type parameterTypePlus;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getParameterType() {
        return parameterType;
    }

    public void setParameterType(String parameterType) {
        this.parameterType = parameterType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

//    public String[] getGenericType() {
//        return genericType;
//    }
//
//    public void setGenericType(String[] genericType) {
//        this.genericType = genericType;
//    }

//    public Class<?> getParameterClass() {
//        return parameterClass;
//    }
//
//    public void setParameterClass(Class<?> parameterClass) {
//        this.parameterClass = parameterClass;
//    }
//
//    public Class<?>[] getGenericClass() {
//        return genericClass;
//    }
//
//    public void setGenericClass(Class<?>[] genericClass) {
//        this.genericClass = genericClass;
//    }


    public Type getParameterTypePlus() {
        return parameterTypePlus;
    }

    public void setParameterTypePlus(Type parameterTypePlus) {
        this.parameterTypePlus = parameterTypePlus;
    }
}
