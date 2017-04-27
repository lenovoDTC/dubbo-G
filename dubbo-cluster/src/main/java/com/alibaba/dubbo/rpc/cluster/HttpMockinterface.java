package com.alibaba.dubbo.rpc.cluster;

import java.util.Map;

/**
 * Created by lzg on 2017/4/1.
 */
public interface HttpMockinterface {
    String httpMockCluster(float errorrate, String methodloadBalance, Map<String, Integer> providers, String rinterface, String method, Map<String,String> args);
}
