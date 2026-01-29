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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphBuilder.class);

    private static final FileFilter CLASS_FILTER = new FileFilter() {
        @Override
        public boolean accept(File path) {
            if (path.isDirectory()) {
                return true;
            }

            return path.getPath().endsWith(".class");
        }
    };

    private ExecutorService executor;
    private Set<File> classPath;
    private ClassNodes nodes;

    public GraphBuilder(final Set<File> clsPath) {

        try {
            ClassFinderLoader loader = new ClassFinderLoader(clsPath);
            nodes = new ClassNodes(loader);

            executor = Executors.newFixedThreadPool(3 * Runtime.getRuntime().availableProcessors());
            classPath = clsPath;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL while loading classes", e);
        }
    }

    public void build() {
        for (final File f : classPath) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    if (f.isFile()) {
                        parseJar(f);
                    } else {
                        parseDirectory(f);
                    }
                }
            });
        }
    }

    public void terminate() {
        executor.shutdownNow();
    }

    public ClassNodes getNodes() {
        return nodes;
    }

    private void parseJar(File f) {
        try {
            ZipEntry ze = null;
            try (JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                ClassGraphBuildingVisitor visitor = new ClassGraphBuildingVisitor(nodes);

                while ((ze = jis.getNextEntry()) != null) {
                    final String clsName = ze.getName();
                    if (clsName.endsWith(".class")) {
                        LOGGER.info("Parsing class {}", clsName);

                        Thread.sleep(100);
                        ClassReader cr = new ClassReader(jis);
                        cr.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed parsing zip entry {} from file {}", ze, f, e);
            }
        } catch (InterruptedException ie) {
            LOGGER.info("Jar {} Parsing interrupted", f);
        }
    }

    private void parseDirectory(File d) {
        ClassGraphBuildingVisitor visitor = new ClassGraphBuildingVisitor(nodes);
        List<File> stack = new ArrayList<>();
        stack.add(d);

        while (!stack.isEmpty()) {
            File f = stack.remove(stack.size() - 1);
            if (f.isDirectory()) {
                File[] children = f.listFiles(CLASS_FILTER);
                if (children != null) {
                    for (File c : children) {
                        stack.add(c);
                    }
                }
            } else {
                try (final InputStream is = new BufferedInputStream(new FileInputStream(f))) {
                    ClassReader cr = new ClassReader(is);
                    cr.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (IOException e) {
                    LOGGER.error("Failed parsing class file {}", f, e);
                }
            }
        }
    }

    private static class ClassFinderLoader extends URLClassLoader implements ClassFinder {

        public ClassFinderLoader(Set<File> clsPath) throws MalformedURLException {
            super(toURLs(clsPath));
        }

        private static URL[] toURLs(Set<File> clsPath) throws MalformedURLException {
            List<URL> urls = new ArrayList<>(clsPath.size());

            for (File path : clsPath) {
                urls.add(path.toURI().toURL());
            }

            return urls.toArray(new URL[urls.size()]);
        }

        @Override
        public ClassType classStatus(String clsName) {
            if (clsName.startsWith("java.") || clsName.startsWith("javax.")) {
                return (clsName.equals("java.lang.Object")) ? ClassType.OBJECT_CLASS : ClassType.SYSTEM_CLASS;
            }

            String clsResourceName = clsName.replaceAll("\\.", "/") + ".class";

            URL u = getResource(clsResourceName);
            if (u != null) {
                return ClassType.APPLICATION_CLASS;
            }

            return ClassType.UNKNOWN_CLASS;
        }
    }
}
