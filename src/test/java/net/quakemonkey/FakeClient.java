package net.quakemonkey;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;

public class FakeClient extends Client {
	@Override
	public int sendUDP(Object object) {
		// do nothing for a testing purposes.
		return 0;
	}

	@Override
	public void addListener(Listener listener) {
	}
}
