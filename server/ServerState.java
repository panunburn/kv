package server;

import java.io.Serializable;
import java.util.*;
import common.*;
import protocol.Request;

/**
 * The server states consists of the key value store, the endpoints of all
 * other available replicated servers, and the Paxos states.
 */
public class ServerState implements Serializable
{
    private static final long serialVersionUID = 1L;

    KVStore store;
    HashMap<EndPoint, ReplicaService> replicas;
    PaxosState<Request> paxos;

    public ServerState()
    {
        this.store = new KVStore(Config.defaultKVStorePath());
        this.replicas = new HashMap<>();
        this.paxos = new PaxosState<>();
    }
    
    public ServerState(KVStore store, 
                       HashMap<EndPoint, ReplicaService> replicas,
                       PaxosState<Request> paxos)
    {
        this.store = store;
        this.replicas = replicas;
        this.paxos = paxos;
    }

    @Override
    public String toString()
    {
        return "Current server state:\n" 
             + store + "\n" 
             + "Available replicated servers: " + replicas.keySet() + "\n"
             + paxos;
    }
}
