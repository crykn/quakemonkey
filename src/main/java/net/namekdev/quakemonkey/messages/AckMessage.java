package net.namekdev.quakemonkey.messages;

import net.namekdev.quakemonkey.utils.pool.Pool;

/**
 * An acknowledgment message that is sent from the client to the server. It
 * contains an {@linkplain #id identifier} of the message that was received.
 * 
 * @author Ben Ruijl
 * 
 */
public class AckMessage {
	public static final Pool<AckMessage> POOL = new Pool<AckMessage>(
			new Pool.ObjectSupplier<AckMessage>() {
				@Override
				public AckMessage newInstance() {
					return new AckMessage();
				}

				@Override
				public void onFree(AckMessage obj) {
					obj.id = 0;
				}
			});

	private short id;

	public AckMessage() {
		// default public constructor
	}

	public short getId() {
		return id;
	}

	public AckMessage set(short id) {
		this.id = id;

		return this;
	}

	@Override
	public String toString() {
		return "AckMessage { id: " + id + "}";
	}
}
