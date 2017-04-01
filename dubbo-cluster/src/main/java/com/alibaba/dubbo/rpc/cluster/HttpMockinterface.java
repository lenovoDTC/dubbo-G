package com.alibaba.dubbo.rpc.cluster;

import java.util.Map;

/**
 * Created by lzg on 2017/4/1.
 */
public interface HttpMockinterface {
    String httpMockCluster(String methodloadBalance, Map<String,Integer> providers, String method, String schema, String args);
}
