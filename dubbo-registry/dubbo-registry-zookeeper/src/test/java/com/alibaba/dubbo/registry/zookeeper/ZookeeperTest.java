package com.alibaba.dubbo.registry.zookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;

public class ZookeeperTest {
	public static void main (String[] args) {
		// 方式一
		 ZkClient zkClient = new ZkClient("10.120.26.4:2181", 1000);
		 zkClient.setZkSerializer(new BytesPushThroughSerializer());

		byte[] dataBytes = zkClient.readData("/eid");
		String data = new String(dataBytes);
		System.out.println("eid" + " data :" + data);

		zkClient.close();
		
	}
}
