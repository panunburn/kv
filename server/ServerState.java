package server;

import java.io.Serializable;
import java.util.*;

import common.EndPoint;

/**
 * The server states consists of the store and the endpoint locations of all other available replicated servers. 
 */
public class ServerState implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	KVStore store;
	Set<EndPoint> replicas;
	
	public ServerState(KVStore store, Set<EndPoint> replicas)
	{
		this.store = store;
		this.replicas = replicas;
	}
	
	@Override
	public String toString()
	{
		return "Current server state:\n" + 
			   "Store contains: " + store + "\n" +
			   "Available replicated servers: " + replicas;
	}
}

