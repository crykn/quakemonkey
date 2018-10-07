## This fork

This fork builds on top of the one by [Namek](https://github.com/Namek/quakemonkey) and tries to bring the KryoNet support up to date.

## What is quakemonkey?

Quakemonkey is an implementation of the snapshot protocol used in the successful Quake 3 engine, built on top of the existing jMonkeyEngine networking framework. In short, this UDP-only protocol sends partial messages to each client in order to save bandwidth. These partials are created from the difference at byte level of the current message and the message that was last received by the client. For a more detailed overview, see [here](http://fabiensanglard.net/quake3/network.php).

Quakemonkey makes your life easier, because you don't have to figure out for yourself which values in your gamestate have changed and create partial messages for each to save bandwidth. You can just send a big gamestate message, and quakemonkey will take care of the rest.

## Features

Because the snapshot mechanism is implemented at byte level - i.e. when the messages are serialized to a byte buffer - no modifications to your messages are required to use quakemonkey in your existing code. It just works! 

**Summary:**

* UDP-only: fast connections
* Delta messages: smaller packages
* Byte-level deltas: invisible to user
* No need to split up master gamestate
* Built on top of existing framework
* Easy way to check if client is lagging

## Example code

The following shows how to use quakemonkey in your code. For a full example, see the _example_ directory in the git repository.

[WIP]

### Server code:

```java
DiffClassRegistration.registerClasses();
myServer = Network.createServer(6143);
diffHandler = new ServerDiffHandler<GameStateMessage>(myServer);
myServer.start();
diffHandler.dispatchMessage(myServer, Filters.in(myServer.getConnections()), newMessage);
```

This should look like the code you already have. The only thing that changes is that a `ServerDiffHandler` is created and that the message you want to follow the quake protocol is broadcast by `diffHandler.dispatchMessage`.

### Client code:

```java
bc. diffHandler = new ClientDiffHandler<>(myClient, GameStateMessage.class);
diffHandler.addListener(this); // register listener for GameStateMessage
```

The client code is even easier: instead of registering a listener for the class `GameStateMessage` in the client class directly, this is done through the `ClientDiffHandler`.