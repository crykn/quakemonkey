package net.quakemonkey;

import static org.junit.Assert.fail;

import java.util.function.BiConsumer;

import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

import net.quakemonkey.ClientDiffHandler;
import net.quakemonkey.DiffConnectionHandler;
import net.quakemonkey.ServerDiffHandler;
import net.quakemonkey.messages.PayloadMessage;

public class DiffHandlerTests {

	@Test(expected = IllegalArgumentException.class)
	public void testConstructors1() {
		new ClientDiffHandler<String>(new Client(), String.class, (short) 3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructors2() {
		new DiffConnectionHandler<>(new Kryo(), (short) 3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructors3() {
		new ServerDiffHandler<String>(new Server(), (short) 3);
	}

	@Test
	public void testListeners() {
		ClientDiffHandler<String> diff = new ClientDiffHandler<>(new Client(),
				String.class, (short) 4);
		BiConsumer<Connection, String> b = new BiConsumer<Connection, String>() {
			@Override
			public void accept(Connection t, String u) {
				fail();
			}
		};

		diff.addListener(b);
		diff.removeListener(b);

		PayloadMessage q = new PayloadMessage();
		q.set((short) 0, "testString");

		diff.processPackage(new FakeClient(), q);
	}

}
