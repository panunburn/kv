/**
 * 
 */
package server;

import java.net.*;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import common.*;
import protocol.*;

/**
 * The local read set.
 */
class ReadSet
{
    private HashMap<String, Integer> readings;

    /**
     * Construct an empty read set.
     */
    public ReadSet()
    {
        readings = new HashMap<String, Integer>();
    }

    /**
     * Mark a key as reading key.
     * 
     * @param key the key to be marked. It should be not null.
     */
    public synchronized void mark(String key)
    {
        Logger.debug("Before marking, " + readings.toString());
        readings.merge(key, 1, (Integer o, Integer n) ->
        {
            return o + 1;
        });
        Logger.debug("After marking, " + readings.toString());
    }

    /**
     * Unmark a key as reading key.
     * 
     * @param key the key to be unmarked. It should be marked already.
     */
    public synchronized void unmark(String key)
    {
        Logger.debug("Before unmarking, " + readings.toString());
        readings.merge(key, 0, (Integer o, Integer n) ->
        {
            Integer v = o - 1;
            return v == 0 ? null : v;
        });
        Logger.debug("After marking, " + readings.toString());
    }

    /**
     * Check if the write key conflicts with any of the on-going read keys
     * 
     * @param writing the key that will be written. If null, then there is no
     *                conflict.
     * @return true if there is a conflict
     */
    private synchronized boolean conflict(String writing)
    {
        return readings.keySet().stream().anyMatch((String key) ->
        {
            return key.equals(writing);
        });
    }

    /**
     * Check if there is no conflict between the write set of the request and the
     * read set.
     * 
     * @param request the request to be checked.
     * @return true if there is no conflict.
     */
    public boolean validate(Request request)
    {
        String writing = request.accept(new RequestVisitor<String, NoThrow>()
                                        {
                                            @Override
                                            public String visit(DeleteRequest r)
                                            {
                                                return r.key;
                                            }
                                
                                            @Override
                                            public String visit(PutRequest r)
                                            {
                                
                                                return r.key;
                                            }
                                
                                            @Override
                                            public String visit(GetRequest r)
                                            {
                                                return null;
                                            }
                                
                                            @Override
                                            public String visit(PrintRequest r)
                                            {
                                                return null;
                                            }
                                        });
        return !conflict(writing);
    }

    @Override
    public String toString()
    {
        return "Read set: " + readings.toString();
    }
}

/**
 * Visitor to update the store.
 */
class ProcessRequest implements RequestVisitor<String, NoThrow>
{
    private ServerState state;

    public ProcessRequest(ServerState s)
    {
        state = s;
    }

    @Override
    public String visit(DeleteRequest r)
    {
        return state.store.delete(r.key);
    }

    @Override
    public String visit(PutRequest r)
    {
        return state.store.put(r.key, r.val);
    }

    @Override
    public String visit(GetRequest r) throws NoThrow
    {
        return null;
    }

    @Override
    public String visit(PrintRequest r) throws NoThrow
    {
        Logger.log(state.toString());
        return null;
    }
}

/**
 * The participant event listener.
 */
interface ParticipantListener
{
    public void onAdd(EndPoint addr);

    public void onRemove(EndPoint addr);

    public void onValidate(Request request);

    public void onCommit(Request request);

    public void onAbort(Request request);

    public void onShutdown(ReplicaService service);
}

/**
 * The participant/replicated service.
 */
class Participant implements ReplicaService
{
    private ServerState state;
    private ReadSet readset;
    private ParticipantListener listener;

    Participant(ServerState state, ReadSet readset, ParticipantListener listener)
    {
        this.state = state;
        this.readset = readset;
        this.listener = listener;
    }

    @Override
    public synchronized void remove(EndPoint addr) throws RemoteException
    {
        listener.onRemove(addr);
        state.replicas.remove(addr);
    }

    @Override
    public synchronized void add(EndPoint addr, ReplicaService service) throws RemoteException
    {
        listener.onAdd(addr);
        state.replicas.put(addr, service);
    }

