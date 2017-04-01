package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.rpc.cluster.HttpLoadBalance;

import java.util.Map;

/**
 * Created by lzg on 2017/3/28.
 */
public abstract class HttpAbstractLoadBalance implements HttpLoadBalance {

    public String httpSelect(Map<String,Integer> providers,String method,String schema,String args) {
        if (providers == null || providers.size() == 0)
            return "no providers";
        return httpDoSelect(providers,method,schema,args);
    }
    protected abstract String httpDoSelect(Map<String,Integer> providers, String method, String schema, String args);
}
