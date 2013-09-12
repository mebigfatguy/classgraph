/*
 * ClassGraph - a dependency graph display
 * Copyright 2013 MeBigFatGuy.com
 * Copyright 2013 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.classgraph;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassNode {

    private static final float[] APPLICATION_COLOR = { 0.0f, 1.0f, 0.0f };
    private static final float[] SYSTEM_COLOR = { 1.0f, 0.0f, 0.0f };
    
	private String packageName;
	private String clsName;
	private float[] color;

	private Map<String, Integer> relationships = new ConcurrentHashMap<>();
	
	public ClassNode(String fqcn) {	
		int dotPos = fqcn.lastIndexOf('.');
		if (dotPos >= 0) {
			packageName = fqcn.substring(0, dotPos);
			clsName = fqcn.substring(dotPos+1);
		} else {
			packageName = "";
			clsName = fqcn;
		}
		
		if (packageName.startsWith("java.") || packageName.startsWith("javax."))
		    color = SYSTEM_COLOR;
		else
		    color = APPLICATION_COLOR;
	}
	
	public void addRelationship(String clsName) {
		Integer count = relationships.get(clsName);
		if (count == 0) {
			count = Integer.valueOf(1);
		} else {
			count = Integer.valueOf(count.intValue() + 1);
		}
		relationships.put(clsName, count);
	}
	
	public Map<String, Integer> getRelationships() {
		return Collections.unmodifiableMap(relationships);
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public String getClsName() {
		return clsName;
	}
	
	public String getFQCN() {
		return packageName.length() > 0 ? packageName + '.' + clsName : clsName;
	}
	
	public float[] getColor() {
	    return color;
	}
	
	public boolean equals(Object o) {
		if (o == this) return true;
	
		if (!(o instanceof ClassNode)) {
			return false;
		}
		
		ClassNode that = (ClassNode) o;
		
		return packageName.equals(that.getPackageName()) && clsName.equals(that.getClsName());
	}
	
	public int hashCode() {
		return (packageName.hashCode() * 17) ^ clsName.hashCode();
	}
}
