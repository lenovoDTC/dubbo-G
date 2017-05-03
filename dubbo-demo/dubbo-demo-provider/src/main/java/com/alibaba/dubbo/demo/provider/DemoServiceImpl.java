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
package com.alibaba.dubbo.demo.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.alibaba.dubbo.config.annotation.Parameter;
import com.alibaba.dubbo.config.annotation.Request;
import com.alibaba.dubbo.config.annotation.Response;
import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.Person;
import com.alibaba.dubbo.rpc.RpcContext;

public class DemoServiceImpl implements DemoService {

    public String sayHello(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello$$*(&^*() " + name + ", response form provider: " + RpcContext.getContext().getLocalAddress();
    }
    public String sayHello1(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello sayHello1, request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello sayHello1, response form provider: " + RpcContext.getContext().getLocalAddress();
    }

    @Request(name="sayHello", value="/demo/sayHello", method = {Request.Method.POST, Request.Method.GET})
    @Response(headers = {""})
    public byte[] sayHello2(@Parameter(value = "name", required = false) List<String> name) {
        return new byte[]{1,2,3,4};
    }

    @Override
    public String sayHello3(Person z) {
        return null;
    }

}