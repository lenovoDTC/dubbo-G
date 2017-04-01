package com.alibaba.dubbo.rpc.cluster;

import java.util.List;
import java.util.Map;

/**
 * Created by lzg on 2017/3/28.
 */
public interface HttpLoadBalance {
    String httpSelect(Map<String,Integer> providers, String method, String schema, String args);
}
