classgraph
==========

a visualization of class dependencies


This is a JOGL application that shows a 3D visualization of dependencies between classes.
It shows classes as orbs of different colors:

    Green - classes found in command line jars
    Red   - classes on the default classpath
    White - java.lang.Object


Classes with stronger relationships will cluster together.

Use the arrow keys to move around in the scene.

    LEFT/RIGHT to spin around the origin
    UP/DOWN to zoom in or zoom out

To run on your code, do

    java -jar classgraph-0.1.0.jar jar1.jar jar2.jar jar3.jar
    

