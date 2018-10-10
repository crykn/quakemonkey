package net.namekdev.quakemonkey;

import com.esotericsoftware.kryo.Kryo;

import net.namekdev.quakemonkey.messages.AckMessage;
import net.namekdev.quakemonkey.messages.DiffMessage;
import net.namekdev.quakemonkey.messages.DiffMessageSerializer;
import net.namekdev.quakemonkey.messages.LabeledMessage;

/**
 * Registers messages in the serializer that are required for the snapshot
 * protocol, for both the server and the client.
 * 
 * @author Ben Ruijl
 * 
 */
public class DiffClassRegistration {

	private DiffClassRegistration() {
		// not used
	}

	/**
	 * Registers the messages that are required for the snapshot protocol, for
	 * both the server and the client. Make <b>absolutely</b> sure that this
	 * function is called before creation of the server and the client and that
	 * the position in the code relative to other {@link Kryo#register(Class)}
	 * calls is the same.
	 * 
	 * @param kryoSerializer
	 *            the serializer used by the endpoint.
	 */
	public static void registerClasses(Kryo kryoSerializer) {
		kryoSerializer.register(DiffMessage.class, new DiffMessageSerializer());
		kryoSerializer.register(AckMessage.class);
		kryoSerializer.register(LabeledMessage.class);
	}
}
