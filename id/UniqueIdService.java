package id;

import java.rmi.*;

/**
 * Unique id generation.
 *
 */
public interface UniqueIdService extends Remote
{
    /**
     * @return the next available id.
     * @throws RemoteException
     */
    long next() throws RemoteException;   
}
