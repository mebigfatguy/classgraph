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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class LengthLimitedInputStream extends InputStream {

	private InputStream parentStream;
	private long remainingLength;
	
	public LengthLimitedInputStream(InputStream parent, long length) {
		parentStream = parent;
		remainingLength = length;
	}
	
	@Override
	public int read() throws IOException {
		if (remainingLength == 0) {
			return -1;
		}
		int data = parentStream.read();
		--remainingLength;
		return data;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (len > remainingLength) {
			len = (int) remainingLength;
		}
		
		if (len <= 0) {
			throw new EOFException("Read past the end of the stream");
		}
		
		int actLen = parentStream.read(b, off, len);
		remainingLength -= actLen;
		return actLen;
	}

	@Override
	public long skip(long n) throws IOException {
		if (remainingLength == 0) {
			throw new EOFException("Read past the end of the stream");
		}
		
		if (n > remainingLength) {
			remainingLength = 0;
			return remainingLength;
		}
		
		remainingLength -= n;
		return n;

	}

	@Override
	public int available() throws IOException {
		if (remainingLength > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		
		return (int) remainingLength;
	}

	@Override
	public void close() throws IOException {
		remainingLength = 0;
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException("Mark not supported by LengthLimitedInputStream");
	}
}
