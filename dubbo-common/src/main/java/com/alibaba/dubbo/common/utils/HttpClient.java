package com.alibaba.dubbo.common.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpClient {
	private static long start = System.currentTimeMillis();
	private static Map<String, AtomicInteger> maptotal = new HashMap<String, AtomicInteger>();

	public static String httpClient(Map<String, List<AtomicInteger>> map,
									float errorrate, String uri, String rinterface, String method,Map<String,String> args) {
		if (System.currentTimeMillis() - start > 1000) {
			start = System.currentTimeMillis();
			maptotal = new HashMap<String, AtomicInteger>();
		}
		String response = "dubbo.http.error";
		String threads = "100";
		if (uri.indexOf("threads%3D") != -1) {
			threads = uri.substring(uri.indexOf("threads%3D")).substring(10,
					uri.substring(uri.indexOf("threads%3D")).indexOf("%26"));
		}
		uri = "http://"
				+ uri.substring(14)
						.substring(0, uri.substring(14).indexOf("%2F"))
						.replace("%3A", ":");
		if (maptotal.containsKey(uri)) {
			maptotal.get(uri).incrementAndGet();
		} else {
			maptotal.put(uri, new AtomicInteger(1));
		}
		if (map.containsKey(uri)) {
			float i = (float) map.get(uri).get(1).get()
					/ (map.get(uri).get(0).get() + map.get(uri).get(1).get());
			if (i * 100 > errorrate)
				return response + uri + "熔断";// 熔断
			float env = Float.parseFloat(threads)
					/ (map.get(uri).get(2).get() / map.get(uri).get(0).get())
					* 1000;
			if (maptotal.get(uri).get() > env)
				return response + uri + "降级";// 降级
		}
		org.apache.http.client.HttpClient httpClient = new DefaultHttpClient();
		// 请求超时
		httpClient.getParams().setParameter(
				CoreConnectionPNames.CONNECTION_TIMEOUT, 6000);
		// 读取超时
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
				6000);
		HttpPost httpPost = new HttpPost(uri+"/"+rinterface+"/"+method);
		List <NameValuePair> params = new ArrayList<NameValuePair>();
		for(String arg : args.keySet()){
			params.add(new BasicNameValuePair(arg, args.get(arg)));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			HttpResponse httpResponse = httpClient.execute(httpPost);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				HttpEntity httpEntity = httpResponse.getEntity();
				response = EntityUtils.toString(httpEntity);
				return response + "dubbo.http.success" + uri;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return response + uri;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return response + uri;
		}
		return response + uri;
	}
}
