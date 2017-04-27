package com.alibaba.dubbo.rpc.cluster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lzg on 2017/3/28.
 */
public interface HttpLoadBalance {
    String httpSelect(Map<String, List<AtomicInteger>> map, float errorrate, Map<String, Integer> providers, String rinterface, String method,Map<String,String> args);
}
