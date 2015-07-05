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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassGraph {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassGraph.class);
	
	private static GraphBuilder builder;
	
	public static void main(String[] args) {
		
		if (args.length == 0) {
			LOGGER.error("No classpath roots specified, no imaging can occur");
			return;
		}
		
		Set<File> clsPath = new HashSet<>();
		
		for (String a : args) {
			File f = new File(a);
			if ((f.isFile() && f.getName().endsWith(".jar")) || f.isDirectory())
				clsPath.add(f);
			else {
				LOGGER.error("Failed loading jar file {}.. ignored", a);
				clsPath.clear();
				break;
			}
		}
		
		if (!clsPath.isEmpty()) {
			builder = new GraphBuilder(clsPath);
		    GraphDisplay gd = new GraphDisplay(builder.getNodes());
		    
		    gd.addTerminationListener(new TerminationListener() {
		    	@Override
		    	public void terminated() {
		    		builder.terminate();
		    		System.exit(0);
		    	}
		    });
		    
		    gd.display();
            builder.build();
		}
	}
}
