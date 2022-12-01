# Final Project

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