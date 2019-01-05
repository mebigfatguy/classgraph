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

public enum RelationshipType {
	INHERITANCE(4.0f), INTERFACE(3.0f), INNER(2.0f), CONTAINMENT(1.0f), UNKNOWN(0.0f);

	private float relationshipWeight;

	private RelationshipType(float weight) {
		relationshipWeight = weight;
	}

	public float getWeight() {
		return relationshipWeight;
	}

}
