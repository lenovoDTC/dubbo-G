package com.alibaba.dubbo.remoting.zookeeper;

import java.util.List;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;

import com.alibaba.dubbo.common.URL;

public interface ZookeeperClient {
	
	String readData(String path);

	void create(String path, boolean ephemeral);
	
	void writeData(String path, Object object);

	void delete(String path);
	
	void subscribeDataChanges(String path, IZkDataListener listener);
	
	List<String> subscribeChildChanges(String path, IZkChildListener listener);

	List<String> getChildren(String path);

	List<String> addChildListener(String path, ChildListener listener);

	void removeChildListener(String path, ChildListener listener);

	void addStateListener(StateListener listener);
	
	void removeStateListener(StateListener listener);

	boolean isConnected();

	void close();

	URL getUrl();

}
