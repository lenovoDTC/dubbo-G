package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.utils.AtomicPositiveInteger;
import com.alibaba.dubbo.common.utils.HttpClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by lzg on 2017/3/28.
 */
public class HttpRoundRobinLoadBalance extends HttpAbstractLoadBalance {
    public static final String NAME = "roundrobin";

    private final ConcurrentMap<String, AtomicPositiveInteger> sequences = new ConcurrentHashMap<String, AtomicPositiveInteger>();

    private static final class IntegerWrapper {
        public IntegerWrapper(int value) {
            this.value = value;
        }

        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void decrement() {
            this.value--;
        }
    }

    protected String httpDoSelect(Map<String, List<AtomicInteger>> map, float errorrate, Map<String, Integer> providers, String rinterface, String method, Map<String,String> args)
    {
        Set<String> keys = providers.keySet();
        List<String> list = new ArrayList<String>(keys);
        String key = list.get(0);
        int length = providers.size(); // 总个数
        int maxWeight = 0; // 最大权重
        int minWeight = Integer.MAX_VALUE; // 最小权重
        final LinkedHashMap<String, IntegerWrapper> invokerToWeightMap = new LinkedHashMap<String, IntegerWrapper>();
        int weightSum = 0;
        for (String keyy:keys) {
            int weight = providers.get(keyy);
            maxWeight = Math.max(maxWeight, weight); // 累计最大权重
            minWeight = Math.min(minWeight, weight); // 累计最小权重
            if (weight > 0) {
                invokerToWeightMap.put(keyy, new IntegerWrapper(weight));
                weightSum += weight;
            }
        }
        if(maxWeight == 0)return "no providers";
        AtomicPositiveInteger sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new AtomicPositiveInteger());
            sequence = sequences.get(key);
        }
        int currentSequence = sequence.getAndIncrement();
        if (maxWeight > 0 && minWeight < maxWeight) { // 权重不一样
            int mod = currentSequence % weightSum;
            for (int i = 0; i < maxWeight; i++) {
                for (Map.Entry<String, IntegerWrapper> each : invokerToWeightMap.entrySet()) {
                    final String k = each.getKey();
                    final IntegerWrapper v = each.getValue();
                    if (mod == 0 && v.getValue() > 0) {
                        return k;
                    }
                    if (v.getValue() > 0) {
                        v.decrement();
                        mod--;
                    }
                }
            }
        }
        // 取模轮循
        return HttpClient.httpClient(map,errorrate,list.get(currentSequence % length),rinterface,method,args);
    }
}