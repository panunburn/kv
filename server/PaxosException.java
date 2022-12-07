package server;

import java.rmi.RemoteException;

/**
 * The PAXOS exception for any unexpected states while running PAXOS.
 */
public class PaxosException extends RemoteException 
{
    private static final long serialVersionUID = 1L;

    public PaxosException(String msg)
    {
        super(msg);
    }
}
