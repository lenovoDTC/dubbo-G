package com.alibaba.dubbo.common.utils;

/**
 * Created by lzg on 2017/3/22.
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjAnalysis {

//    public static Map ConvertObjToMap(Object obj){
    public static List<String> ConvertObjToList(Object obj){
//        Map<String,Object> reMap = new HashMap<String,Object>();
        List<String> reList = new ArrayList<String>();
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
//                    if ("boolean".equals(fields[i].getType().getName()))  o = f.get(obj);
//                    else if ("byte".equals(fields[i].getType().getName())) o = f.get(obj);
//                    else if ("char".equals(fields[i].getType().getName())) o = f.get(obj);
//                    else if ("double".equals(fields[i].getType().getName())) o = f.get(obj);
//                    else if ("float".equals(fields[i].getType().getName())) o = f.get(obj);
//                    else if ("int".equals(fields[i].getType().getName())) o = f.get(obj);
//                    else if ("long".equals(fields[i].getType().getName())) o = f.get(obj);
//                    else if ("short".equals(fields[i].getType().getName())) o = f.get(obj);
//                    else if (fields[i].getType().getName().indexOf("java.lang")!=-1) o = f.get(obj);
//                    else if(fields[i].getType().getName().indexOf("java.util")!=-1)o = f.get(obj);
//                    else {
//                        try {
//                            o = ObjAnalysis.ConvertObjToMap(fields[i].getType().newInstance());
//                        } catch (InstantiationException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                     else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Boolean"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Byte"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Char"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Double"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Float"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Integer"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Long"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.Short"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .equals("java.lang.String"))
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .indexOf("[Ljava") != -1)
//                        o = f.get(obj);
//                    else if (fields[i].getType().getName().replace("class ","")
//                            .indexOf("java.util") != -1)
//                        o = f.get(obj);
//                    else {
//                        total = "{ParameterName="+lastName+",ParameterType="+parameterType
//                                + "("
//                                + ObjAnalysis
//                                .ConvertObjToMap(types[i]
//                                        .newInstance())
//                                + ")"+",Required=0";
//                    }
//                    reMap.put("("+fields[i].getGenericType().toString().replace("class ","")+")"+fields[i].getName().toString(), o);
                    String p = fields[i].getGenericType().toString().replace("class ","");
                    if (p.equals("java.lang.Boolean"))p = "Boolean";
                    else if (p.equals("java.lang.Byte"))p = "Byte";
                    else if (p.equals("java.lang.Char"))p = "Char";
                    else if (p.equals("java.lang.Double"))p = "Double";
                    else if (p.equals("java.lang.Float"))p = "Float";
                    else if (p.equals("java.lang.Integer"))p = "Integer";
                    else if (p.equals("java.lang.Long"))p = "Long";
                    else if (p.equals("java.lang.Short"))p = "Short";
                    else if (p.equals("java.lang.String"))p = "String";
                    reList.add("{ParameterName="+fields[i].getName().toString()+",ParameterType="+p+",Required=0,desc=}");
                } catch (NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
//                catch (IllegalAccessException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
            }
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return reList;
    }
}