    @Override
    public synchronized void shutdown() throws RemoteException
    {
        listener.onShutdown(this);
    }

    @Override
    public boolean validate(Request request) throws RemoteException
    {
        boolean result = readset.validate(request);
        if (result)
        {
            Logger.log("Ready to commit request " + request);
            return true;
        }
        else
        {
            Logger.log("Conflict occurred. Need to abort request " + request);
            return false;
        }
    }

    @Override
    public synchronized void commit(Request request) throws RemoteException
    {
        listener.onCommit(request);
        request.accept(new ProcessRequest(state));
    }

    @Override
    public synchronized void abort(Request request) throws RemoteException
    {
        listener.onAbort(request);
    }
}

/**
 * The coordinator service.
 */
class Coordinator implements CoordinatorService
{
    private KVStore store;
    private ReadSet readset;
    private HashMap<EndPoint, ReplicaService> mapper;

    public Coordinator(KVStore store, ReadSet readset)
    {
        this.store = store;
        this.readset = readset;
        this.mapper = new HashMap<EndPoint, ReplicaService>();
    }

    /**
     * Wait until all replicated servers have been fully initialized.
     */
    private void waitForServices()
    {
        while (!mapper.values().stream().allMatch((ReplicaService r) -> { return r != null; }))
        {
            try
            {
                Logger.debug("Thread " + Thread.currentThread() + " waits.");
                wait();
            }
            catch (InterruptedException e)
            {
                Logger.debug("Thread " + Thread.currentThread() + " has been waken up.");
            }
        }
    }

    /**
     * Exclude unresponsive servers from future operations as if they have been
     * disconnected.
     * 
     * @param unresponsive a list of unresponsive servers
     */
    private void exclude(ArrayList<EndPoint> unresponsive)
    {
        while (unresponsive.size() != 0)
        {
            for (EndPoint u : unresponsive)
            {
                Logger.log("Removing unresponsive server " + u + " in coordinator.");
                mapper.remove(u);
            }

            ArrayList<EndPoint> newUnresponsive = new ArrayList<EndPoint>();
            for (EndPoint u : unresponsive)
            {
                mapper.forEach((EndPoint p, ReplicaService r) ->
                                {
                                    try
                                    {
                                        Logger.log("Removing unresponsive server " + u + " in " + p + ".");
                                        r.remove(u);
                                    }
                                    catch (RemoteException e)
                                    {
                                        Logger.warning("Replicated server " + p + " has lost contact.", e);
                                        newUnresponsive.add(p);
                                    }
                                });
            }
            unresponsive = newUnresponsive;
        }
    }

    /**
     * Allow the replicated server to connect and initialize itself with the state
     * the coordinator currently has. The client has to register its replica service
     * to fully initialized itself in the coordinator. All subsequent service calls
     * (except connect and register) have to wait until all partially initialized
     * services have been fully initialized.
     * 
     * @param replica the replicated server
     * @return the local key value store and all the currently available servers.
     */
    @Override
    public synchronized ServerState connect(EndPoint replica) throws RemoteException
    {
        // make a copy of the old state
        HashMap<EndPoint, ReplicaService> keys = new HashMap<EndPoint, ReplicaService>(mapper);

        // partially initialize the replicated server
        mapper.put(replica, null);

        Logger.log(replica + " has connected.");

        return new ServerState(store, keys);
    }

    /**
     * Register the replicated server to the coordinator so that it will be fully
     * initialized. Notify other blocked operations.
     */
    @Override
    public synchronized void register(EndPoint replica, ReplicaService service) throws RemoteException
    {
        ReplicaService partial = mapper.put(replica, service);
        if (partial != null)
        {
            Logger.warning("Replicated server " + replica + " has already been fully initialized.");
        }
        notifyAll();

        waitForServices();

        ArrayList<EndPoint> unresponsive = new ArrayList<EndPoint>();
        mapper.forEach((EndPoint p, ReplicaService r) ->
                        {
                            if (!p.equals(replica))
                            {
                                try
                                {
                                    Logger.log("Adding replicated server " + replica + " in " + p + ".");
                                    r.add(replica, service);
                                }
                                catch (RemoteException e)
                                {
                                    Logger.warning("Replicated server " + p + " didn't respond in time.", e);
                                    unresponsive.add(p);
                                }
                            }
                        });
        exclude(unresponsive);

        Logger.log(replica + " has registered itself.");
    }

