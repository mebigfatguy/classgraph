/*
 * ClassGraph - a dependency graph display
 * Copyright 2013-2019 MeBigFatGuy.com
 * Copyright 2013-2019 Dave Brosius
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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class ClassGraphBuildingVisitor extends ClassVisitor {

    private ClassNodes classNodes;
    private String clsName;

    public ClassGraphBuildingVisitor(ClassNodes nodes) {
        super(Opcodes.ASM5);
        classNodes = nodes;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        clsName = internalToExternalName(name);
        String superClsName = internalToExternalName(superName);

        classNodes.addRelationship(clsName, superClsName, RelationshipType.INHERITANCE);

        for (String inf : interfaces) {
            String interfaceClsName = internalToExternalName(inf);
            classNodes.addRelationship(clsName, interfaceClsName, RelationshipType.INTERFACE);
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (desc.startsWith("L")) {
            String fieldClsName = internalToExternalName(desc.substring(1, desc.length() - 1));
            classNodes.addRelationship(clsName, fieldClsName, RelationshipType.CONTAINMENT);
        }
        return null;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {

        if ((name == null) || (outerName == null)) {
            return;
        }

        String outerClsName = internalToExternalName(outerName);
        String innerClsName = internalToExternalName(name);

        classNodes.addRelationship(outerClsName, innerClsName, RelationshipType.INNER);
    }

    private static String internalToExternalName(String name) {
        return name.replace('/', '.');
    }
}
