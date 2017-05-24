package com.alibaba.dubbo.registry.zookeeper;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.HttpClient;

import com.alibaba.dubbo.rpc.cluster.HttpLoadBalance;
import com.alibaba.dubbo.rpc.cluster.HttpMockinterface;
import com.alibaba.dubbo.rpc.cluster.loadbalance.HttpRandomLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.HttpRoundRobinLoadBalance;
import com.alibaba.dubbo.rpc.cluster.support.wrapper.HttpMock;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.json.JSONObject;

import java.util.*;

import org.I0Itec.zkclient.ZkClient;

/**
 * Created by lzg on 2017/3/29.
 */
public class HttpZookeeperRegistry implements HttpClient{
    private final static Logger logger = LoggerFactory.getLogger(HttpZookeeperRegistry.class);

    private final String weightDefault = "100";

    private final String loadBalanceDefault = "random";

    private Map<String,Map<String,Map<String,List<Object>>>> map = new HashMap<String, Map<String,Map<String,List<Object>>>>();

    private Map<String,Map<String,Map<String,String>>> loadBalanceMap = new HashMap<String, Map<String, Map<String, String>>>();

    private float errorrate;

    private ZkClient zkClient;

    private HttpMockinterface httpMockinterface;
    public HttpZookeeperRegistry(float errorrate,String host) {
    	new ZKLoadBalanceDataListener();
    	new ZKProviderDataListener();
    	new ZKProviderChildListener();
        new ZKProviderChildDataListener();
        this.errorrate = errorrate;
        httpMockinterface = new HttpMock();
        zkClient = new ZkClient(host);
        zkClient.subscribeChildChanges("/http", new ZKChildListener());
        if (zkClient.exists("/http")){
        List<String> groups = zkClient.getChildren("/http");
        for(String group:groups){
            Map<String,Map<String,List<Object>>> interfacemap = new HashMap<String, Map<String,List<Object>>>();
            Map<String,Map<String,String>> Rinterfacemap = new HashMap<String, Map<String, String>>();
            List<String> interfaces = zkClient.getChildren("/http/"+group);
            for(String rinterface:interfaces){
                Map<String,List<Object>> providermap = new HashMap<String,List<Object>>();
                Map<String,String> methodmap = new HashMap<String, String>();
                List<String> providers = zkClient.getChildren("/http/"+group+"/"+rinterface+"/providers");
                List<String> methods = zkClient.getChildren("/http/"+group+"/"+rinterface+"/loadbalance");
                String method =zkClient.readData("/http/"+group+"/"+rinterface+"/providers");
                JSONObject jsonObject = new JSONObject(method);
                zkClient.subscribeChildChanges("/http/"+group+"/"+rinterface+"/providers", new ZKProviderChildListener());//增加提供者节点监听
                zkClient.subscribeDataChanges("/http/"+group+"/"+rinterface+"/providers", new ZKProviderDataListener());//增加提供者节点监听
                for(String provider:providers){
                    zkClient.subscribeDataChanges("/http/"+group+"/"+rinterface+"/providers/"+provider,new ZKProviderChildDataListener());
                    String weight = zkClient.readData("/http/"+group+"/"+rinterface+"/providers/"+provider);
                    if(weight==null)weight=weightDefault;
                    List<Object> info  = new ArrayList<Object>();
                    info.add(weight);
                    info.add(jsonObject);
                    providermap.put(provider,info);
                }
                interfacemap.put(rinterface,providermap);
                for (String method1:methods){
                    zkClient.subscribeDataChanges("/http/"+group+"/"+rinterface+"/loadbalance/"+method1,new ZKLoadBalanceDataListener());
                    String loadBalance = zkClient.readData("/http/"+group+"/"+rinterface+"/loadbalance/"+method1);
                    if(loadBalance==null||loadBalance.equals(""))loadBalance=loadBalanceDefault;
                    methodmap.put(method1,loadBalance);
                }
                Rinterfacemap.put(rinterface,methodmap);
            }
            map.put(group,interfacemap);
            loadBalanceMap.put(group,Rinterfacemap);
        }
            logger.info("Map success");
        }
        else {
            logger.info("No http protocol provider in this zookeeper");
        }
    }
    private class ZKChildListener implements IZkChildListener {
        /**
         * handleChildChange： 用来处理服务器端发送过来的通知 parentPath：对应的父节点的路径
         * currentChilds：子节点的相对路径
         */
        public void handleChildChange(String parentPath,
                                      List<String> currentChilds) throws Exception {
            List<String> groups = zkClient.getChildren("/http");
            for(String group:groups){
                Map<String,Map<String,List<Object>>> interfacemap = new HashMap<String, Map<String,List<Object>>>();
                Map<String,Map<String,String>> Rinterfacemap = new HashMap<String, Map<String, String>>();
                List<String> interfaces = zkClient.getChildren("/http/"+group);
                for(String rinterface:interfaces){
                    Map<String,List<Object>> providermap = new HashMap<String,List<Object>>();
                    Map<String,String> methodmap = new HashMap<String, String>();
                    List<String> providers = zkClient.getChildren("/http/"+group+"/"+rinterface+"/providers");
                    List<String> methods = zkClient.getChildren("/http/"+group+"/"+rinterface+"/loadbalance");
                    String method =zkClient.readData("/http/"+group+"/"+rinterface+"/providers");
                    JSONObject jsonObject = new JSONObject(method);
                    zkClient.subscribeChildChanges("/http/"+group+"/"+rinterface+"/providers", new ZKProviderChildListener());//增加提供者节点监听
                    zkClient.subscribeDataChanges("/http/"+group+"/"+rinterface+"/providers", new ZKProviderDataListener());//增加提供者节点监听
                    for(String provider:providers){
                        zkClient.subscribeDataChanges("/http/"+group+"/"+rinterface+"/providers/"+provider,new ZKProviderChildDataListener());
                        String weight = zkClient.readData("/http/"+group+"/"+rinterface+"/providers/"+provider);
                        if(weight==null)weight=weightDefault;
                        List<Object> info  = new ArrayList<Object>();
                        info.add(weight);
                        info.add(jsonObject);
                        providermap.put(provider,info);
                    }
                    interfacemap.put(rinterface,providermap);
                    for (String method1:methods){
                        zkClient.subscribeDataChanges("/http/"+group+"/"+rinterface+"/loadbalance/"+method1,new ZKLoadBalanceDataListener());
                        String loadBalance = zkClient.readData("/http/"+group+"/"+rinterface+"/loadbalance/"+method1);
                        if(loadBalance==null||loadBalance.equals(""))loadBalance=loadBalanceDefault;
                        methodmap.put(method1,loadBalance);
                    }
                    Rinterfacemap.put(rinterface,methodmap);
                }
                map.put(group,interfacemap);
                loadBalanceMap.put(group,Rinterfacemap);
            }
            logger.info("Map success");
        }
    }
    private class ZKProviderChildListener implements IZkChildListener {
        /**
         * handleChildChange： 用来处理服务器端发送过来的通知 parentPath：对应的父节点的路径
         * currentChilds：子节点的相对路径
         */
        public void handleChildChange(String parentPath,
                                      List<String> currentChilds) throws Exception {
            String[] path = parentPath.split("/");
            String method = zkClient.readData(parentPath);
            Map<String,List<Object>> b = new HashMap<String, List<Object>>();
            for(String Child:currentChilds){
                List<Object> list = new ArrayList<Object>();
                list.add(weightDefault);
                list.add(method);
                b.put(Child,list);
                zkClient.subscribeDataChanges(parentPath+ "/" + Child,new ZKProviderChildDataListener());
            }
            map.get(path[2]).put(path[3],b);
            logger.info("Map providers update");
        }
    }
    private class ZKProviderDataListener implements IZkDataListener {
        /**
         * dataPath 触发事件目录 data 修改数据
         */
        public void handleDataChange(String dataPath, Object data)
                throws Exception {
            String[] path = dataPath.split("/");
            List<Object> list = new ArrayList<Object>();
            list.add(weightDefault);
            list.add(data.toString());
            map.get(path[2]).get(path[3]).put(path[5],list);
            logger.info("Map update");
        }
        /**
         * dataPath 触发事件目录
         */
        public void handleDataDeleted(String dataPath) throws Exception {
            System.out.println(dataPath);
        }
    }
    private class ZKProviderChildDataListener implements IZkDataListener {
        /**
         * dataPath 触发事件目录 data 修改数据
         */
        public void handleDataChange(String dataPath, Object data)
                throws Exception {
            String[] path = dataPath.split("/");
            String method = map.get(path[2]).get(path[3]).get(path[5]).get(1).toString();
            List<Object> list = new ArrayList<Object>();
            list.add(data.toString());
            list.add(method);
            map.get(path[2]).get(path[3]).put(path[5],list);
            logger.info("Map weight update");
        }
        /**
         * dataPath 触发事件目录
         */
        public void handleDataDeleted(String dataPath) throws Exception {
            System.out.println(dataPath);
        }
    }
    private class ZKLoadBalanceDataListener implements IZkDataListener {
        /**
         * dataPath 触发事件目录 data 修改数据
         */
        public void handleDataChange(String dataPath, Object data)
                throws Exception {
            String[] path = dataPath.split("/");
            loadBalanceMap.get(path[2]).get(path[3]).put(path[5],data.toString());
            logger.info("LoadBalanceMap update");
        }
        /**
         * dataPath 触发事件目录
         */
        public void handleDataDeleted(String dataPath) throws Exception {
            System.out.println(dataPath);
        }
    }
    public String doStart(String group, String rinterface, String method,Map<String,String> args){
    	Map<String,Integer> providers = new HashMap<String, Integer>();
        if (!map.containsKey(group)){
            return "group does not exist";
        }
        if(!map.get(group).containsKey(rinterface)){
            return "interface does not exist";
        }
        if(!loadBalanceMap.get(group).get(rinterface).containsKey(method)){
            return "method does not exist";
        }
        for(String provider : map.get(group).get(rinterface).keySet()){
        	providers.put(provider, Integer.parseInt(map.get(group).get(rinterface).get(provider).get(0).toString()));
        }
        String methodloadBalance = loadBalanceMap.get(group).get(rinterface).get(method);
        return httpMockinterface.httpMockCluster(errorrate,methodloadBalance,providers,rinterface,method,args);
    }
    public Map<String,Map<String,Map<String,List<Object>>>> getMap(){
        return map;
    }
    public Map<String,Map<String,Map<String,String>>> getLoadBalanceMap(){
        return loadBalanceMap;
    }
    public String updateWeight(String group, String rinterface, String provider, String data){
        int i = Integer.parseInt(data);
        if(i<0&&i>100){
            return "data not in 0-100";
        }
        zkClient.writeData("/http/"+group+"/"+rinterface+"/providers/"+provider,data);
        return "success";
    }
    public String updateLoadBalance(String group, String rinterface, String method, String data){
        zkClient.writeData("/http/"+group+"/"+rinterface+"/loadbalance/"+method,data);
        return "success";
    }
}