    @Override
    public synchronized void disconnect(EndPoint replica) throws RemoteException
    {
        waitForServices();

        mapper.remove(replica);

        ArrayList<EndPoint> unresponsive = new ArrayList<EndPoint>();
        mapper.forEach((EndPoint p, ReplicaService r) ->
                        {
                            try
                            {
                                Logger.log("Removing replicated server " + replica + " in " + p + ".");
                                r.remove(replica);
                            }
                            catch (RemoteException e)
                            {
                                Logger.warning("Replicated server " + p + " didn't respond in time.", e);
                                unresponsive.add(p);
                            }
                        });
        exclude(unresponsive);

        Logger.log(replica + " has been disconnected.");
    }

    @Override
    public synchronized void shutdown() throws RemoteException
    {
        waitForServices();

        mapper.forEach((EndPoint p, ReplicaService r) ->
                        {
                            try
                            {
                                Logger.log("Shutting down replicated server " + p + ".");
                                r.shutdown();
                            }
                            catch (RemoteException e)
                            {
                                Logger.warning("Replicated server " + p + " didn't respond in time.", e);
                            }
                        });
    }

    /**
     * Broadcast a request to all available replicated servers. The two-phase commit
     * protocol is adopted to commit or abort a request.
     * 
     * @param request the request to be processed
     * @return a nullable value after processing the request.
     * @throws RemoteException if request got aborted or any communication-related
     *                         issues have occurred.
     */
    @Override
    public synchronized String broadcast(Request request) throws RemoteException
    {
        waitForServices();

        // two-phase commit protocol

        // 1. voting phase
        Logger.log("Validating request " + request);
        boolean coordVote = readset.validate(request);
        Logger.log("Validated request " + request + " on coordinator with result " + coordVote + ".");

        HashMap<EndPoint, Boolean> responded = new HashMap<EndPoint, Boolean>();
        ArrayList<EndPoint> unresponsive = new ArrayList<EndPoint>();
        for (Map.Entry<EndPoint, ReplicaService> i : mapper.entrySet())
        {
            try
            {
                boolean vote = i.getValue().validate(request);
                Logger.log("Validated request " + request + " on server " + i.getKey() + " with result " + vote + ".");
                responded.put(i.getKey(), vote);
            }
            catch (RemoteException e)
            {
                Logger.warning("Replicated server " + i.getKey() + " didn't respond in time.", e);
                unresponsive.add(i.getKey());
            }
        }
        exclude(unresponsive);

        // 2. completion phase
        unresponsive.clear();
        if (coordVote && responded.values().stream().allMatch((Boolean b) -> { return b; }))
        {
            Logger.log("Committing request " + request);
            mapper.forEach((EndPoint p, ReplicaService r) ->
            {
                try
                {
                    Logger.log("Committing request " + request + " on server " + p + ".");
                    r.commit(request);
                }
                catch (RemoteException e)
                {
                    Logger.warning("Replicated server " + p + " didn't respond in time.", e);
                    unresponsive.add(p);
                }
            });
            exclude(unresponsive);

            String val = request.accept(new ProcessRequest(new ServerState(store, mapper)));
            Logger.log("Request " + request + " has been committed.");
            return val;
        }
        else
        {
            Logger.log("Aborting request " + request);
            responded.forEach((EndPoint p, Boolean v) ->
                              {
                                  if (v.booleanValue() == true)
                                  {
                                      try
                                      {
                                          mapper.get(p).abort(request);
                                      }
                                      catch (RemoteException e)
                                      {
                                          Logger.warning("Replicated server " + p + " didn't respond in time.", e);
                                          unresponsive.add(p);
                                      }
                                   }
                              });
            exclude(unresponsive);
            Logger.log("Request " + request + " has been aborted.");
            throw new TransactionAbortException();
        }
    }
}

