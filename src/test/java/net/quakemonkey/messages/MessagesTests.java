package net.quakemonkey.messages;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MessagesTests {

	@Test
	public void testToString() {
		// AckMessage
		AckMessage ackMessage = new AckMessage();
		ackMessage.set((short) 3);
		assertEquals("AckMessage { id: 3}", ackMessage.toString());

		// test onFree
		AckMessage.POOL.free(ackMessage);
		assertEquals((short) 0, ackMessage.getId());

		// DiffMessage
		DiffMessage diffMessage = new DiffMessage();
		diffMessage.set((short) 5, new byte[] { (byte) 12 },
				new int[] { 3, 6, 88 });
		assertEquals("DiffMessage { id: 5, flags: [12], data: [3, 6, 88]}",
				diffMessage.toString());

		// LabeledMessage
		PayloadPackage labeledMessage = new PayloadPackage();
		labeledMessage.set((short) 23, "testabc");
		assertEquals("LabeledMessage { label: 23, payloadMessage: testabc}",
				labeledMessage.toString());
	}

}
