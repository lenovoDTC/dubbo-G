package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.utils.HttpClient;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Created by lzg on 2017/3/28.
 */
public class HttpRandomLoadBalance extends HttpAbstractLoadBalance {

    protected String httpDoSelect(Map<String,Integer> providers, String method, String schema, String args)
    {
        // 重建一个Map，避免服务器的上下线导致的并发问题
        Map<String, Integer> serverMap = providers;

        // 取得Ip地址List
        Set<String> keySet = serverMap.keySet();
        ArrayList<String> keyList = new ArrayList<String>();
        keyList.addAll(keySet);

        java.util.Random random = new java.util.Random();
        int randomPos = random.nextInt(keyList.size());

        return HttpClient.httpClient(keyList.get(randomPos),method,schema,args);
    }
}