/**
 * The store service.
 */
class Store implements StoreService
{
    CoordinatorService coordinator;
    ReadSet readset;
    KVStore store;

    public Store(CoordinatorService coordinator, ReadSet readset, KVStore store)
    {
        this.coordinator = coordinator;
        this.readset = readset;
        this.store = store;
    }

    /**
     * Process a request.
     * 
     * @param request the request to be processed
     * @note multiple GET requests will be processed concurrently.
     * @note PUT/DELETE/PRINT request will be forwarded to the coordinator to
     *       broadcast to other replicated servers.
     * @return a nullable value depending on the request type.
     * @throws RemoteException if a conflict occurred between the write set of the
     *                         broadcast request and the read set of the concurrent
     *                         reads or any other communication-related issues
     *                         occurred.
     */
    @Override
    public String process(Request request) throws RemoteException
    {
        return request.accept(new RequestVisitor<String, RemoteException>()
                              {
                                    @Override
                                    public String visit(GetRequest r)
                                    {
                                        readset.mark(r.key);
                                        final String val = store.get(r.key);
                                        readset.unmark(r.key);
                                        return val;
                                    }
                        
                                    @Override
                                    public String visit(DeleteRequest r) throws RemoteException
                                    {
                                        return coordinator.broadcast(r);
                                    }
                        
                                    @Override
                                    public String visit(PutRequest r) throws RemoteException
                                    {
                                        return coordinator.broadcast(r);
                                    }
                        
                                    @Override
                                    public String visit(PrintRequest r) throws RemoteException
                                    {
                                        return coordinator.broadcast(r);
                                    }
                              });
    }
}

/**
 * The Server.
 */
class Server
{
    private static Registry registry;
    private static EndPoint local;
    private static ReadSet readset;

    private static CoordinatorService coordinator;
    private static StoreService store;
    private static ServerState state;

    private static boolean shutdownByCoordinator = false;

    public static void initialize(EndPoint coordAddr, int port) throws RemoteException, NotBoundException, UnknownHostException
    {
        registry = LocateRegistry.createRegistry(port);
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(Config.defaultResponseTimeout()));

        local = new EndPoint(InetAddress.getLocalHost(), port);
        readset = new ReadSet();

        if (coordAddr == null)
        {
            // run as coordinator
            KVStore kv = new KVStore(Config.defaultStorePath());
            HashMap<EndPoint, ReplicaService> replicas = new HashMap<EndPoint, ReplicaService>();
            state = new ServerState(kv, replicas);
            Logger.log("Initialized coordinator server state.\n" + state.toString());

            coordinator = new Coordinator(kv, readset);
            registry.rebind(Config.defaultCoordinatorServiceName(), UnicastRemoteObject.exportObject(coordinator, 0));
            Logger.log("Initialized coordinator service.");
        }
        else
        {
            // run as replica
            try
            {
                coordinator = (CoordinatorService) LocateRegistry.getRegistry(coordAddr.getHost().getHostAddress(), coordAddr.getPort()).lookup(Config.defaultCoordinatorServiceName());
                Logger.log("Found coordinator service on " + coordAddr + ".");
            }
            catch (RemoteException | NotBoundException e)
            {
                Logger.error("Failed to find the coordinator service on " + coordAddr + ".");
                throw e;
            }

            state = coordinator.connect(local);
            Logger.log("Connected coordinator service and initialized replicated server state.");
            Logger.log(state.toString());

            ReplicaService replica = new Participant(state, readset, new ParticipantListener()
                                     {
                                            @Override
                                            public void onRemove(EndPoint addr)
                                            {
                                                Logger.log("Removing replicated server " + addr + ".");
                                            }
                            
                                            @Override
                                            public void onAdd(EndPoint addr)
                                            {
                                                Logger.log("Adding replicated server " + addr + ".");
                                            }
                            
                                            @Override
                                            public void onShutdown(ReplicaService replica)
                                            {
                                                Logger.log("Recevied shutdown event from the coordinator.");
                            
                                                try
                                                {
                                                    registry.unbind(Config.defaultStoreServiceName());
                                                    UnicastRemoteObject.unexportObject(store, true);
                            
                                                    registry.unbind(Config.defaultReplicaServiceName());
                                                    UnicastRemoteObject.unexportObject(replica, true);
                            
                                                    shutdownByCoordinator = true;
                                                }
                                                catch (RemoteException | NotBoundException e)
                                                {
                                                    Logger.warning("Failed to shutdown replicated server.", e);
                                                }
                                            }
                            
                                            @Override
                                            public void onValidate(Request request)
                                            {
                                                Logger.log("Validating request " + request);
                                            }
                            
                                            @Override
                                            public void onCommit(Request request)
                                            {
                                                Logger.log("Commit request " + request);
                                            }
                            
                                            @Override
                                            public void onAbort(Request request)
                                            {
                                                Logger.log("Abort request " + request);
                                            }
                                     });
            registry.rebind(Config.defaultReplicaServiceName(), UnicastRemoteObject.exportObject(replica, 0));
            coordinator.register(local, replica);
            Logger.log("Initialized replica service.");
        }

