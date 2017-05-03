package com.alibaba.dubbo.rpc.cluster.support.wrapper;

import com.alibaba.dubbo.rpc.cluster.HttpLoadBalance;
import com.alibaba.dubbo.rpc.cluster.HttpMockinterface;
import com.alibaba.dubbo.rpc.cluster.loadbalance.HttpRandomLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.HttpRoundRobinLoadBalance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lzg on 2017/4/1.
 */
public class HttpMock implements HttpMockinterface{
    private Integer number = 0;
    private long start = System.currentTimeMillis();
    private Map<String,List<AtomicInteger>> map = new HashMap<String, List<AtomicInteger>>();
    public String httpMockCluster(float errorrate, String methodloadBalance, Map<String, Integer> providers, String rinterface, String method,Map<String,String> args) {
        if (System.currentTimeMillis()-start>10000){
            map = new HashMap<String, List<AtomicInteger>>();
            start = System.currentTimeMillis();
        }
        String result;
        if(methodloadBalance.equals("roundrobin")){
            HttpLoadBalance dothis = new HttpRoundRobinLoadBalance();
            long timeBefore = System.currentTimeMillis();
            result = dothis.httpSelect(map,errorrate,providers,rinterface,method,args);
            long time = System.currentTimeMillis()-timeBefore;
            count(result,time);
        }else{
            HttpLoadBalance dothis = new HttpRandomLoadBalance();
            long timeBefore = System.currentTimeMillis();
            result = dothis.httpSelect(map,errorrate,providers, rinterface, method,args);
            long time = System.currentTimeMillis()-timeBefore;
            count(result,time);
        }
        return result;
    }
    private void count(String result,long time){
        if(result.indexOf("dubbo.http.success")!=-1){
            String check = result.substring(result.indexOf("dubbo.http.success")+18);
            if(map.containsKey(check)){
                map.get(check).get(0).incrementAndGet();
                map.get(check).get(2).set(map.get(check).get(2).get()+(int)time);
            }else {
                AtomicInteger first = new AtomicInteger(1);
                AtomicInteger second = new AtomicInteger();
                AtomicInteger time2 = new AtomicInteger((int) time);
                List<AtomicInteger> list = new ArrayList<AtomicInteger>();
                list.add(first);
                list.add(second);
                list.add(time2);
                map.put(check,list);
            }
        }else if (result.indexOf("dubbo.http.error")!=-1){
            String check = result.substring(result.indexOf("dubbo.http.error")+16);
            if(map.containsKey(check)){
                map.get(check).get(1).incrementAndGet();
            }else {
                AtomicInteger first = new AtomicInteger();
                AtomicInteger second = new AtomicInteger(1);
                AtomicInteger time2 = new AtomicInteger();
                List<AtomicInteger> list = new ArrayList<AtomicInteger>();
                list.add(first);
                list.add(second);
                list.add(time2);
                map.put(check,list);
            }
        }
    }
}
