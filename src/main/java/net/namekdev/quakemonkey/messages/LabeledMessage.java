package net.namekdev.quakemonkey.messages;

import net.namekdev.quakemonkey.utils.pool.Pool;

/**
 * A message containing another message with an identifier.
 * 
 * @author Ben Ruijl
 */
public class LabeledMessage {
	public static final Pool<LabeledMessage> POOL = new Pool<LabeledMessage>(
			new Pool.ObjectSupplier<LabeledMessage>() {
				@Override
				public LabeledMessage get() {
					return new LabeledMessage();
				}

				@Override
				public void onFree(LabeledMessage obj) {
					obj.label = 0;
					obj.message = null;
				}
			});

	private short label;
	private Object message;

	public LabeledMessage() {
		// default public constructor
	}

	public short getLabel() {
		return label;
	}

	public Object getPayloadMessage() {
		return message;
	}

	/**
	 * Method existing only for pooling.
	 */
	public LabeledMessage set(short label, Object message) {
		this.label = label;
		this.message = message;

		return this;
	}

	@Override
	public String toString() {
		return "LabeledMessage { label: " + label + ", payloadMessage: "
				+ message + "}";
	}
}
