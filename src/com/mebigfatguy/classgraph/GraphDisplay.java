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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

public class GraphDisplay {

    private static final float RADIUS = 6.378f;
    private static final int SLICES = 16;
    private static final int STACKS = 16;
    
    private static final float[] AMBIENT = { 0.7f, 0.7f, 0.7f, 1 };
    private static final float[] SPECULAR = { 0.5f, 0.5f, 0.5f, 1 };
    private static final float[] DIFFUSE = { 1, 1, 1, 1 };
    private static final float[] LIGHT_POSITION = { 0,3000,2000,1 };
    
    private ClassNodes classNodes;
    private Animator animator;
    private Thread modifier;
    private GLWindow glWindow;
    private float[] eyeLocation = { 0, 0, 500 };
    
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
        glWindow.addKeyListener(new GDKeyListener());
        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent arg0) {
                System.exit(0);
            }
        });
        centerWindow(glWindow);
        
        animator = new Animator();
        animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator.setExclusiveContext(false);
        
        animator.add(glWindow);
        animator.start();
        
        modifier = new Thread(new Modifier());
        modifier.setName("modifier");
        modifier.start();
        
        glWindow.setVisible(true);
        animator.setUpdateFPSFrames(10, null);
    }
    
    public void terminate() {
        try {
            modifier.interrupt();
            modifier.join();
        } catch (InterruptedException ie) {
        }
        
        animator.stop();
        glWindow.destroy();
    }
    
    private void updateNodes() {
        for (ClassNode node : classNodes) {
            float[] pos = node.getPosition();
            
            pos[0] += ((Math.random() * 2) - 1);
            pos[1] += ((Math.random() * 2) - 1);
            pos[2] += ((Math.random() * 2) - 1);
        }
    }
    
    private void centerWindow(GLWindow window) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle screenBounds = gc.getBounds();
        
        int w = window.getWidth();
        int h = window.getHeight();
        
        window.setPosition((screenBounds.width - w) / 2, ((screenBounds.height - h) / 3));
    }
    
    class GDEvents implements GLEventListener {
 
        private GLU glu;
        private int sphereList;
        
        @Override
        public void display(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();
            
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            for (ClassNode node : classNodes) {
                
                float[] color = node.getColor();
                gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT, color, 0);
                gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR, color, 0);
                gl.glMaterialf(GL.GL_FRONT, GLLightingFunc.GL_SHININESS, 0.5f);
                
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
            
            gl.glEnable( GLLightingFunc.GL_LIGHTING );
            gl.glEnable( GLLightingFunc.GL_LIGHT0 );
            
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, AMBIENT, 0);
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, SPECULAR, 0);
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, DIFFUSE, 0);
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, LIGHT_POSITION, 0);
            
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
            glu.gluLookAt(eyeLocation[0], eyeLocation[1], eyeLocation[2], 0, 0, 0, 0, 1, 0);

            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
        }  
    }
    
    class GDKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {

            GL2 gl = glWindow.getGL().getGL2();
            gl.getContext().makeCurrent();
            
            try {
            
                gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
                gl.glLoadIdentity();
                
                int keyCode = e.getKeyCode();
                if ((keyCode == KeyEvent.VK_UP)) {
                    eyeLocation[0] *= 0.95f;
                    eyeLocation[1] *= 0.95f;
                    eyeLocation[2] *= 0.95f;
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    eyeLocation[0] *= 1.05f;
                    eyeLocation[1] *= 1.05f;
                    eyeLocation[2] *= 1.05f;
                }
                GLU glu = new GLU();
                float widthHeightRatio = (float) glWindow.getWidth() / (float) glWindow.getHeight();
                glu.gluPerspective(45, widthHeightRatio, 1, 1000);
                glu.gluLookAt(eyeLocation[0], eyeLocation[1], eyeLocation[2], 0, 0, 0, 0, 1, 0);
                
                gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            } finally {
                gl.getContext().release();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
    
    class Modifier implements Runnable {
        public void run() {
            try {         
                while (!Thread.interrupted()) {
                    Thread.sleep(500);
                    updateNodes();
                }
            } catch (InterruptedException ie) {
            }
        }
    }
}
