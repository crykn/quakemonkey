## What is quakemonkey?

[![Release](https://jitpack.io/v/crykn/quakemonkey.svg)](https://jitpack.io/#crykn/quakemonkey) [![Build Status](https://travis-ci.com/crykn/quakemonkey.svg?branch=master)](https://travis-ci.com/crykn/quakemonkey) [![Code Coverage](https://codecov.io/gh/crykn/quakemonkey/branch/master/graph/badge.svg)](https://codecov.io/gh/crykn/quakemonkey)

Quakemonkey is an implementation of the **delta snapshot protocol** used in the successful Quake 3 engine in java. It was originally built on top of the existing jMonkeyEngine networking framework, but this fork ports it to the popular [KryoNet](https://github.com/EsotericSoftware/kryonet)-Framework. In short, this UDP-only protocol sends partial messages to each client in order to save bandwidth. These partials are created from the difference at byte level of the current message and the message that was last received by the client. For a more detailed overview, see [here](http://fabiensanglard.net/quake3/network.php).

Quakemonkey makes your life easier, because you don't have to figure out for yourself which values in your gamestate have changed and create partial messages for each to save bandwidth. You can just send a big gamestate message, and quakemonkey will take care of the rest.

## Features

Quakemonkey works by looking at the bytes of a serialized gamestate message: These are looked at in blocks of four. If one of those blocks is the same as in the previous message it is not sent again. To indicate this to the client the message includes one bit for every block, that denotes whether this section was sent again or the previously received data can be used. For more information about snapshot compression take a look at [this post](https://gafferongames.com/post/snapshot_compression/).

Because the snapshot mechanism is implemented at byte level - i.e. when the messages are serialized to a byte buffer - no modifications to your messages are required to use quakemonkey in your existing code. It just works! 

**Summary:**

* UDP-only: fast connections & low latency
* Delta messages: smaller packages (just do the maths: 4 unchanged bytes -> 1 bit)
* Byte-level deltas: invisible to the user and therefore no need to split up the master gamestate yourself
* Built on top of an existing, well tested networking framework
* Easy way to check if a client is lagging

## Example code

The following example shows how to use quakemonkey in your code. For a full example, see the [ServerTest](https://github.com/crykn/quakemonkey/blob/master/src/test/java/net/namekdev/quakemonkey/ServerTest.java) in the _src/test/java/_ directory.

### Server:

```java
// Create the server
Server server = new Server();
server.start();
DiffClassRegistration.registerClasses(server.getKryo()); // Register the relevant classes to the Kryo serializer

// Register the diff handler
ServerDiffHandler<GameStateMessage> diffHandler = new ServerDiffHandler<>(server);

server.bind(54555, 54777); // Start the server

// Send a message to the clients
// this message is reduced in size by using byte level deltas
diffHandler.dispatchMessageToAll(messageToSend);
```

This should look mostly like the code you already have. The only thing that changes is that a `ServerDiffHandler` is created and that the message you want to follow the quake protocol is broadcast by `diffHandler.dispatchMessageToAll(msg)` and not via the server directly.

### Client:

```java
Client client = new Client();
client.start();
DiffClassRegistration.registerClasses(client.getKryo()); 

// Register the diff handler & listeners
ClientDiffHandler<GameStateMessage> diffHandler = new ClientDiffHandler<>(client, GameStateMessage.class); 
diffHandler.addListener( // register a listener for the GameStateMessage
	(con, msg) -> {
      // do anything with the received message
	});
		
client.connect(5000, "192.168.0.4", 54555, 54777); // Start the client
```

The client code is even easier: instead of registering a listener for the class `GameStateMessage` in the client directly, a `ClientDiffHandler` is instantiated. The diff handler takes care of receiving the messages and distributes them to its own listeners.