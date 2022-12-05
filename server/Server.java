/**
 * 
 */
package server;

import java.net.*;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.server.RemoteServer;
import java.util.*;

import common.*;
import id.UniqueIdService;
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
        return state.store.get(r.key);
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
    private UniqueIdService id;
    private ServerState state;
    private ReadSet readset;
    private HashSet<EndPoint> partial;

    public Coordinator(UniqueIdService id, ServerState state, ReadSet readset)
    {
        this.id = id;
        this.state = state;
        this.readset = readset;
        this.partial = new HashSet<>();
    }
    
    /**
     * Wait until all replicated servers have been fully initialized.
     */
    private void waitForServices()
    {
        while (!partial.isEmpty())
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
                state.replicas.remove(u);
            }

            ArrayList<EndPoint> newUnresponsive = new ArrayList<EndPoint>();
            for (EndPoint u : unresponsive)
            {
                state.replicas.forEach((EndPoint p, ReplicaService r) ->
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
        partial.add(replica);
        Logger.log(replica + " has connected.");
        return state;
    }

    /**
     * Register the replicated server to the coordinator so that it will be fully
     * initialized. Notify other blocked operations.
     */
    @Override
    public synchronized void register(EndPoint replica, ReplicaService service) throws RemoteException
    {
        partial.remove(replica);
        state.replicas.put(replica, service);
        notifyAll();

        waitForServices();

        ArrayList<EndPoint> unresponsive = new ArrayList<EndPoint>();
        state.replicas.forEach((EndPoint p, ReplicaService r) ->
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

        state.replicas.remove(replica);

        ArrayList<EndPoint> unresponsive = new ArrayList<EndPoint>();
        state.replicas.forEach((EndPoint p, ReplicaService r) ->
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

        state.replicas.forEach((EndPoint p, ReplicaService r) ->
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
        for (Map.Entry<EndPoint, ReplicaService> i : state.replicas.entrySet())
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
            state.replicas.forEach((EndPoint p, ReplicaService r) ->
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

            final String val = request.accept(new ProcessRequest(state));
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
                                          state.replicas.get(p).abort(request);
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
    private CoordinatorService coordinator;
    private ServerState state;
    private ReadSet readset;

    public Store(CoordinatorService coordinator, ServerState state, ReadSet readset)
    {
        this.coordinator = coordinator;
        this.state = state;
        this.readset = readset;
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
                                        final String val = r.accept(new ProcessRequest(state));
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

enum ServerType 
{
    Coordinator("coordinator"),
    Replica("replica");
    
    private String text;
    
    ServerType(String text)
    {
        this.text = text;
    }
    
    public String getText() { return this.text; }
    
    public static ServerType parse(String s)
    {
        for (ServerType t : ServerType.values())
        {
            if (t.text.equals(s))
            {
                return t;
            }
        }
        return null;
    }
}

/**
 * The Server.
 */
class Server
{
    private static ServiceRegistry registry;
    
    private static EndPoint local;
    private static ReadSet readset;

    private static CoordinatorService coordinator;
    private static StoreService store;
    private static ServerState state;

    private static boolean shutdownByCoordinator = false;

    public static void initialize(ServerType type, EndPoint addr, int port) throws RemoteException, NotBoundException, UnknownHostException, ServiceRegistryException
    {
        registry = new ServiceRegistry(port);
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(Config.defaultResponseTimeout()));

        local = new EndPoint(InetAddress.getLocalHost(), port);
        readset = new ReadSet();

        if (type.equals(ServerType.Coordinator))
        {
            UniqueIdService id = ServiceRegistry.connect(addr, UniqueIdService.class);            
            state = new ServerState();
            Logger.log("Initialized coordinator server state.\n" + state);

            coordinator = new Coordinator(id, state, readset);
            Logger.log("Initialized coordinator service.");

            registry.start(coordinator);
        }
        else
        {
            try
            {
                coordinator = ServiceRegistry.connect(addr, CoordinatorService.class);
                Logger.log("Found coordinator service on " + addr + ".");
            }
            catch (RemoteException | NotBoundException e)
            {
                Logger.error("Failed to find the coordinator service on " + addr + ".");
                throw e;
            }

            state = coordinator.connect(local);
            Logger.log("Connected coordinator service and initialized replicated server state.\n" + state);

            ReplicaService replica = new Participant(state, readset, 
                                                     new ParticipantListener()
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
                                                                    registry.shutdown(store);
                                                                    registry.shutdown(replica);
                                                                    shutdownByCoordinator = true;
                                                                }
                                                                catch (ServiceRegistryException | RemoteException | NotBoundException e)
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
            
            registry.start(replica);
            coordinator.register(local, replica);
            Logger.log("Initialized replica service.");
        }

        store = new Store(coordinator, state, readset);
        registry.start(store);
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

            final ServerType type;
            final EndPoint addr;
            final int port;

            if (args.length == 3)
            {
                type = ServerType.parse(args[0]);
                addr = CmdLineParser.parseEndPoint(args[1], Config.defaultServerPortNumber());
                port = CmdLineParser.parsePort(args[2], Config.defaultServerPortNumber());
            }
            else
            {
                throw new CmdLineParserException("Invalid server input. Usage: java server.Server coordinator <endpoint> <port>.");
            }
            
            try
            {
                initialize(type, addr, port);

                Logger.log(type + " is up at host " + local.getHost().getHostName() + " with address " + local.getHost().getHostAddress() + " and port " + local.getPort() + ".");

                // setup exit handler to save the store or disconnect the service when server exits
                Runtime.getRuntime().addShutdownHook(new Thread(() ->
                                                    {
                                                        Logger.log("Shutting down " + type + " ...");
                                                        
                                                        if (type.equals(ServerType.Coordinator))
                                                        {
                                                            Logger.log("Saving the store ...");
                                                            state.store.save(Config.defaultKVStorePath());
                                    
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