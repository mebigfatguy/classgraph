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

import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.awt.TextRenderer;

public class GraphDisplay {

    private static final int FONT_SIZE = 6;

    private static final float RADIUS = 6.378f;
    private static final int SLICES = 12;
    private static final int STACKS = 12;

    private static final float LONG_REPEL_DISTANCE = RADIUS * 8.0f;
    private static final float LONG_REPEL_DISTANCE_SQUARED = LONG_REPEL_DISTANCE * LONG_REPEL_DISTANCE;

    private static final float SHORT_REPEL_DISTANCE = RADIUS * 3.0f;
    private static final float SHORT_REPEL_DISTANCE_SQUARED = SHORT_REPEL_DISTANCE * SHORT_REPEL_DISTANCE;

    private static final float ATTRACTION_DISTANCE = RADIUS * 4.0f;
    private static final float ATTRACTION_DISTANCE_SQUARED = ATTRACTION_DISTANCE * ATTRACTION_DISTANCE;

    private static final float[] ORIGIN = { 0.0f, 0.0f, 0.0f };
    private static final float STEP_SIZE = 10.0f;
    private static final float ROTATION_SIZE = (float) (Math.PI / 180.0f);

    private static final float[] AMBIENT = { 0.7f, 0.7f, 0.7f, 1 };
    private static final float[] SPECULAR = { 0.5f, 0.5f, 0.5f, 1 };
    private static final float[] DIFFUSE = { 1, 1, 1, 1 };
    private static final float[] LIGHT_POSITION = { 0, 3000, 2000, 1 };

    private ClassNodes classNodes;
    private Animator animator;
    private TextRenderer textRenderer;
    private Thread modifier;
    private GLWindow glWindow;
    private float[] eyeLocation = { 0, 0, 500 };
    private List<TerminationListener> listeners;

    public GraphDisplay(ClassNodes nodes) {
        classNodes = nodes;
        listeners = new ArrayList<>();
    }

    public void addTerminationListener(TerminationListener l) {
        listeners.add(l);
    }

    public void display() {
        GLProfile profile = GLProfile.getDefault();
        final GLCapabilities caps = new GLCapabilities(profile);
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
                for (TerminationListener l : listeners) {
                    l.terminated();
                }
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
        animator.setUpdateFPSFrames(20, null);
    }

    public void terminate() {
        try {
            modifier.interrupt();
            modifier.join();
        } catch (InterruptedException ie) {
            // expected
        }

        animator.stop();
        glWindow.destroy();
    }

    private void updateNodes() {
        Map<String, ClassNode> nodeMap = classNodes.get();

        repelNodes(nodeMap);
        attractNodes(nodeMap);
    }

    private static void repelNodes(Map<String, ClassNode> nodeMap) {
        ClassNode[] nodes = nodeMap.values().toArray(new ClassNode[nodeMap.size()]);
        for (int i = 0; i < (nodes.length - 1); ++i) {
            ClassNode node1 = nodes[i];
            for (int j = i + 1; j < nodes.length; ++j) {
                ClassNode node2 = nodes[j];

                if (node1.getRelationships().containsKey(node2.getFQCN()) || node2.getRelationships().containsKey(node1.getFQCN())) {
                    if (isCloseTo(node1, node2, SHORT_REPEL_DISTANCE_SQUARED)) {
                        repel(node1, node2, SHORT_REPEL_DISTANCE / 3);
                    }
                } else {
                    float distance = getDistanceSquared(node1, node2);
                    if (distance < LONG_REPEL_DISTANCE_SQUARED) {
                        float repelDistance = (float) (Math.sqrt(LONG_REPEL_DISTANCE_SQUARED - distance) / 3);
                        repel(node1, node2, repelDistance);
                    }
                }
            }
        }
    }

    private void attractNodes(Map<String, ClassNode> nodeMap) {
        for (ClassNode node : nodeMap.values()) {
            for (String attractorName : node.getRelationships().keySet()) {
                ClassNode attractorNode = nodeMap.get(attractorName);

                if (attractorNode != null) {
                    if (isFarAwayFrom(node, attractorNode)) {
                        attract(node, attractorNode);
                    }
                }
            }
        }
    }

    private static boolean isCloseTo(ClassNode node1, ClassNode node2, float distance) {
        float distanceSq = getDistanceSquared(node1, node2);
        return distanceSq < distance;
    }

    private static float getDistanceSquared(ClassNode node1, ClassNode node2) {
        float[] pos1 = node1.getPosition();
        float[] pos2 = node2.getPosition();

        return distanceSquared(pos1, pos2);
    }

    private static boolean isFarAwayFrom(ClassNode node1, ClassNode node2) {
        float[] pos1 = node1.getPosition();
        float[] pos2 = node2.getPosition();

        float distanceSq = distanceSquared(pos1, pos2);
        return distanceSq > ATTRACTION_DISTANCE_SQUARED;
    }

    private static void repel(ClassNode node1, ClassNode node2, float repelSpeed) {
        float[] pos1 = node1.getPosition();
        float[] pos2 = node2.getPosition();

        float[] uv = unitVector(pos1, pos2);

        for (int i = 0; i < 3; ++i) {
            pos1[i] += -repelSpeed * uv[i];
            pos2[i] += +repelSpeed * uv[i];
        }
    }

