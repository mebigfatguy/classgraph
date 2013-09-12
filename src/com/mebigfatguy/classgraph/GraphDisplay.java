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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
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
        
        glWindow.addGLEventListener(new GDEvents());
        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent arg0) {
                System.exit(0);
            }
        });
        
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
    
    public void updateNodes() {
        for (ClassNode node : classNodes) {
            float[] pos = node.getPosition();
            
            pos[0] += ((Math.random() * 2) - 1);
            pos[1] += ((Math.random() * 2) - 1);
            pos[2] += ((Math.random() * 2) - 1);
        }
    }
    
    class GDEvents implements GLEventListener {

        private static final float RADIUS = 6.378f;
        private static final int SLICES = 16;
        private static final int STACKS = 16;
        
        private GLU glu;
        private int sphereList;
        
        
        @Override
        public void display(GLAutoDrawable drawable) {
            updateNodes();
            GL2 gl = drawable.getGL().getGL2();
            
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            for (ClassNode node : classNodes) {
                
                float[] color = node.getColor();
                gl.glColor3f(color[0], color[1], color[2]);  
                gl.glPushMatrix();
                try {
                    float[] pos = node.getPosition();
                    gl.glTranslatef(pos[0], pos[1], pos[2]);
                    gl.glCallList(sphereList);
                } finally {
                    gl.glPopMatrix();
                }
            }
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            glu = new GLU();
            
            GL2 gl = drawable.getGL().getGL2();
            sphereList = gl.glGenLists(1);
            gl.glNewList(sphereList, GL2.GL_COMPILE);
            
            GLUquadric nodeGraphic = glu.gluNewQuadric();
            glu.gluQuadricDrawStyle(nodeGraphic, GLU.GLU_FILL);
            glu.gluQuadricNormals(nodeGraphic, GLU.GLU_SMOOTH);
            glu.gluQuadricOrientation(nodeGraphic, GLU.GLU_OUTSIDE);

            glu.gluSphere(nodeGraphic, RADIUS, SLICES, STACKS);
            glu.gluDeleteQuadric(nodeGraphic);
            gl.glEndList();
     
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2 gl = drawable.getGL().getGL2();
            gl.glViewport(0, 0, width, height);
            
            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();
            
            float widthHeightRatio = (float) drawable.getWidth() / (float) drawable.getHeight();
            glu.gluPerspective(45, widthHeightRatio, 1, 1000);
            glu.gluLookAt(0, 0, 1000, 0, 0, 0, 0, 1, 0);

            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
        }  
    }
}
