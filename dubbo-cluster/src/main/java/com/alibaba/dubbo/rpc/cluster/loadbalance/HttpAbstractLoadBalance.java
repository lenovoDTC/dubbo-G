package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.rpc.cluster.HttpLoadBalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lzg on 2017/3/28.
 */
public abstract class HttpAbstractLoadBalance implements HttpLoadBalance {

    public String httpSelect(Map<String, List<AtomicInteger>> map, float errorrate, Map<String, Integer> providers, String rinterface, String method,Map<String,String> args) {
        if (providers == null || providers.size() == 0)
            return "no providers";
        return httpDoSelect(map,errorrate,providers,rinterface,method,args);
    }
    protected abstract String httpDoSelect(Map<String, List<AtomicInteger>> map, float errorrate, Map<String, Integer> providers, String rinterface, String method, Map<String,String> args);
}
