package com.alibaba.dubbo.common.utils;

/**
 * Created by lzg on 2017/3/22.
 */
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ObjAnalysis {

    public static Map ConvertObjToMap(Object obj){
        Map<String,Object> reMap = new HashMap<String,Object>();
        if (obj == null)
            return null;
        Field[] fields = obj.getClass().getDeclaredFields();
        try {
            for(int i=0;i<fields.length;i++){
                try {
                    Field f = obj.getClass().getDeclaredField(fields[i].getName());
                    f.setAccessible(true);
                    Object o = null;
//                    System.out.println(fields[i].getType().getName());
                    if ("boolean".equals(fields[i].getType().getName()))  o = f.get(obj);
                    else if ("byte".equals(fields[i].getType().getName())) o = f.get(obj);
                    else if ("char".equals(fields[i].getType().getName())) o = f.get(obj);
                    else if ("double".equals(fields[i].getType().getName())) o = f.get(obj);
                    else if ("float".equals(fields[i].getType().getName())) o = f.get(obj);
                    else if ("int".equals(fields[i].getType().getName())) o = f.get(obj);
                    else if ("long".equals(fields[i].getType().getName())) o = f.get(obj);
                    else if ("short".equals(fields[i].getType().getName())) o = f.get(obj);
                    else if (fields[i].getType().getName().indexOf("java.lang")!=-1) o = f.get(obj);
                    else if(fields[i].getType().getName().indexOf("java.util")!=-1)o = f.get(obj);
                    else {
                        try {
                            o = ObjAnalysis.ConvertObjToMap(fields[i].getType().newInstance());
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        }
                    }
                    reMap.put("("+fields[i].getGenericType()+")"+fields[i].getName().toString(), o);
                } catch (NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return reMap;
    }
}
