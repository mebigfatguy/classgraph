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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.core.util.Loader;

public class ClassNodes implements Iterable<ClassNode> {

    private ClassFinder clsFinder;
    private Map<String, ClassNode> nodes = new ConcurrentHashMap<>();
    
    public ClassNodes(ClassFinder finder) {
        clsFinder = finder;
    }
    
    public void addRelationship(String clsName1, String clsName2) {
        addNodeToNode(clsName1, clsName2);
        addNodeToNode(clsName2, clsName1);
    }
    
    private void addNodeToNode(String clsName1, String clsName2) {
        ClassNode node = nodes.get(clsName1);
        if (node == null) {
            
            node = new ClassNode(clsName1, clsFinder.classStatus(clsName1));
            nodes.put(clsName1,  node);
        }
        node.addRelationship(clsName2);
    }

    @Override
    public Iterator<ClassNode> iterator() {
        return nodes.values().iterator();
    }
}
