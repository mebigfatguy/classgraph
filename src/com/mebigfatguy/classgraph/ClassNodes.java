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

public class ClassNodes implements Iterable<ClassNode> {

    private ClassFinder clsFinder;
    private Map<String, ClassNode> nodes = new ConcurrentHashMap<>();
    
    public ClassNodes(ClassFinder finder) {
        clsFinder = finder;
    }
    
    /**
     * add both nodes to the map, but only put the classes with later
     * names as dependencies of classes with earlier names, so we don't
     * duplicate the relationship.
     * 
     * @param clsName1
     * @param clsName2
     */
    public void addRelationship(String clsName1, String clsName2) {
        
        int cmp = clsName1.compareTo(clsName2);
        if (cmp == 0) {
            return;
        }
        
        if (cmp < 0) {
            addNodeToNode(clsName1, clsName2);
            addNodeToNode(clsName2, null);
        } else {
            addNodeToNode(clsName1, null);
            addNodeToNode(clsName2, clsName1);
        }
    }
    
    /**
     * find the attraction count by looking at the earlier name's relationship
     * map
     * @param node
     * @return
     */
    public int getAttractionBetween(ClassNode node1, ClassNode node2) {
        
        if (node1.getFQCN().compareTo(node2.getFQCN()) > 0) {
            ClassNode tmp = node1;
            node1 = node2;
            node2 = tmp;
        }
        
        Integer attraction = node1.getRelationships().get(node2.getFQCN());
        if (attraction == null) {
            return 0;
        }
        
        return attraction.intValue();
    }
    
    private void addNodeToNode(String clsName1, String clsName2) {
        ClassNode node = nodes.get(clsName1);
        if (node == null) {
            
            node = new ClassNode(clsName1, clsFinder.classStatus(clsName1));
            nodes.put(clsName1,  node);
        }
        if (clsName2 != null)
            node.addRelationship(clsName2);
    }

    Map<String, ClassNode> get() {
        return nodes;
    }
    
    @Override
    public Iterator<ClassNode> iterator() {
        return nodes.values().iterator();
    }
}
