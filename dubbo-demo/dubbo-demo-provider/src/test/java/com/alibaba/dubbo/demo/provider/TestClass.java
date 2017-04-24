package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.DemoService;
import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * Created by haoning1 on 2017/4/20.
 */
public class TestClass {
    public static void main(String[] args) {
        Class<?> clazz = DemoService.class;
        ClassPool pool = ClassPool.getDefault();
        try {
            CtClass ctClass = pool.get(clazz.getName());
            CtMethod ctMethod = ctClass.getDeclaredMethod("sayHello");

            // 使用javassist的反射方法的参数名
            MethodInfo methodInfo = ctMethod.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
                    .getAttribute(LocalVariableAttribute.tag);
            if (attr != null) {
                int len = ctMethod.getParameterTypes().length;
                // 非静态的成员函数的第一个参数是this
                int pos = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;
                System.out.print("test : ");
                for (int i = 0; i < len; i++) {
                    System.out.print(attr.variableName(i + pos) + ' ');
                }
                System.out.println();
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void test(String param1, int param2) {
        System.out.println(param1 + param2);
    }
}
