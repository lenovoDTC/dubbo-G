package com.alibaba.dubbo.rpc.filter;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import javassist.expr.NewArray;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.json.JSONObject;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

public class FuseFilter implements Filter {

	private int total = 0;
	private int successNumber = 0;
	private int oldtotal = 0;
	private int oldsuccessNumber = 0;
	private long start = System.currentTimeMillis();
	private long start1 = System.currentTimeMillis();
	private JSONObject jsonObject = new JSONObject();
	// private String methodLimit =
	// GetValueByKey("dubbo.properties","dubbo.method.limit");
	private static Logger log = LoggerFactory.getLogger(FuseFilter.class);

	// public static String GetValueByKey(String filePath, String key) {
	// Properties pps = new Properties();
	// try {
	// InputStream in = new BufferedInputStream(new FileInputStream(
	// filePath));
	// pps.load(in);
	// String value = pps.getProperty(key);
	// System.out.println(key + " = " + value);
	// return value;
	//
	// } catch (IOException e) {
	// e.printStackTrace();
	// return null;
	// }
	// }

	@Activate(group = Constants.CONSUMER)
	public Result invoke(Invoker<?> invoker, Invocation invocation)
			throws RpcException {
		long elapsed = System.currentTimeMillis() - start;
		long elapsed1 = System.currentTimeMillis() - start1;
		if (elapsed / 1000 >= 300) {
			start = System.currentTimeMillis();
			jsonObject.put("total", "" + total);
			try {
				ZooKeeper zkClient = new ZooKeeper(Constants.ZKSERVERS, 600000,
						null);
				try {
					String bb = "{}";
					byte[] aa = zkClient.getData("/dubbo/"
							+ invoker.getInterface().toString().substring(10),
							false, null);
					if (aa != null) {
						bb = new String(aa);
					}
					JSONObject aaJsonObject = new JSONObject(bb);
					Iterator iterator = jsonObject.keys();
					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						Long value = Long.parseLong(jsonObject.get(key)
								.toString());
						if (aaJsonObject.isNull(key)) {
							aaJsonObject.put(key, value);
							continue;
						}
						Long value1 = Long.parseLong(aaJsonObject.get(key)
								.toString());
						aaJsonObject.put(key, value + value1);
					}
					zkClient.setData("/dubbo/"
							+ invoker.getInterface().toString().substring(10),
							(aaJsonObject.toString()).getBytes(), -1);
					zkClient.close();
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			jsonObject = new JSONObject();
			total = 0;
		}
		total++;
		if (elapsed1 / 1000 >= 10) {
			int newsuccessNumber = successNumber;
			int newtotal = total;
			String ss = invoker
					.getUrl()
					.toString()
					.substring(
							invoker.getUrl().toString().indexOf("&errorrate") + 11);
			String s = ss.substring(0, ss.indexOf("&"));
			if (Float.parseFloat(s) != 0) {
				if (newtotal != oldtotal) {
					if (10 - ((float) (newsuccessNumber - oldsuccessNumber)
							/ (newtotal - oldtotal) * 10) >= Float
								.parseFloat(s)) {
						if (elapsed1 / 1000 >= 30) {
							start1 = System.currentTimeMillis();
							oldsuccessNumber = successNumber;
							oldtotal = total;
						}
						log.info("-----------------------------熔--------------------------断---------------------------------");
						return null;
					}
				}
			}
			start1 = System.currentTimeMillis();
			oldsuccessNumber = successNumber;
			oldtotal = total;
		}
		Result result = invoker.invoke(invocation);
		// if (jsonObject.isNull(invocation.getMethodName())) {
		// jsonObject.put(invocation.getMethodName(), (long) 0);
		// }
		// Object successNumber = jsonObject.get(invocation.getMethodName());
		// jsonObject.put(invocation.getMethodName(), (Long) successNumber + 1);
		successNumber++;
		jsonObject.put("success", "" + successNumber);
		return result;
	}
}