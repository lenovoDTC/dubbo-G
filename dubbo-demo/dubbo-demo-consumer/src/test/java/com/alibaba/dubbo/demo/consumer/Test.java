package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.fastjson.JSON;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by haoning1 on 2017/3/2.
 */
public class Test {
        public static void main(String[] args) {
//            String text = "{\"name\":\"haoning\",\"age\":30}";
//            Person p = JSON.parseObject(text, Person.class);
//
//            System.out.println(p.getName());

            Field[] fields = User.class.getDeclaredFields();
            for (Field field : fields) {
                System.out.println(field.getType());
                if (field.getName().equals("list")) {
                    Class<?> classType = field.getType();
                    Type mapMainType = field.getGenericType();
                    // 为了确保安全转换，使用instanceof
                    if (mapMainType instanceof ParameterizedType) {
                        // 执行强制类型转换
                        ParameterizedType parameterizedType = (ParameterizedType)mapMainType;
                        // 获取基本类型信息，即Map
                        Type basicType = parameterizedType.getRawType();
                        System.out.println("基本类型为："+basicType);
                        // 获取泛型类型的泛型参数
                        Type[] types = parameterizedType.getActualTypeArguments();
                        for (int i = 0; i < types.length; i++) {
                            System.out.println("第"+(i+1)+"个泛型类型是："+types[i]);
                        }
                    } else {
                        System.out.println("获取泛型类型出错!");
                    }
                }
            }


        }

        public class User {
            private Map<String, String> map;
            private List<String> list;
            private Set<String> set;


        }
}
