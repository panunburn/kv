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
     * @note the number returned is always larger than the predecessors.
     * @throws RemoteException
     */
    long next() throws RemoteException;   
}
