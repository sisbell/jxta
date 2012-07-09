package org.i2peer.android.jxta;

import org.i2peer.android.network.Messenger;
import org.i2peer.android.network.PingMessageLoader;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class JxtaModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PingMessageLoader.class).annotatedWith(Names.named("jxta")).to(
				JxtaPingMessageLoader.class);
		
		bind(Messenger.class).to(JxtaMessenger.class);
	}

}
