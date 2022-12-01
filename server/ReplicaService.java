package server;

import java.rmi.*;
import common.EndPoint;
import protocol.Request;

/**
 * The service interfaces between the coordinator and replicated servers. 
 *
 */
public interface ReplicaService extends Remote
{
	/**
	 * Capture the event whenever a new replicated server has been added.
	 * @param addr the endpoint location for the added server 
	 * @throws RemoteException
	 */
	void add(EndPoint addr) throws RemoteException;

	/**
	 * Capture the event whenever a new replicated server has been removed.
	 * @param addr the endpoint location for the removed server 
	 * @throws RemoteException
	 */
	void remove(EndPoint addr) throws RemoteException;	

	/**
	 * Shutdown the replicated server.
	 * @throws RemoteException
	 */
	void shutdown() throws RemoteException;	
	
	/**
	 * Validate the request to see if the write set of the request conflicts with the read set. 
	 * @param request the request to be validated.
	 * @return true if there is no conflict.
	 * @throws RemoteException
	 */
	boolean validate(Request request) throws RemoteException;
		
	/**
	 * Commit the request and update the store.
	 * @param request the request to be committed.
	 * @throws RemoteException
	 */
	void commit(Request request) throws RemoteException;
	
	/**
	 * Abort the request.
	 * @param request the request to be aborted.
	 * @throws RemoteException
	 */
	void abort(Request request) throws RemoteException;
}
