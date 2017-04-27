package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.HttpClient;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lzg on 2017/3/28.
 */
public class HttpRandomLoadBalance extends HttpAbstractLoadBalance {

    private static final Logger logger = LoggerFactory.getLogger(HttpRandomLoadBalance.class);

    protected String httpDoSelect(Map<String, List<AtomicInteger>> map, float errorrate, Map<String, Integer> providers, String rinterface, String method,Map<String,String> args)
    {
        TreeMap<Double, String> weightMap = new TreeMap<Double, String>();
        Set<String> keySet = providers.keySet();
        for (String key : keySet) {
            double lastWeight = weightMap.size() == 0 ? 0 : weightMap.lastKey().doubleValue();//统一转为double
            weightMap.put(providers.get(key).doubleValue() + lastWeight, key);//权重累加
        }
        if (weightMap.lastKey().doubleValue()==0)return "no providers";
        double randomWeight = weightMap.lastKey() * Math.random();
        SortedMap<Double, String> tailMap = weightMap.tailMap(randomWeight,false);
        return HttpClient.httpClient(map,errorrate,weightMap.get(tailMap.firstKey()),rinterface,method,args);
    }
}
