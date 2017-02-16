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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.json.JSONObject;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.support.MockInvoker;

/**
 * @author chao.liuc
 */
public class MockClusterInvoker<T> implements Invoker<T> {
    private int total = 0;
    private int successNumber = 0;
    private int oldtotal = 0;
    private int oldsuccessNumber = 0;
    private long start = System.currentTimeMillis();
    private long start1 = System.currentTimeMillis();
    private JSONObject jsonObject = new JSONObject();

    private static final Logger logger = LoggerFactory.getLogger(MockClusterInvoker.class);

    private final Directory<T> directory;

    private final Invoker<T> invoker;

    public MockClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
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

        String value = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY, Boolean.FALSE.toString()).trim();

        if (value.length() == 0 || value.equalsIgnoreCase("false")) {
            //no mock
            result = this.invoker.invoke(invocation);
        } else if (value.startsWith("force")) {
            if (logger.isWarnEnabled()) {
                logger.info("force-mock: " + invocation.getMethodName() + " force-mock enabled , url : " + directory.getUrl());
            }
            //force:direct mock
            result = doMockInvoke(invocation, null);
        } else {
            //fail-mock
            try {
                long elapsed = System.currentTimeMillis() - start;
                long elapsed1 = System.currentTimeMillis() - start1;
                if (elapsed / 1000 >= 300) {
                    start = System.currentTimeMillis();
                    jsonObject.put("total", "" + total);
                    try {
                        ZooKeeper zkClient = new ZooKeeper(Constants.ZKSERVERS, 30000,
                                new Watcher() {
                                    public void process(WatchedEvent arg0) {
                                    }
                                });
                        try {
                            String bb = "{}";
                            byte[] aa = zkClient.getData("/dubbo/"
                                            + directory.getUrl().getServiceInterface(),
                                    false, null);
                            if (aa != null) {
                                bb = new String(aa);
                            }
                            JSONObject aaJsonObject = new JSONObject(bb);
                            Iterator iterator = jsonObject.keys();
                            while (iterator.hasNext()) {
                                String key = (String) iterator.next();
                                Long value2 = Long.parseLong(jsonObject.get(key)
                                        .toString());
                                if (aaJsonObject.isNull(key)) {
                                    aaJsonObject.put(key, value2);
                                    continue;
                                }
                                Long value1 = Long.parseLong(aaJsonObject.get(key)
                                        .toString());
                                aaJsonObject.put(key, value2 + value1);
                            }
                            zkClient.setData("/dubbo/"
                                            + directory.getUrl().getServiceInterface(),
                                    (aaJsonObject.toString()).getBytes(), -1);
                            zkClient.close();
                        } catch (KeeperException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    jsonObject = new JSONObject();
                    total = 0;
                    successNumber = 0;
                }
                total++;
                if (elapsed1 / 1000 >= 10) {
                    if (total == 0 && successNumber == 0) {
                        oldsuccessNumber = 0;
                        oldtotal = 0;
                    }
                    int newsuccessNumber = successNumber;
                    int newtotal = total;
                    String errorrate = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.ERRORRATE_KEY, Boolean.FALSE.toString()).trim();
                    if (Float.parseFloat(errorrate) != 0) {
                        if (newtotal != oldtotal) {
                            if (10 - ((float) (newsuccessNumber - oldsuccessNumber)
                                    / (newtotal - oldtotal) * 10) >= Float
                                    .parseFloat(errorrate)) {
                                if (elapsed1 / 1000 >= 40) {
                                    start1 = System.currentTimeMillis();
                                    oldsuccessNumber = successNumber;
                                    oldtotal = total;
                                }
                                System.out.println("熔断");
                                result = doMockInvoke(invocation, null);
                                return result;
                            }
                        }
                    }
                    start1 = System.currentTimeMillis();
                    oldsuccessNumber = successNumber;
                    oldtotal = total;
                }
                result = this.invoker.invoke(invocation);
                successNumber++;
                jsonObject.put("success", "" + successNumber);
            } catch (RpcException e) {
                if (e.isBiz()) {
                    throw e;
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.info("fail-mock: " + invocation.getMethodName() + " fail-mock enabled , url : " + directory.getUrl(), e);
                    }
                    result = doMockInvoke(invocation, e);
                }
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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
                throw new RpcException(me.getCode(), getMockExceptionMessage(e, me), me.getCause());
            }
//			
        } catch (Throwable me) {
            throw new RpcException(getMockExceptionMessage(e, me), me.getCause());
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
     * 返回MockInvoker
     * 契约：
     * directory根据invocation中是否有Constants.INVOCATION_NEED_MOCK，来判断获取的是一个normal invoker 还是一个 mock invoker
     * 如果directorylist 返回多个mock invoker，只使用第一个invoker.
     *
     * @param invocation
     * @return
     */
    private List<Invoker<T>> selectMockInvoker(Invocation invocation) {
        //TODO generic invoker？
        if (invocation instanceof RpcInvocation) {
            //存在隐含契约(虽然在接口声明中增加描述，但扩展性会存在问题.同时放在attachement中的做法需要改进
            ((RpcInvocation) invocation).setAttachment(Constants.INVOCATION_NEED_MOCK, Boolean.TRUE.toString());
            //directory根据invocation中attachment是否有Constants.INVOCATION_NEED_MOCK，来判断获取的是normal invokers or mock invokers
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
}