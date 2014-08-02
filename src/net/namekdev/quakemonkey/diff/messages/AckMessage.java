package net.namekdev.quakemonkey.diff.messages;

/**
 * An acknowledgment message that is sent from the client to the server. It
 * contains and identifier of the message that was received.
 * 
 * @author Ben Ruijl
 * 
 */
public class AckMessage {
	private short id;

	public AckMessage() {
	}

	public AckMessage(short id) {
		this.id = id;
	}

	public short getId() {
		return id;
	}
}