    private void attract(ClassNode node1, ClassNode node2) {

        float attraction = classNodes.getAttractionBetween(node1, node2);
        if (attraction != 0.0f) {

            float[] pos1 = node1.getPosition();
            float[] pos2 = node2.getPosition();

            float[] uv = unitVector(pos1, pos2);
            float multipler = attraction;

            for (int i = 0; i < 3; ++i) {
                pos1[i] += multipler * uv[i];
                pos2[i] += -multipler * uv[i];
            }
        }
    }

    private static float distanceSquared(float[] pos1, float[] pos2) {
        float x = pos1[0] - pos2[0];
        float y = pos1[1] - pos2[1];
        float z = pos1[2] - pos2[2];

        return (x * x) + (y * y) + (z * z);
    }

    private static float[] unitVector(float[] pos1, float[] pos2) {
        float[] uv = { pos2[0] - pos1[0], pos2[1] - pos1[1], pos2[2] - pos1[2] };
        float denom = (float) Math.sqrt((uv[0] * uv[0]) + (uv[1] * uv[1]) + (uv[2] * uv[2]));

        if (denom == 0.0f) {
            uv[0] = (float) ((Math.random() * 10.0) - 5.0);
            uv[1] = (float) ((Math.random() * 10.0) - 5.0);
            uv[2] = (float) ((Math.random() * 10.0) - 5.0);
            denom = (float) Math.sqrt((uv[0] * uv[0]) + (uv[1] * uv[1]) + (uv[2] * uv[2]));
        }

        uv[0] /= denom;
        uv[1] /= denom;
        uv[2] /= denom;

        return uv;
    }

    private static void centerWindow(GLWindow window) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle screenBounds = gc.getBounds();

        int w = window.getWidth();
        int h = window.getHeight();

        window.setPosition((screenBounds.width - w) / 2, (screenBounds.height - h) / 3);
    }

    class GDEvents implements GLEventListener {

        private GLU glu;
        private int sphereList;

        @Override
        public void display(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            for (ClassNode node : classNodes) {

                float[] color = node.getColor();
                gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT, color, 0);
                gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR, color, 0);
                gl.glMaterialf(GL.GL_FRONT, GLLightingFunc.GL_SHININESS, 0.5f);
                gl.glMaterialf(GL.GL_FRONT, GLLightingFunc.GL_EMISSION, node.getType() == ClassType.OBJECT_CLASS ? 1.0f : 0.0f);

                float[] pos = node.getPosition();
                try {
                    textRenderer.begin3DRendering();
                    textRenderer.setColor(1.0f, 0.5f, 0.2f, 1.0f);

                    textRenderer.draw3D(node.getFQCN(), pos[0] + 20.0f, pos[1], pos[2], 1.0f);
                } finally {
                    textRenderer.end3DRendering();
                }

                gl.glColor3f(color[0], color[1], color[2]);
                gl.glPushMatrix();
                try {
                    gl.glTranslatef(pos[0], pos[1], pos[2]);
                    gl.glCallList(sphereList);

                } finally {
                    gl.glPopMatrix();
                }
            }
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
            // do nothing
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            glu = new GLU();

            GL2 gl = drawable.getGL().getGL2();

            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glShadeModel(GLLightingFunc.GL_SMOOTH);

            gl.glEnable(GLLightingFunc.GL_LIGHTING);
            gl.glEnable(GLLightingFunc.GL_LIGHT0);

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

            Font f = new Font("SansSerif", Font.BOLD, FONT_SIZE);
            textRenderer = new TextRenderer(f);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2 gl = drawable.getGL().getGL2();
            gl.glViewport(0, 0, width, height);

            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();

            float widthHeightRatio = (float) width / (float) height;
            glu.gluPerspective(45, widthHeightRatio, 1, 1000);
            glu.gluLookAt(eyeLocation[0], eyeLocation[1], eyeLocation[2], 0, 0, 0, 0, 1, 0);

            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
        }
    }

    class GDKeyListener extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {

            GL2 gl = glWindow.getGL().getGL2();
            gl.getContext().makeCurrent();

            try {

                gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
                gl.glLoadIdentity();

                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_UP) {
                    float[] uv = unitVector(ORIGIN, eyeLocation);
                    for (int i = 0; i < 3; ++i) {
                        uv[i] *= STEP_SIZE;
                        eyeLocation[i] -= uv[i];
                    }
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    float[] uv = unitVector(ORIGIN, eyeLocation);
                    for (int i = 0; i < 3; ++i) {
                        uv[i] *= STEP_SIZE;
                        eyeLocation[i] += uv[i];
                    }
                } else if (keyCode == KeyEvent.VK_LEFT) {
                    float x = (float) ((Math.cos(ROTATION_SIZE) * eyeLocation[0]) - (Math.sin(ROTATION_SIZE) * eyeLocation[2]));
                    float z = (float) ((Math.sin(ROTATION_SIZE) * eyeLocation[0]) + (Math.cos(ROTATION_SIZE) * eyeLocation[2]));
                    eyeLocation[0] = x;
                    eyeLocation[2] = z;
                } else if (keyCode == KeyEvent.VK_RIGHT) {
                    float x = (float) ((Math.cos(-ROTATION_SIZE) * eyeLocation[0]) - (Math.sin(-ROTATION_SIZE) * eyeLocation[2]));
                    float z = (float) ((Math.sin(-ROTATION_SIZE) * eyeLocation[0]) + (Math.cos(-ROTATION_SIZE) * eyeLocation[2]));
                    eyeLocation[0] = x;
                    eyeLocation[2] = z;
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
    }

    class Modifier implements Runnable {
        @Override
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
