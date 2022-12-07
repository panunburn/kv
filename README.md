# kv: A simple key value store

## Project Structure
* `client` contains the client implementation.
* `common` contains some common utilities.
* `protocol` contains the request types.
* `id` contains the global unique id service implementation. 
* `server` contains both the coordinator and replicated server implementation.

## Build
* `make` builds all `client`, `id`, and `server` packages under the `bin` directory and builds the `kv.jar` file. 
* `make clean` removes the `bin` directory and the `kv.jar` file.

## Usage
+  `java -cp kv.jar id.Server <port>?` starts the global unique id service on a specified port. 
	- `1099` will be used if not present.
+ `java -cp kv.jar server.Server coordinator <endpoint> <port>` starts the coordinator on the current host given an endpoint to the global id service and the port number. 
	- `<endpoint> := <ip | hostname> | <port> | <ip | hostname>:<port>`, where if either the `ip` or `hostname` is omitted, then `localhost` will be used and port number is default to be `1099`. 
+ `java -cp kv.jar server.Server replica <endpoint> <port>` starts the replicated server on the current host given the endpoint to the coordinator and the port number.
* `java -cp kv.jar client.Client <endpoint>?` starts the client given an optional endpoint to any server.  

## Quick Run
1. `java -cp kv.jar id.Server` starts the global unique id service with the default port `1099`.
2. `java -cp kv.jar server.Server coordinator 1099 1110` starts the coordinator on the current host given the unique id service endpoint `1099` and the port number, `1110`, it will use.
3. `java -cp kv.jar server.Server replica localhost:1110 1111` starts the replicated server on the current host with port `1111` given the coordinator's endpoint location, `localhost:1110`.
4. `java -cp kv.jar server.Server replica localhost:1110 1112` starts another replicated server on the current host with port `1112` given the coordinator's endpoint location, `localhost:1110`.
5. `java -cp kv.jar client.Client 1110` starts the client to connect to the coordinator.
6. `java -cp kv.jar client.Client localhost:1111` or `java client.Client 1112` starts the client connecting to the replicated server we have just started.

The `logs` folder contains an example run where we have 2 clients interacting with 5 servers, where the replicated servers can be added and removed (when terminated) dynamically. 

## Request Format
* `GET <symbol>`
* `DELETE <symbol>`
* `PUT <symbol> <symbol>`
* `PRINT`

`<symbol>` is a string with no blanks, such as space or newline characters. If spaces are needed, then this issue can be worked around by replacing spaces with other characters, such as ‘-’.

`PRINT` is added as a debugging request to dump out the states on each of the servers. 