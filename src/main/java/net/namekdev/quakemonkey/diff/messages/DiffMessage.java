package net.namekdev.quakemonkey.diff.messages;

import net.namekdev.quakemonkey.diff.utils.Pool;

/**
 * This message is used to send the byte-level difference of two messages to the
 * client.
 * 
 * @author Ben Ruijl
 */
public class DiffMessage {
	public static final Pool<DiffMessage> POOL = new Pool<DiffMessage>(
			new Pool.ObjectSupplier<DiffMessage>() {
				@Override
				public DiffMessage onCreate() {
					return new DiffMessage();
				}

				@Override
				public void onFree(DiffMessage obj) {
					obj.messageId = (byte) 0;
					obj.data = null;
					obj.flags = null;
				}
			});

	/**
	 * ID of the message the diff is from.
	 */
	private short messageId;
	private byte[] flags;
	private int[] data;

	public DiffMessage() {
		// default public constructor
	}

	public DiffMessage(short messageId, byte[] flag, int[] data) {
		this.messageId = messageId;
		this.data = data;
		this.flags = flag;
	}

	public short getMessageId() {
		return messageId;
	}

	public int[] getData() {
		return data;
	}

	public byte[] getFlag() {
		return flags;
	}

	/**
	 * Method existing only for pooling.
	 */
	public DiffMessage set(short id, byte[] flag, int[] data) {
		this.messageId = id;
		this.flags = flag;
		this.data = data;

		return this;
	}
}
