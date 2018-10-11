package net.namekdev.quakemonkey.messages;

import net.namekdev.quakemonkey.utils.pool.Pool;

/**
 * A package containing a payload message with an identifier.
 * 
 * @author Ben Ruijl
 */
public class QuakeMonkeyPackage {
	public static final Pool<QuakeMonkeyPackage> POOL = new Pool<QuakeMonkeyPackage>(
			new Pool.ObjectSupplier<QuakeMonkeyPackage>() {
				@Override
				public QuakeMonkeyPackage newInstance() {
					return new QuakeMonkeyPackage();
				}

				@Override
				public void onFree(QuakeMonkeyPackage obj) {
					obj.currentId = 0;
					obj.message = null;
				}
			});

	private short currentId;
	private Object message;

	public QuakeMonkeyPackage() {
		// default public constructor
	}

	public short getLabel() {
		return currentId;
	}

	public Object getPayloadMessage() {
		return message;
	}

	/**
	 * Method existing only for pooling.
	 */
	public QuakeMonkeyPackage set(short label, Object message) {
		this.currentId = label;
		this.message = message;

		return this;
	}

	@Override
	public String toString() {
		return "LabeledMessage { label: " + currentId + ", payloadMessage: "
				+ message + "}";
	}
}
