package server;

import java.io.Serializable;
import java.rmi.*;

/**
 * The PAXOS service type.
 * @param <V> the value type to agree on
 */
public interface PaxosService<V extends Serializable> extends Remote
{    
    /**
     * Check if the proposal id is larger than any proposal ids the acceptor has seen for a PAXOS round
     * @param round the PAXOS round
     * @param id a unique proposal id
     * @return a promise to not accept any proposals with ids less than the input id for the PAXOS round; null otherwise. 
     * @return null if the acceptor decides to fail.
     * @throws RemoteException
     */
    Promise<V> prepare(int round, long id) throws RemoteException;

    /**
     * Accept the proposal for a PAXOS round if the proposal id is the largest id the acceptor has responded a promise.
     * @param round the PAXOS round
     * @param proposal the proposal to be accepted.
     * @return the accepted value if the acceptor has accepted the proposal; null otherwise.
     * @return null if the acceptor decides to fail.
     * @throws PaxosException if the proposal hasn't been proposed and promised before. 
     * @throws RemoteException
     */
    V accept(int round, Proposal<V> proposal) throws RemoteException, PaxosException;  

    /**
     * Learn the value for a PAXOS round.
     * @param round the PAXOS round.
     * @param value an accepted value to be learned.
     * @throws RemoteException
     * @note the learner can decide to fail.
     */
    void learn(int round, V value) throws RemoteException;
}