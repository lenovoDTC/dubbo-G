package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.utils.HttpClient;
import com.alibaba.dubbo.rpc.cluster.HttpMockinterface;
import com.alibaba.dubbo.rpc.cluster.support.wrapper.HttpMock;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by lzg on 2017/3/28.
 */
public class HttpRoundRobinLoadBalance extends HttpAbstractLoadBalance {
    private static Integer pos = 0;
    protected String httpDoSelect(Map<String,Integer> providers, String method, String schema, String args)
    {
        // 重建一个Map，避免服务器的上下线导致的并发问题
        Map<String, Integer> serverMap = providers;

        // 取得Ip地址List
        Set<String> keySet = serverMap.keySet();
        ArrayList<String> keyList = new ArrayList<String>();
        keyList.addAll(keySet);

        String server = null;
        synchronized (pos)
        {
            if (pos >= keySet.size())pos = 0;
            server = keyList.get(pos);
            pos ++;
        }
        return HttpClient.httpClient(server,method,schema,args);
    }
}