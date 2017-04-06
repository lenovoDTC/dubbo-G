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
package com.alibaba.dubbo.rpc.cluster.support.wrapper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.support.MockInvoker;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chao.liuc
 */
public class MockClusterInvoker<T> implements Invoker<T> {
    private String consumerIp ;
    private String group;
    private AtomicInteger total = new AtomicInteger();
    //    private AtomicInteger timetotal = new AtomicInteger();
    private AtomicInteger successNumber = new AtomicInteger();
    private AtomicInteger zkStatus = new AtomicInteger();
    private AtomicInteger fuseStatus = new AtomicInteger();
    private int successStatus = 2;
    private int oldtotal = 0;
    private int oldsuccessNumber = 0;
    private long start = System.currentTimeMillis();
    private long start1 = System.currentTimeMillis();
    private long start3 = System.currentTimeMillis();
    private JSONObject jsonObject = new JSONObject();
    private Map<String,Long> maptime = new ConcurrentHashMap<String, Long>();
    private Map<String,Long> mapnumber = new ConcurrentHashMap<String, Long>();
    private Map<String,Long> maptotal = new ConcurrentHashMap<String, Long>();
    private Map<String,Double> averagemap = new ConcurrentHashMap<String, Double>();
    private double xiancheng = 0;
    private ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Logger logger = LoggerFactory
            .getLogger(MockClusterInvoker.class);

    private final Directory<T> directory;

    private final Invoker<T> invoker;

