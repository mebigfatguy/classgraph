/*
 * ClassGraph - a dependency graph display
 * Copyright 2013-2015 MeBigFatGuy.com
 * Copyright 2013-2015 Dave Brosius
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

    private String fqcn;
    private ClassType clsType;
    private float[] position = { 0.0f, 0.0f, 0.0f };

    private Map<String, Float> relationships = new ConcurrentHashMap<>();

    public ClassNode(String className, ClassType classType) {
        fqcn = className;
        clsType = classType;
    }

    public void addRelationship(String clsName, RelationshipType type) {
        Float count = relationships.get(clsName);
        if (count == null) {
            count = Float.valueOf(type.getWeight());
        } else {
            count = Float.valueOf(count.floatValue() + type.getWeight());
        }
        relationships.put(clsName, count);
    }

    public Map<String, Float> getRelationships() {
        return Collections.unmodifiableMap(relationships);
    }

    public String getFQCN() {
        return fqcn;
    }

    public ClassType getType() {
        return clsType;
    }

    public float[] getColor() {
        return clsType.color();
    }

    public float[] getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof ClassNode)) {
            return false;
        }

        ClassNode that = (ClassNode) o;

        return fqcn.equals(that.fqcn);
    }

    @Override
    public int hashCode() {
        return fqcn.hashCode();
    }

    @Override
    public String toString() {
        return fqcn;
    }
}
