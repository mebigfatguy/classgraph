package com.mebigfatguy.classgraph;

public enum ClassType {

    APPLICATION_CLASS(new float[] {0.0f, 1.0f, 0.0f}), 
    SYSTEM_CLASS(new float[] { 1.0f, 0.0f, 0.0f }), 
    OBJECT_CLASS(new float[] { 1.0f, 1.0f, 1.0f }), 
    UNKNOWN_CLASS(new float[] { 0.0f, 0.0f, 1.0f });

    float[] nodeColor;
    
    ClassType(float[] color) {
        nodeColor = color;
    }
    
    public float[] color() {
        return nodeColor;
    }
}
