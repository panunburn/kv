ALL: build

.PHONY: build clean client id server

JAR = kv.jar
BIN = bin

SHARED = common protocol
EXEC = client id server
SHARED_FILES = $(addsuffix /*.java,$(SHARED))

$(BIN):
	mkdir $(BIN)

client: $(BIN) $(SHARED_FILES) client/*.java
	javac -d $^

server: $(BIN) $(SHARED_FILES) server/*.java
	javac -d $^

id: $(BIN) $(SHARED_FILES) id/*.java
	javac -d $^

build: $(BIN) $(EXEC)
	jar -cvf $(JAR) $(addprefix -C $(BIN) ,$(SHARED) $(EXEC))

clean:
	rm -rf $(BIN)
	rm -rf $(JAR)
