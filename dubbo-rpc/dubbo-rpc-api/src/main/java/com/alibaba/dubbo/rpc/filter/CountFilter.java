package com.alibaba.dubbo.rpc.filter;

import java.io.IOException;
import java.util.Iterator;


import org.apache.zookeeper.*;
import org.apache.zookeeper.server.auth.ProviderRegistry;
import org.json.JSONObject;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

public class CountFilter implements Filter {

    private long start = System.currentTimeMillis();
    private JSONObject jsonObject = new JSONObject();

    @Activate(group = Constants.CONSUMER)
    public Result invoke(Invoker<?> invoker, Invocation invocation)
            throws RpcException {
        invocation.getMethodName();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed / 1000 >= 300) {
            start = System.currentTimeMillis();
            try {
                ZooKeeper zkClient = new ZooKeeper(Constants.ZKSERVERS, 30000,
                        new Watcher() {
                            public void process(WatchedEvent arg0) {
                            }
                        });
                try {
                    String bb = "{}";
                    byte[] aa = zkClient.getData("/dubbo/"
                                    + invoker.getInterface().toString().substring(10),
                            false, null);
                    if (aa != null) {
                        bb = new String(aa);
                    }
                    JSONObject aaJsonObject = new JSONObject(bb);
                    Iterator iterator = jsonObject.keys();
                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        Long value = Long.parseLong(jsonObject.get(key)
                                .toString());
                        if (aaJsonObject.isNull(key)) {
                            aaJsonObject.put(key, value);
                            continue;
                        }
                        Long value1 = Long.parseLong(aaJsonObject.get(key)
                                .toString());
                        aaJsonObject.put(key, value + value1);
                    }
                    zkClient.setData("/dubbo/"
                                    + invoker.getInterface().toString().substring(10),
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
        }
        Result result = invoker.invoke(invocation);
        if (jsonObject.isNull("Provider : " + invoker.getUrl().getHost())) {
            jsonObject.put("Provider : " + invoker.getUrl().getHost(), 0);
        }
        Object successNumber = jsonObject.get("Provider : " + invoker.getUrl().getHost());
        jsonObject.put("Provider : " + invoker.getUrl().getHost(), Integer.parseInt(String.valueOf(successNumber)) + 1);
        return result;
    }
}