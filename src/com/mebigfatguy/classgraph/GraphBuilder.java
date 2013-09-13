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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
	
	private ExecutorService executor;
	private Set<File> classPath;
	private ClassNodes nodes;
	
	public GraphBuilder(Set<File> clsPath) throws MalformedURLException {
        
	    nodes = new ClassNodes(new ClassFinderLoader(clsPath));
	    
	    executor = Executors.newFixedThreadPool(3 * Runtime.getRuntime().availableProcessors());
		classPath = clsPath;
	}
	
	public void build() {
		for (final File f : classPath) {
		    executor.submit(new Runnable() {
		        public void run() {
		            parseJar(f);
		        }
		    });
		}
	}
	
	public ClassNodes getNodes() {
	    return nodes;
	}
	
	private void parseJar(File f) {
		ZipEntry ze = null;
		try (JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(f)))) {
			while ((ze = jis.getNextEntry()) != null) {
			    final String clsName = ze.getName();
				if (clsName.endsWith(".class")) {
					try (final InputStream is = new LengthLimitedInputStream(jis, ze.getSize())) {
                        ClassReader cr = new ClassReader(is);
                        cr.accept(new ClassGraphBuildingVisitor(nodes), ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed parsing zip entry {} from file {}", ze, f, e);
		}
	}
	
	private static class ClassFinderLoader extends URLClassLoader implements ClassFinder {
	    
	    public ClassFinderLoader(Set<File> clsPath) throws MalformedURLException {
	        super(toURLs(clsPath));
	    }
	    
	    private static URL[] toURLs(Set<File> clsPath) throws MalformedURLException {
	        List<URL> urls = new ArrayList<URL>(clsPath.size());
	        
	        for (File path : clsPath) {
	            urls.add(path.toURI().toURL());
	        }
	        
	        return urls.toArray(new URL[urls.size()]);
	    }

        @Override
        public ClassType classStatus(String clsName) {
            if (clsName.startsWith("java.") || clsName.startsWith("javax.")) {
                return ClassType.SYSTEM_CLASS;
            }
            
            String clsResourceName = clsName.replaceAll("\\.",  "/") + ".class";
            URL u = Thread.currentThread().getContextClassLoader().getResource(clsResourceName);
            if (u != null) {
                return ClassType.SYSTEM_CLASS;
            }
            
            u = getResource(clsResourceName);
            if (u != null) {
                return ClassType.PRIMARY_CLASS;
            }
            
            return ClassType.UNKNOWN_CLASS;
        }
	}
}
