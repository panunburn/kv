# Project 3

## Project Structure
```bash
src
├── README.md
├── client
│   └── Client.java
├── common
│   ├── CmdLineParser.java
│   ├── CmdLineParserException.java
│   ├── Config.java
│   ├── EndPoint.java
│   ├── Logger.java
│   ├── NoThrow.java
│   └── Utils.java
├── protocol
│   ├── DeleteRequest.java
│   ├── GetRequest.java
│   ├── InvalidRequestException.java
│   ├── PrintRequest.java
│   ├── PutRequest.java
│   ├── Request.java
│   ├── RequestParser.java
│   └── RequestVisitor.java
└── server
    ├── CoordinatorService.java
    ├── KVStore.java
    ├── ParticipantService.java
    ├── Server.java
    ├── ServerState.java
    └── StoreService.java

4 directories, 23 files

```
* `client` contains the client implementation.
* `common` contains some common utilities.
* `protocol` contains the request types.
* `server` contains both the coordinator and replicated server implementation.

## Build
* Compile the client with `javac common/*.java protocol/*.java client/*.java`
* Compile the server with `javac common/*.java protocol/*.java server/*.java`

## Usage
+ `java server.Server <port>?` starts the coordinator on the current host with the port specified. 
	- Note that the server registry port number is optional. `1099` will be used if not present. 			
+ `java server.Server <endpoint> <port>` starts the replicated server on the current host with the port specified, where the first argument specifies the endpoint location for the coordinator.
	- `<endpoint> := <ip | hostname> | <port> | <ip | hostname>:<port>`, where if either the `ip` or `hostname` is omitted, then `localhost` will be used and port number is default to be `1099`. 
* `java client.Client <endpoint>?`
	- Note that the argument is optional with default value as discussed before. 

## Quick Run
* `java server.Server` starts the coordinator on the current host with port 1099.
* `java server.Server localhost:1099 1111` starts the replicated server on the current host with port 1111 given the coordinator's endpoint location, `localhost:1099`.
* `java client.Client` starts the client to connect to the coordinator.
* `java client.Client localhost:1111` or just `java client.Client 1111` starts the client connecting to the replicated server we have just started.

The `logs` folder contains an example run where we have 2 clients interacting with 5 servers, where the replicated servers can be added and removed (when terminated) dynamically. 

## Request Format
* `GET <symbol>`
* `DELETE <symbol>`
* `PUT <symbol> <symbol>`
* `PRINT`

`<symbol>` is a string with no blanks, such as space or newline characters. If spaces are needed, then this issue can be worked around by replacing spaces with other characters, such as ‘-’.

`PRINT` is added as a debugging request to dump out the states on each of the servers. 

## Executive Summary

### Assignment Overview

The purpose of this project is for us i) to apply replication management and distributed transactions concepts, specifically, the two-phase transaction protocol, learned in class to a relatively simple model, ii) to reason multithreaded program behaviors and thread safety in such systems, and ii) to further familiarize ourselves with the Java RMI interfaces and programming patterns. The scope is extended to support multithreaded replicated servers, maintaining consistency among them, and handling multiple concurrent client requests, which can be regarded as simple transactions. As with project 1, the clients can send three types of requests, `GET <key>`, `PUT <key> <value>`, and `DELETE <key>`, to interact with any of the replicated server stores. They should receive the response from the connected server in a successful run or log the warnings/errors when the request aborts or any communication-related exception occurs and continue with the subsequent request. The servers should run forever until terminated by users. Only the coordinator needs to save/recover the stores when it exits/restarts. The replicated servers should get the latest store from the coordinator when they start. Logging is performed on both the server and client sides with timestamps in milliseconds precision. Finally, the Java code should be well-factored and well-documented.

### Technical Impression

I started by first figuring out how to support dynamically adding/removing a server to/from the coordinator while maintaining a consistent server state across all the remaining replicas. The tricky part was during the replicated server initialization phase, where a proper synchronization mechanism has to be adopted to prevent the state from being corrupted. 

Next, I worked on broadcasting the requests. After any of the servers has received a `PUT/DELETE` request, it needs forward the request to the coordinator to be broadcasted to all the replicated servers (including itself). To ease the debugging effort, I added the `PRINT` request to print out the server state on each server. Later, I realized a request could be regarded as a trivial transaction and thus the system would form a simple distributed transaction system, thus the two-phase commit protocol should be applied. To implement the protocol, I had a question about when we should abort. After consulting with Ani, I realized I should keep track of the keys which are currently read. I also thought about and played around with the synchronization such that multiple `GET` requests can be processed concurrently. After manually adding some sleep calls to increase the read time, I observed that the server would truly abort and the two-phase commit protocol worked as expected.   

Then, to deal with coordinator failures, I believe the best way is to implement an election algorithm. But due to the time constraint, I designed the coordinator such that when it terminates, it will kill all the replicated servers to achieve consistency. A slightly better way is to let the coordinator pick an available replica as its successor. In terms of recovery, since our system mainly deals with simple in-memory data structures, I decided on allowing the replica to always start with the latest server state the coordinator is in when the replica starts. 

Besides election and recovery, two other enhancements can be implemented in the future. i) We can enhance the client to input a list of requests. By doing so, we can achieve a more sophisticated distributed transaction system. ii) On the client side, I only implemented the basic requirements as in project 2. But we can also allow the client to connect to other servers if the current server has gone down. 

