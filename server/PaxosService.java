package server;

import java.io.Serializable;
import java.rmi.*;

public interface PaxosService<V extends Serializable> extends Remote
{    
    Promise<V> prepare(int round, long id) throws RemoteException;

    V accept(int round, Proposal<V> proposal) throws RemoteException;  

    void learn(int round, V value) throws RemoteException;
}