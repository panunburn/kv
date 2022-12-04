ALL: build

.PHONY: clean client id server


SHARED = common/*.java protocol/*.java

BIN = bin

$(BIN):
	mkdir $(BIN)

client: $(BIN) $(SHARED) client/*.java
	javac -d $^

server: $(BIN) $(SHARED) server/*.java
	javac -d $^

id: $(BIN) $(SHARED) id/*.java
	javac -d $^

build: client id server

clean:
	rm -rf $(BIN)
