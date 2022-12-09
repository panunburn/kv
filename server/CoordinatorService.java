package server;

import java.rmi.*;
import common.EndPoint;
import protocol.*;

/**
 * The service interfaces between the server and the coordinator.
 */
public interface CoordinatorService extends PaxosService<Request>
{
    /**
     * Connect a replicated server to the coordinator.
     * 
     * @param replica the endpoint location for the server
     * @return the current server state.
     * @throws RemoteException
     */
    ServerState connect(EndPoint replica) throws RemoteException;

    /**
     * Register the replica/participant service in the coordinator.
     * 
     * @param replica the endpoint location for the server
     * @param service the replica service
     * @throws RemoteException
     */
    void register(EndPoint replica, ReplicaService service) throws RemoteException;

    /**
     * Disconnect a replicated server.
     * 
     * @param replica the endpoint location for the server
     * @throws RemoteException
     */
    void disconnect(EndPoint replica) throws RemoteException;

    /**
     * Shutdown the coordinator.
     * 
     * @throws RemoteException
     */
    void shutdown() throws RemoteException;

    /**
     * Process the request and update the replicas when the request commits.
     * 
     * @param request the request to be processed.
     * @return a response depending on the request type.
     * @throws TransactionAbortException if the request in the current transaction is aborted.
     * @throws RemoteException 
     */
    Response process(Request request) throws RemoteException;
}
