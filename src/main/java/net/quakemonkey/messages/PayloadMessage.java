package net.quakemonkey.messages;

import net.quakemonkey.utils.pool.Pool;

/**
 * A message containing a payload with an identifier.
 * 
 * @author Ben Ruijl
 */
public final class PayloadMessage {
	public static final Pool<PayloadMessage> POOL = new Pool<PayloadMessage>(
			new Pool.ObjectSupplier<PayloadMessage>() {
				@Override
				public PayloadMessage newInstance() {
					return new PayloadMessage();
				}

				@Override
				public void onFree(PayloadMessage obj) {
					obj.currentId = 0;
					obj.message = null;
				}
			});

	private short currentId;
	private Object message;

	public PayloadMessage() {
		// default public constructor
	}

	/**
	 * @return the identifier of this package.
	 */
	public short getId() {
		return currentId;
	}

	/**
	 * @return the payload message if this package.
	 */
	public Object getPayloadMessage() {
		return message;
	}

	/**
	 * Sets the properties of this message.
	 * <p>
	 * Utility method for the {@linkplain #POOL pool}.
	 * 
	 * @param label
	 * @param message
	 * @return
	 */
	public PayloadMessage set(short label, Object message) {
		this.currentId = label;
		this.message = message;

		return this;
	}

	@Override
	public String toString() {
		return "PayloadMessage { id: " + currentId + ", payload: "
				+ message + "}";
	}
}
