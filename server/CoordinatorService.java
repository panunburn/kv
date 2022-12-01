package server;

import java.rmi.*;
import common.EndPoint;
import protocol.Request;

/**
 * The service interfaces between the server and the coordinator.
 */
public interface CoordinatorService extends Remote
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
     * Broadcast the request to all available replicated servers.
     * 
     * @param request the request to be broadcasted
     * @return a nullable value depending on the request type.
     * @throws RemoteException if the request aborted or any communication-related
     *                         issues occurred.
     */
    String broadcast(Request request) throws RemoteException;
}
