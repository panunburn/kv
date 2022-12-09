package server;

import java.rmi.*;
import protocol.*;
import transaction.TransactionId;

/**
 * Remote Store interface between the client and server.
 *
 */
public interface StoreService extends Remote
{
    /**
     * Process the request.
     * 
     * @param request the request to be processed
     * @param tid the transaction Id
     * @return a response depending on the request type.
     * @throws RemoteException
     */
    Response process(Request request, TransactionId tid) throws RemoteException;
}
