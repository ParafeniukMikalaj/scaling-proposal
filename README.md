# ZooKeeper-based scaling proposal
This repository contains proof of concept for distribution of kafka partitions through ZooKeeper coordination.

# How to launch
## Producer
Just launch ProducerLauncher main class. 
It will publish periodically to kafka with specified delay the accountId and the increasing value.

## Clients
Clients is an application which spawns and decommissions clients within account range with specified delays.
Every single client is instance of java.nio.SocketChannel.
Just launch ClientsLauncher main class.

## Server
Server is an instance of processing application with coordination through ZooKeeper. It will consume from the part of 
kafka partitions, will accept connections from clients and forward messages to online clients.
To start server launch ServerLauncherClass with following parameters:


    ServerLauncher <nodeId> <host> <port>

*   nodeId - unique numeric id among all instances of servers
*   host - advertised host. A host to which client should go to connect to this server
*   port - port to which server should bind
