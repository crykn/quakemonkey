package net.namekdev.quakemonkey.messages;

import java.util.Arrays;

import net.namekdev.quakemonkey.utils.pool.Pool;

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
				public DiffMessage newInstance() {
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

	public short getMessageId() {
		return messageId;
	}

	public int[] getData() {
		return data;
	}

	public byte[] getFlags() {
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

	@Override
	public String toString() {
		return "DiffMessage { id: " + messageId + ", flags: "
				+ Arrays.toString(flags) + ", data: " + Arrays.toString(data)
				+ "}";
	}
}
