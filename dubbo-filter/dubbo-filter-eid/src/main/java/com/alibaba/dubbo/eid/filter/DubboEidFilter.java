package com.alibaba.dubbo.eid.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.RpcContext;

public class DubboEidFilter implements Filter {

	public void init(FilterConfig filterConfig) throws ServletException {

	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String eid = ((HttpServletRequest) request).getHeader("X-Request-EID");
		if (eid == null)
			eid = Constants.DEFAULT_EID;
		RpcContext.getContext().addHeader(Constants.GENERIC_HEADER_EID, eid);
	}

	public void destroy() {

	}

}
