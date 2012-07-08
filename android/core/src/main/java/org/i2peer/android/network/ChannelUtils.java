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
package org.i2peer.android.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChannelUtils {
	
	public static void blockUntilConnected(final SocketChannel channel, long timeout) throws IOException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, timeout, 
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		FutureTask<Boolean> future = new FutureTask<Boolean>(
				new Callable<Boolean>() {
					public Boolean call() {
						while(!channel.isConnected()) {
							try {
								Thread.sleep(300);
							} catch (InterruptedException e) {
							}
						}
						return true;
					}
				});
	    executor.execute(future);
		
		try {
			future.get(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			channel.close();
			throw new IOException(e);
		} 
	}
}
