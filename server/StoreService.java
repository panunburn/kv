package server;

import java.rmi.*;

import protocol.Request;

/**
 * Remote Store interface between the client and server.
 *
 */
public interface StoreService extends Remote
{
	/**
	 * Process the request. 
	 * @param request the request to be processed
	 * @return a nullable value depending on the request type.
	 * @throws RemoteException if the request aborted or any communication-related issues occurred. 
	 */
	String process(Request request) throws RemoteException;
}