    public MockClusterInvoker(Directory<T> directory, Invoker<T> invoker)  {
        this.directory = directory;
        this.invoker = invoker;
            try {
                consumerIp = InetAddress.getLocalHost().getHostAddress().toString()+":"+directory.getUrl().getParameter(Constants.PID_KEY);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            group =  ConfigUtils.getProperty("dubbo.registry.group");
        if(group==null||group.equals("")){
            group = "/dubbo";
        }
    }

    public URL getUrl() {
        return directory.getUrl();
    }

    public boolean isAvailable() {
        return directory.isAvailable();
    }

    public void destroy() {
        this.invoker.destroy();
    }

    public Class<T> getInterface() {
        return directory.getInterface();
    }

    public Result invoke(Invocation invocation) throws RpcException {
        Result result = null;
        String value = directory
                .getUrl()
                .getMethodParameter(invocation.getMethodName(),
                        Constants.MOCK_KEY, Boolean.FALSE.toString()).trim();

        if (value.length() == 0 || value.equalsIgnoreCase("false")) {
            // no mock
            result = this.invoker.invoke(invocation);
        } else if (value.startsWith("force")) {
            if (logger.isWarnEnabled()) {
                logger.info("force-mock: " + invocation.getMethodName()
                        + " force-mock enabled , url : " + directory.getUrl());
            }
            // force:direct mock
            result = doMockInvoke(invocation, null);
        } else {
            // fail-mock
            try {
                String SF = "no";
                String errorrate = directory
                        .getUrl()
                        .getMethodParameter(invocation.getMethodName(),
                                Constants.ERRORRATE_KEY,
                                Boolean.FALSE.toString()).trim();
                String supervene = directory
                        .getUrl()
                        .getMethodParameter(invocation.getMethodName(),
                                Constants.SUPERVENE_KEY,
                                Boolean.FALSE.toString()).trim();
                if (Float.parseFloat(errorrate) != 0
                        || Integer.parseInt(supervene) != 0) {
                    SF = fuseCount(errorrate, supervene, "before",(long)-1,invocation.getMethodName());
                }
                if (SF.equals("fail")) {
                    result = doMockInvoke(invocation, null);
                    return result;
                }
                if (SF.equals("no")) {
                    result = this.invoker.invoke(invocation);
                }
                if (SF.equals("success")) {
                    long timeBefore = System.currentTimeMillis();
                    result = this.invoker.invoke(invocation);
                    long time = System.currentTimeMillis()-timeBefore;
                    fuseCount(errorrate, supervene, "after",time,invocation.getMethodName());
                }
            } catch (RpcException e) {
                if (e.isBiz()) {
                    throw e;
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.info(
                                "fail-mock: " + invocation.getMethodName()
                                        + " fail-mock enabled , url : "
                                        + directory.getUrl(), e);
                    }
                    result = doMockInvoke(invocation, e);
                }
            }
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Result doMockInvoke(Invocation invocation, RpcException e) {
        Result result = null;
        Invoker<T> minvoker;

        List<Invoker<T>> mockInvokers = selectMockInvoker(invocation);
        if (mockInvokers == null || mockInvokers.size() == 0) {
            minvoker = (Invoker<T>) new MockInvoker(directory.getUrl());
        } else {
            minvoker = mockInvokers.get(0);
        }
        try {
            result = minvoker.invoke(invocation);
        } catch (RpcException me) {
            if (me.isBiz()) {
                result = new RpcResult(me.getCause());
            } else {
                throw new RpcException(me.getCode(), getMockExceptionMessage(e,
                        me), me.getCause());
            }
            //
        } catch (Throwable me) {
            throw new RpcException(getMockExceptionMessage(e, me),
                    me.getCause());
        }
        return result;
    }

    private String getMockExceptionMessage(Throwable t, Throwable mt) {
        String msg = "mock error : " + mt.getMessage();
        if (t != null) {
            msg = msg + ", invoke error is :" + StringUtils.toString(t);
        }
        return msg;
    }

    /**
     * 返回MockInvoker 契约：
     * directory根据invocation中是否有Constants.INVOCATION_NEED_MOCK，来判断获取的是一个normal
     * invoker 还是一个 mock invoker 如果directorylist 返回多个mock invoker，只使用第一个invoker.
     *
     * @param invocation
     * @return
     */
    private List<Invoker<T>> selectMockInvoker(Invocation invocation) {
        // TODO generic invoker？
        if (invocation instanceof RpcInvocation) {
            // 存在隐含契约(虽然在接口声明中增加描述，但扩展性会存在问题.同时放在attachement中的做法需要改进
            ((RpcInvocation) invocation).setAttachment(
                    Constants.INVOCATION_NEED_MOCK, Boolean.TRUE.toString());
            // directory根据invocation中attachment是否有Constants.INVOCATION_NEED_MOCK，来判断获取的是normal
            // invokers or mock invokers
            List<Invoker<T>> invokers = directory.list(invocation);
            return invokers;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "invoker :" + this.invoker + ",directory: " + this.directory;
    }

    /**
     * 计数
     *
     * @param errorrate
     * @param status
     * @return
     */
    public String fuseCount(String errorrate, String supervene, String status,Long time,String methodName) {
        if (status.equals("before")) {
            long elapsed = System.currentTimeMillis() - start;
            long elapsed1 = System.currentTimeMillis() - start1;
            if (elapsed / 1000 >= 30) {// 30秒后数据上传zk
                if (zkStatus.getAndIncrement() == 0) {
                    Runnable run = new Runnable() {
                        public void run()  {
                            start = System.currentTimeMillis();
                            averagemap = new ConcurrentHashMap<String, Double>();
                            xiancheng = 0;
                            jsonObject.put(consumerIp + "/" + "total", "" + total.get());
                            for (String key : maptotal.keySet()) {
                                jsonObject.put(key + "/Number", maptotal.get(key));
                                jsonObject.put(key + "/time", maptime.get(key) / mapnumber.get(key));
                            }
                            try {
                                ZooKeeper zkClient = new ZooKeeper(directory.getUrl().getHost(),
                                        30000, new Watcher() {
                                    public void process(WatchedEvent arg0) {
                                    }
                                });
                                try {
                                    String bb = "{}";
                                    List<String> consumerChild = zkClient.getChildren(group+ "/" + directory.getUrl().getServiceInterface() + "/consumers", null);
                                    List<String> providerChild = zkClient.getChildren(group+ "/" + directory.getUrl().getServiceInterface() + "/providers", null);
                                    for (int providerInt = 0; providerInt < providerChild.size(); providerInt++) {//根据prvider计算线程总数线程数
                                        if(providerChild.get(providerInt).indexOf("threads%3D")==-1){
                                            xiancheng=xiancheng+100;
                                        }else {
                                            String threads = providerChild.get(providerInt).substring(providerChild.get(providerInt).indexOf("threads%3D")).substring(10, providerChild.get(providerInt).substring(providerChild.get(providerInt).indexOf("threads%3D")).indexOf("%26"));
                                            xiancheng=xiancheng+Integer.parseInt(threads);
                                        }
                                    }
                                    byte[] aa = zkClient.getData(group+ "/"
                                                    + directory.getUrl().getServiceInterface()+"/consumers",
                                            false, null);
                                    if (aa != null) {
                                        bb = new String(aa);
                                    }
                                    JSONObject aaJsonObject = new JSONObject(bb);
                                    Iterator iterator2 = jsonObject.keys();
                                    while (iterator2.hasNext()) {
                                        String key = (String) iterator2.next();
                                        Long value2 = Long.parseLong(jsonObject
                                                .get(key).toString());
                                        aaJsonObject.put(key, value2);
                                    }
                                    zkClient.setData(group+ "/"
                                                    + directory.getUrl().getServiceInterface()+"/consumers",
                                            (aaJsonObject.toString()).getBytes(), -1);
                                    zkClient.close();
                                    Map<String, Integer> NATmap = new ConcurrentHashMap<String, Integer>();
                                    String[] methods = providerChild.get(0).substring(providerChild.get(0).indexOf("methods%3D")).substring(10, providerChild.get(0).substring(providerChild.get(0).indexOf("methods%3D")).indexOf("%26")).split("%2C");
                                    for (String method : methods) {
                                        for (int consumerInt = 0; consumerInt < consumerChild.size(); consumerInt++) {
                                            String IP = consumerChild.get(consumerInt).substring(17).substring(0, consumerChild.get(consumerInt).substring(17).indexOf("%2F"));
                                            String pid = consumerChild.get(consumerInt).substring(consumerChild.get(consumerInt).indexOf("pid%3D")).substring(6, consumerChild.get(consumerInt).substring(consumerChild.get(consumerInt).indexOf("pid%3D")).indexOf("%26"));
                                            if (aaJsonObject.toString().indexOf(IP + "/" + pid +"/"+ method) != -1) {
                                                int aNumber = Integer.parseInt(aaJsonObject.get(IP + "/" + pid +"/"+ method + "/Number").toString());
                                                int atime = Integer.parseInt(aaJsonObject.get(IP + "/" + pid +
                                                        "/"+ method + "/time").toString());
                                                if (NATmap.containsKey(method)) {
                                                    NATmap.put(method, NATmap.get(method) + aNumber * atime);
                                                } else {
                                                    NATmap.put(method, aNumber*atime);
                                                }
                                            }
                                        }
                                    }
                                    int timeAll = 0;
                                    for (String key : NATmap.keySet()) {
                                        timeAll = timeAll + NATmap.get(key);
                                    }

                                    for (String method : methods) {//每个提供者每个方法
                                        if (!aaJsonObject.isNull(consumerIp + "/" + method + "/Number")) {
                                            int bNumber = Integer.parseInt(aaJsonObject.get(consumerIp + "/" + method + "/Number").toString());
//                                            int btime = Integer.parseInt(aaJsonObject.get(consumerIp + "/" + method + "/time").toString());
//									averagemap.put(method,(xiancheng / timeAll) * bNumber * btime);//线程数
                                            averagemap.put(method, (xiancheng / timeAll) * bNumber * 1000);//并发量
                                        }
                                    }
                                } catch (KeeperException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            jsonObject = new JSONObject();
                            maptotal = new ConcurrentHashMap<String, Long>();
                            maptime = new ConcurrentHashMap<String, Long>();
                            mapnumber = new ConcurrentHashMap<String, Long>();
                            total.set(0);
                            start3 = System.currentTimeMillis();
                            successNumber.set(0);
                            zkStatus.set(0);
                        }
                    };
                    pool.execute(run);
                }
            }

            if (Integer.parseInt(supervene) != 0) {
                if (System.currentTimeMillis() - start3 != 0) {//通过配置降级
                    if ((float) total.get() * 1000 / (System.currentTimeMillis() -
                            start3) >= Integer.parseInt(supervene)) {
                        System.out.println("降   级");
                        return "fail";
                    }
                }
            }
            total.getAndIncrement();
            if (maptotal.containsKey(consumerIp + "/" + methodName)) {
                maptotal.put(consumerIp + "/" + methodName, maptotal.get(consumerIp + "/" + methodName) + 1);
            } else {
                maptotal.put(consumerIp + "/" + methodName, (long) 1);
            }
            if (Integer.parseInt(supervene) == 0 && averagemap.size() > 0) {
                if (System.currentTimeMillis() - start3 != 0) {
                    if (maptotal.get(consumerIp + "/" + methodName) * 1000 / (System.currentTimeMillis() -
                            start3) >= averagemap.get(methodName)) {
                        System.out.println("降   级");
                        return "fail";
                    }
                }
            }
            if (Float.parseFloat(errorrate) != 0) {
                if (elapsed1 / 1000 >= 10) {// 10秒检测熔断
                    if (total.get() == 0 && successNumber.get() == 0) {
                        oldsuccessNumber = 0;
                        oldtotal = 0;
                    }
                    int newsuccessNumber = successNumber.get();
                    int newtotal = total.get() - 1;
                    if (newtotal != oldtotal) {
                        if (100 - ((float) (newsuccessNumber - oldsuccessNumber)
                                / (newtotal - oldtotal) * 100) >= Float
                                .parseFloat(errorrate)) {
                            if (elapsed1 / 1000 >= successStatus * 10) {
                                if (fuseStatus.getAndIncrement() == 0) {
                                    successStatus++;
                                    fuseStatus.set(0);
                                    return "success";
                                }
                            }
                            System.out.println("熔   断");
                            return "fail";
                        }
                    }
                    start1 = System.currentTimeMillis();
                    oldsuccessNumber = successNumber.get();
                    oldtotal = total.get() - 1;
                }
            }
        }
        if (status.equals("after")) {
            successNumber.getAndIncrement();
            jsonObject.put(consumerIp+"/"+"success", "" + successNumber.get());
            if(maptime.containsKey(consumerIp+"/"+methodName)){
                maptime.put(consumerIp+"/"+methodName, maptime.get(consumerIp+"/"+methodName)+time);
                mapnumber.put(consumerIp+"/"+methodName,mapnumber.get(consumerIp+"/"+methodName)+1);
            }
            else{
                maptime.put(consumerIp+"/"+methodName,time);
                mapnumber.put(consumerIp+"/"+methodName,(long)1);
            }
            if (fuseStatus.get() >= 0) {
                successStatus = 2;
                start1 = System.currentTimeMillis();
                oldsuccessNumber = successNumber.get();
                oldtotal = total.get() - 1;
            }
        }
        return "success";
    }
}