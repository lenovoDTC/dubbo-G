/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common;

import java.util.List;

public class EidNode {

	private String eid;
	private EidNode parent;
	private String path;

	/**
	 * @return the eid
	 */
	public String getEid() {
		return eid;
	}

	/**
	 * @param eid
	 *            the eid to set
	 */
	public void setEid(String eid) {
		this.eid = eid;
	}

	/**
	 * @return the parent
	 */
	public EidNode getParent() {
		return parent;
	}

	/**
	 * @param parent
	 */
	public void setParent(EidNode parent) {
		if (eid.equals("default")) {
			parent = null;
		} 
			this.parent = parent;
		
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 */
	public void setPath() {
		String path;
		if (eid.equals("default")) {
			path = "/default";
		} else {
			path = getParent().getPath() + "/" + eid;
		}
		this.path = path;
	}

}
