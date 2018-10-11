package net.namekdev.quakemonkey.messages;

import net.namekdev.quakemonkey.utils.pool.Pool;

/**
 * A package containing a payload message with an identifier.
 * 
 * @author Ben Ruijl
 */
public class PayloadPackage {
	public static final Pool<PayloadPackage> POOL = new Pool<PayloadPackage>(
			new Pool.ObjectSupplier<PayloadPackage>() {
				@Override
				public PayloadPackage newInstance() {
					return new PayloadPackage();
				}

				@Override
				public void onFree(PayloadPackage obj) {
					obj.currentId = 0;
					obj.message = null;
				}
			});

	private short currentId;
	private Object message;

	public PayloadPackage() {
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
	 * Method existing only for pooling.
	 */
	public PayloadPackage set(short label, Object message) {
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
