package com.alibaba.dubbo.rpc.cluster.support.wrapper;

import com.alibaba.dubbo.common.utils.HttpClient;
import com.alibaba.dubbo.rpc.cluster.HttpLoadBalance;
import com.alibaba.dubbo.rpc.cluster.HttpMockinterface;
import com.alibaba.dubbo.rpc.cluster.loadbalance.HttpRandomLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.HttpRoundRobinLoadBalance;

import java.util.Map;

/**
 * Created by lzg on 2017/4/1.
 */
public class HttpMock implements HttpMockinterface{
    private Integer number = 0;

    public String httpMockCluster(String methodloadBalance, Map<String, Integer> providers, String method, String schema, String args) {
        String result;
        if(methodloadBalance.equals("roundrobin")){
            HttpLoadBalance dothis = new HttpRoundRobinLoadBalance();
            result = dothis.httpSelect(providers,method,schema,args);
        }else{
            HttpLoadBalance dothis = new HttpRandomLoadBalance();
            result = dothis.httpSelect(providers,method,schema,args);
        }
        return result;
    }
}
