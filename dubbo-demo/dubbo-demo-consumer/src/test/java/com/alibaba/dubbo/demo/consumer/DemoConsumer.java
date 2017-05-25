/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DemoConsumer {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        com.alibaba.dubbo.container.Main.main(args);
//        com.alibaba.dubbo.container.Http.main(args);
//        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"classpath*:META-INF/spring/dubbo-demo-consumer.xml"});
//        context.start();
//
//        DemoService demoService = (DemoService) context.getBean("demoService");
//
//        demoService.sayHello("hello world");
//        Future<String> future = RpcContext.getContext().getFuture();
//        String result = future.get(30000, TimeUnit.MILLISECONDS);
//        System.out.println("result  " + result);
//        System.in.read(); // 按任意键退出
    }

}