        store = new Store(coordinator, readset, state.store);
        registry.rebind(Config.defaultStoreServiceName(), UnicastRemoteObject.exportObject(store, 0));
        Logger.log("Initialized store service.");
    }

    public static void main(String[] args)
    {
        try
        {
            if (Config.debugMode())
            {
                System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tY.%1$tm.%1$td.%1$tH.%1$tM.%1$tS.%1$tL %2$s]: %5$s%n");
                RemoteServer.setLog(Logger.getLogStream());
            }

            final EndPoint coordAddr;
            final int port;

            if (args.length == 0)
            {
                coordAddr = null;
                port = Config.defaultServerPortNumber();
            }
            else if (args.length == 1)
            {
                coordAddr = null;
                port = CmdLineParser.parsePort(args[0], Config.defaultServerPortNumber());
            }
            else if (args.length == 2)
            {
                coordAddr = CmdLineParser.parseEndPoint(args[0], Config.defaultServerPortNumber());
                port = CmdLineParser.parsePort(args[1], Config.defaultServerPortNumber());
            }
            else
            {
                throw new CmdLineParserException("Invalid server input. Usage: java server.Server <port>? | <address> <port>.");
            }

            try
            {
                initialize(coordAddr, port);

                Logger.log("Server is up at host " + local.getHost().getHostName() + " with address " + local.getHost().getHostAddress() + " and port " + local.getPort() + ".");

                // setup exit handler to save the store or disconnect the service when server
                // exits
                Runtime.getRuntime().addShutdownHook(new Thread(() ->
                                                    {
                                                        if (coordAddr == null)
                                                        {
                                                            Logger.log("Shutting down coordinator  ...");
                                                            Logger.log("Saving the store ...");
                                                            state.store.save(Config.defaultStorePath());
                                    
                                                            if (Config.exitWhenCoordinatorFails())
                                                            {
                                                                try
                                                                {
                                                                    coordinator.shutdown();
                                                                }
                                                                catch (RemoteException e)
                                                                {
                                                                    Logger.error("Failed to shutdown the coordinator.", e);
                                                                }
                                                            }
                                                            else
                                                            {
                                                                // TODO elect a new coordinator
                                                            }
                                                        }
                                                        else
                                                        {
                                                            Logger.log("Shutting down replicated server ...");
                                                            if (!shutdownByCoordinator)
                                                            {
                                                                try
                                                                {
                                                                    coordinator.disconnect(local);
                                                                }
                                                                catch (RemoteException e)
                                                                {
                                                                    Logger.error("Failed to disconnect from the coordinator.", e);
                                                                }
                                                            }
                                                        }
                                                    }));
            }
            catch (Exception e)
            {
                Logger.error("Failed to start the server.", e);
                System.exit(-1);
            }
        }
        catch (CmdLineParserException e)
        {
            Logger.error(e);
            System.exit(-1);
        }
    }
}