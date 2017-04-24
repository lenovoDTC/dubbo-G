package com.alibaba.dubbo.common.utils;

/**
 * Created by yuanbo on 2017/4/21.
 */

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ObjAnalysis {
    public static String ConvertObjToList(String param) throws ClassNotFoundException {
        if(!param.contains("<")
                &&(param.contains("java.util")
                ||param.contains("java.lang")
                ||param.equals("boolean")
                ||param.equals("byte")
                ||param.equals("char")
                ||param.equals("double")
                ||param.equals("float")
                ||param.equals("int")
                ||param.equals("long")
                ||param.equals("short")))
        {
            return param.replace("[L","").replace(";","[]").replace("java.lang.","").replace("java.util.","");
        }
        else if (param.contains("[L")){
            param = param.substring(2,param.length()-1)+"[]"+pojo(param.substring(2,param.length()-1));
        }
        else if (param.contains("<")){
            if (param.substring(param.indexOf("<")).contains("java.util")) param = param.replace("java.lang.","").replace("java.util.","");
            else {
                param = param.replace("java.lang.","").replace("java.util.","")+pojo(param.substring(0,param.indexOf("<")));
            }
        }else {
            if (param.length() == 1);
            else param = param+ObjAnalysis.pojo(param);
        }
    return param;
    }
    private static List<String> pojo(String name) throws ClassNotFoundException {
        List<String> reList = new ArrayList<String>();
        Field[] fields = Class.forName(name, true, Thread.currentThread().getContextClassLoader()).getDeclaredFields();
        for(int i=0;i<fields.length;i++) {
                String file = fields[i].getGenericType().toString().replace("class ","");
                reList.add("{ParameterName="+fields[i].getName().toString()+",ParameterType="+ConvertObjToList(file)+",Required=0,desc=}");
            }
        return reList;
    }
}
