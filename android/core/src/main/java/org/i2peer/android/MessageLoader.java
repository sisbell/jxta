/**
 * Copyright (C) 2012 i2peer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.i2peer.android;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class MessageLoader<Params> {
	
	protected static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(5, 64, 1000, 
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	protected OnMessageLoadedListener listener;

	public abstract Message load(Params param) throws IOException;

	public void registerListener(OnMessageLoadedListener listener) {
		this.listener = listener;
	}
	
	public void unregisterListener(OnMessageLoadedListener listener) {
		listener = null;
	}
	
	public void execute(final Params params) throws IOException {
		FutureTask<Boolean> future = new FutureTask<Boolean>(
				new Callable<Boolean>() {
					public Boolean call() {
						Message m;
						try {
							m = load(params);
						} catch (IOException e) {
							return false;
						}
						if(listener != null) {
							listener.onMessageReceive(m);
						}
						return m != null;
					}
				});
		
		EXECUTOR.execute(future);
		
		try {
			future.get(8000, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new IOException(e);
		} 		
	}
}
