## What is quakemonkey?

[![Release](https://jitpack.io/v/crykn/quakemonkey.svg)](https://jitpack.io/#crykn/quakemonkey) 

Quakemonkey is an implementation of the snapshot protocol used in the successful Quake 3 engine in java. It was originally built on top of the existing jMonkeyEngine networking framework, but this fork ports it to the popular [KryoNet](https://github.com/EsotericSoftware/kryonet)-Framework. In short, this UDP-only protocol sends partial messages to each client in order to save bandwidth. These partials are created from the difference at byte level of the current message and the message that was last received by the client. For a more detailed overview, see [here](http://fabiensanglard.net/quake3/network.php).

Quakemonkey makes your life easier, because you don't have to figure out for yourself which values in your gamestate have changed and create partial messages for each to save bandwidth. You can just send a big gamestate message, and quakemonkey will take care of the rest.

## Features

Because the snapshot mechanism is implemented at byte level - i.e. when the messages are serialized to a byte buffer - no modifications to your messages are required to use quakemonkey in your existing code. It just works! 

**Summary:**

* UDP-only: fast connections & low latency
* Delta messages: smaller packages
* Byte-level deltas: invisible to the user and therefore no need to split up the master gamestate yourself
* Built on top of an existing, well tested framework
* Easy way to check if a client is lagging

## Example code

The following example shows how to use quakemonkey in your code. For a full example, see the _src/test/java/_ directory in the git repository.

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
	new BiConsumer<Connection, SerializationTests.GameStateMessage>() {
		@Override
		public void accept(Connection con, GameStateMessage msg) {
			// do anything with the message
		}
	});
		
client.connect(5000, "192.168.0.4", 54555, 54777); // Start the client
```

The client code is even easier: instead of registering a listener for the class `GameStateMessage` in the client directly, a `ClientDiffHandler` is instantiated. The diff handler takes care of receiving the messages and distributes them to its own listeners.