package server;

import java.rmi.RemoteException;

public class PaxosException extends RemoteException 
{
    private static final long serialVersionUID = 1L;

    public PaxosException(String msg)
    {
        super(msg);
    }
}
