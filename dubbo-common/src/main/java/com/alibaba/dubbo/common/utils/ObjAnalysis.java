package com.alibaba.dubbo.common.utils;

/**
 * Created by yuanbo on 2017/4/21.
 */

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

public class ObjAnalysis {
    public static JSONObject ConvertObjToList(Type type) throws ClassNotFoundException {
        JSONObject jsonObject = new JSONObject();
        String param = type.toString().replace("class ","");
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
            jsonObject.put("parent",param.replace("[L","").replace(";","[]").replace("java.lang.","").replace("java.util.",""));
//            param = param.replace("[L","").replace(";","[]").replace("java.lang.","").replace("java.util.","");
        }
        else if (param.contains("[L")){
//            JSONObject children = new JSONObject();
//            children.put(param.substring(2,param.length()-1),pojo(param.substring(2,param.length()-1),new Type[0]));
            jsonObject.put("parent",param.substring(2,param.length()-1)+"[]");
//            jsonObject.put("children",children);
            jsonObject.put("children",pojo(param.substring(2,param.length()-1),new Type[0]));
//            param = param.substring(2,param.length()-1)+"[]"+pojo(param.substring(2,param.length()-1),new Type[0]);
        }
        else if (param.contains("<")){
//            JSONObject children = new JSONObject();
            JSONArray children = new JSONArray();
            Type[] types =((ParameterizedType)type).getActualTypeArguments();
            if (param.substring(0,param.indexOf("<")).contains("java.util")) {
                for(Type type1:types){
                    JSONObject zz = ConvertObjToList(type1);
                    if (!zz.isNull("children")){
                        children.put(zz);
                    }
                }
                jsonObject.put("parent",param.replace("java.lang.","").replace("java.util.",""));
                if (children.length()!=0) jsonObject.put("genericity",children);
                //            param = param.replace("java.lang.","").replace("java.util.","");
            }
            else {
//                children.put(pojo(param.substring(0,param.indexOf("<")),types));
                jsonObject.put("parent",param.replace("java.lang.","").replace("java.util.",""));
                jsonObject.put("children",pojo(param.substring(0,param.indexOf("<")),types));
//                param = param.replace("java.lang.","").replace("java.util.","")+pojo(param.substring(0,param.indexOf("<")),types);
            }
        }else {
            if (param.contains(".")) {
//                JSONObject children = new JSONObject();
//                children.put(param,pojo(param,new Type[0]));
                jsonObject.put("parent", param);
//                jsonObject.put("children",children);
                jsonObject.put("children",pojo(param,new Type[0]));
            }else jsonObject.put("parent", param);
//                param = param+ObjAnalysis.pojo(param,new Type[0]);
        }
    return jsonObject;
    }
    private static JSONArray pojo(String name, Type[] types) throws ClassNotFoundException {
//        List<String> reList = new ArrayList<String>();
        JSONArray jsonObject = new JSONArray();
        Map<String,Type> map = new HashMap<String,Type>();
        Field[] fields = Class.forName(name, true, Thread.currentThread().getContextClassLoader()).getDeclaredFields();
        if (types.length!=0){
            TypeVariable<?>[] files = Class.forName(name, true, Thread.currentThread().getContextClassLoader()).getTypeParameters();
            for (int i=0;i<files.length;i++){
                map.put(files[i].getName(),types[i]);
            }
        }
        for(int i=0;i<fields.length;i++) {
            JSONObject smalljson = new JSONObject();
//                String file = fields[i].getGenericType().toString().replace("class ","");
            Type file = fields[i].getGenericType();
            if (map.containsKey(file.toString().replace("class ",""))){
                file = map.get(file.toString().replace("class ",""));
            }
            smalljson.put("ParameterName",fields[i].getName().toString());
            smalljson.put("ParameterType",ConvertObjToList(file));
            smalljson.put("Required",0);
            smalljson.put("desc","");
            jsonObject.put(smalljson);
//            reList.add("{ParameterName="+fields[i].getName().toString()+",ParameterType="+ConvertObjToList(file)+",Required=0,desc=}");
            }
        return jsonObject;
    }
}
