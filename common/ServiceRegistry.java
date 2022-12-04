package common;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.Stream;

/**
 * ServiceRegistry handles the service management more conveniently.
 */
public class ServiceRegistry
{
    /**
     * Connect to a service using an endpoint.
     * @param <S> the service type
     * @param endpoint the endpoint to connect
     * @param service the service class
     * @return the service object if it exits.
     * @throws AccessException
     * @throws RemoteException
     * @throws NotBoundException
     */
    @SuppressWarnings("unchecked")
    public static <S extends Remote> S connect(EndPoint endpoint, Class<S> service) throws AccessException, RemoteException, NotBoundException
    {
        return (S) LocateRegistry.getRegistry(endpoint.getHost().getHostAddress(), endpoint.getPort()).lookup(service.getSimpleName());
    }
    
    private Registry registry;
    
    /**
     * Create a registry on a specified port.
     * @param port the port number
     * @throws RemoteException
     */
    public ServiceRegistry(int port) throws RemoteException
    {
        registry = LocateRegistry.createRegistry(port);
    }
    
    private Class<?> find(Class<?> root) throws ServiceRegistryException
    {
        Stream<Class<?>> rs = Arrays.asList(root.getInterfaces()).stream().filter((Class<?> i) -> { return Remote.class.isAssignableFrom(i); });

        Iterator<Class<?>> i = rs.iterator();
        if (!i.hasNext())
        {
            throw new ServiceRegistryException("Failed to identify remote service name.");
        }
        else 
        {
            Class<?> r = i.next();
            if (!i.hasNext())
            {
                return r;
            }
            else
            {
                Class<?> c = i.next();
                throw new ServiceRegistryException("Unexpected remote service name. Got: " + r.getSimpleName() + " and " + c.getSimpleName() + (i.hasNext() ? " ..." : "."));
            }
        }
    }
    
    /**
     * Start a remote service with this registry.
     * @param o the remote service object
     * @throws AccessException
     * @throws RemoteException
     * @throws ServiceRegistryException
     */
    public void start(Remote o) throws AccessException, RemoteException, ServiceRegistryException
    {
        Class<?> r = find(o.getClass());
        registry.rebind(r.getSimpleName(), UnicastRemoteObject.exportObject(o, 0));
        Logger.log("Service " + r.getSimpleName() + " has started.");
    }
    
    /**
     * Shutdown the remote service started in this registry.
     * @param o the remote service object 
     * @throws AccessException
     * @throws RemoteException
     * @throws NotBoundException
     * @throws ServiceRegistryException 
     */
    public void shutdown(Remote o) throws AccessException, RemoteException, NotBoundException, ServiceRegistryException
    {
        Class<?> r = find(o.getClass());
        registry.unbind(r.getSimpleName());
        UnicastRemoteObject.unexportObject(o, true);
        Logger.log("Service " + r.getSimpleName() + " has been shutdown.");
    }
}