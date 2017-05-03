package com.alibaba.dubbo.common;

import java.util.List;
import java.util.Map;

/**
 * Created by lzg on 2017/3/30.
 */
public interface HttpClient {
    String doStart(String group, String rinterface, String method,Map<String,String> args);
    Map<String,Map<String,Map<String,List<Object>>>> getMap();
    Map<String,Map<String,Map<String,String>>> getLoadBalanceMap();
    String updateWeight(String group, String rinterface, String provider, String data);
    String updateLoadBalance(String group, String rinterface, String method, String data);
}
