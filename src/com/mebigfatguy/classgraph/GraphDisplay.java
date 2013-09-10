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

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

public class GraphDisplay {

    private ClassNodes classNodes;
    private Animator animator;
    private GLWindow glWindow;
    
    public GraphDisplay(ClassNodes nodes) {
        classNodes = nodes;
    }
    
    public void display() {
        GLProfile profile = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities( profile );
        caps.setBackgroundOpaque(true);
        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        glWindow = GLWindow.create(screen, caps);
        glWindow.setTitle("ClassGraph");
        glWindow.setSize(800, 600);
        
        animator = new Animator();
        animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator.setExclusiveContext(false);
        
        animator.add(glWindow);
        animator.start();
        
        glWindow.setVisible(true);
        animator.setUpdateFPSFrames(15, null);
    }
    
    public void terminate() {
        animator.stop();
        glWindow.destroy();
    }
}
