/**
 * 
 */
package server;

import java.io.Serializable;
import java.net.*;
import java.rmi.*;
import java.rmi.server.RemoteServer;
import java.util.*;
import java.util.concurrent.*;

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
 * Generic PAXOS service.
 */
class Paxos<V extends Serializable> implements PaxosService<V>
{
    private final static Random rand = new Random();
    private PaxosState<V> state;
    
    Paxos()
    {
        this(new PaxosState<>());
    }
    
    Paxos(PaxosState<V> state)
    {
        this.state = state;
    }
    
    public static boolean mightFail()
    {
        return rand.nextInt(100) <= Config.defaultPaxosFailureRate(); 
    }
    
    @Override
    public synchronized Promise<V> prepare(int round, long id) throws RemoteException
    {
        if (mightFail())
        {
            Logger.debug("Acceptor decides to fail.");
            return null;
        }
        
        Pair<Long, Proposal<V>> p = state.get(round);
        if (p == null)
        {
            state.init(round, id);            
            return new Promise<>(id);
        }
        else // round existed
        {
            if (id <= p.getFirst())
            {
                return null;
            }
            else 
            {                
                p.setFirst(id);
                if (p.getSecond() == null) // no accepted proposal yet
                {
                    return new Promise<>(id);
                }
                else // some accepted proposal
                {
                    return new Promise<>(id, p.getSecond());                    
                }
            }
        }
    }
    
    @Override
    public synchronized V accept(int round, Proposal<V> proposal) throws RemoteException
    {
        if (mightFail()) 
        {
            Logger.debug("Acceptor decides to fail.");
            return null;
        }
        
        Pair<Long, Proposal<V>> p = state.get(round);
        if (p == null)
        {
            throw new PaxosException("Failed to accept " + proposal + ". It hasn't been proposed yet.");
        }
        else // round existed
        {
            if (proposal.getId() == p.getFirst())
            {
                p.setSecond(proposal);
                return proposal.getValue();
            }
            else
            {
                return null;
            }
        }
    }
    
    @Override
    public void learn(int round, V value) throws RemoteException 
    {
        if (mightFail())
        {
            Logger.debug("Learner decides to fail.");
            return;
        }
        
        Logger.log("Paxos round " + round + " has learned value " + value + ".");
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
class Replica implements ReplicaService
{
    private ServerState state;
    private ReadSet readset;
    private ParticipantListener listener;
    private Paxos<Request> logs;

    Replica(ServerState state, ReadSet readset, ParticipantListener listener)
    {
        this.state = state;
        this.readset = readset;
        this.listener = listener;
        this.logs = new Paxos<>(state.paxos);
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

    @Override
    public Promise<Request> prepare(int round, long id) throws RemoteException
    {
        return logs.prepare(round, id);
    }

    @Override
    public Request accept(int round, Proposal<Request> proposal) throws RemoteException
    {
        return logs.accept(round, proposal);
    }

    @Override
    public void learn(int round, Request value) throws RemoteException
    {
        logs.learn(round, value);
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
    private final EndPoint local; 
    private HashSet<EndPoint> partial;
    private Paxos<Request> logs;
    private final ExecutorService pool = Executors.newFixedThreadPool(Config.defaultPaxosThreads());
    
    public Coordinator(UniqueIdService id, 
                       ServerState state, 
                       ReadSet readset, 
                       EndPoint local)
    {
        this.id = id;
        this.state = state;
        this.readset = readset;
        this.local = local;
        this.partial = new HashSet<>();
        this.logs = new Paxos<>(state.paxos);
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
                Thread.currentThread().interrupt();
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
        
        pool.shutdown();
        try
        {
            while (!pool.awaitTermination(Config.defaultPaxosTimeout(), TimeUnit.MILLISECONDS))
            {
                pool.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isMajority(int n, int N)
    {
        return n > (N / 2);
    }

    private static class PaxosFailure extends Exception
    {
        private static final long serialVersionUID = 1L;
        
        PaxosFailure(String msg)
        {
            super(msg);
        }
    }
    
    /**
     * Propose a value to agree on for a PAXOS round
     * @param round the PAXOS round
     * @param value the value to agree on
     * @return true if the value is actually accepted for the current round.
     * @throws RemoteException if the id service fails.
     * @throws PaxosFailure if either the distinguished proposer or learner decides to fail.
     */
    private synchronized boolean propose(int round, Request value) throws RemoteException, PaxosFailure
    {
        Logger.log("Running PAXOS round " + round + " with committed request " + value + ".");

        if (Paxos.mightFail())
        {
            throw new PaxosFailure("The distinguished proposer decides to fail before phase 1.");
        }
        
        // phase 1
        final HashMap<EndPoint, Promise<Request>> promises = new HashMap<>(state.replicas.size());
        final ArrayList<EndPoint> unresponsive = new ArrayList<EndPoint>();
        while (!isMajority(promises.size(), state.replicas.size()))
        {
            final long n = id.next();

            // recollect promises
            promises.clear();
            
            {
                Promise<Request> p = prepare(round, n);
                if (p != null)
                {
                    Logger.log("Got " + p + " from " + local + ".");
                    promises.put(local, p);
                }
            }
            
            unresponsive.clear();
            state.replicas.forEach((a, r) -> 
                                   {
                                       try
                                       {
                                           Promise<Request> p = r.prepare(round, n);
                                           if (p != null)
                                           {
                                               Logger.log("Got " + p + " from " + a + ".");
                                               promises.put(a, p);
                                           }
                                       }
                                       catch (RemoteException e)
                                       {
                                           Logger.warning("Replicated server " + a + " didn't respond in time.", e);
                                           unresponsive.add(a);
                                       }
                                   });
            exclude(unresponsive);
        }
        
        if (Paxos.mightFail())
        {
            throw new PaxosFailure("The distinguished proposer decides to fail after phase 1 but before phase 2.");
        }
        
        // phase 2
        final long pid = promises.values().stream().findAny().get().getId(); // promises shouldn't be empty
        final Optional<Promise<Request>> highest = promises.values().stream()
                                                  .filter((p) -> { return p.getProposal() != null; })
                                                  .max((a, b) -> { return Long.compare(a.getProposal().getId(), b.getProposal().getId());});
        final Request val = highest.isPresent() ? highest.get().getProposal().getValue() : value;
        final Proposal<Request> proposal = new Proposal<Request>(pid, val);

        // collect accepted values based on promises
        final ArrayList<Request> accepted = new ArrayList<>(promises.size());
        while (accepted.isEmpty())
        {
            unresponsive.clear();
            promises.forEach((EndPoint a, Promise<Request> p) -> 
                             {
                                 try
                                 {
                                     final ReplicaService r = state.replicas.get(a);
                                     
                                     final Request v;
                                     if (r == null) // coordinator
                                     {
                                         v = accept(round, proposal);
                                     }
                                     else 
                                     {
                                         v = r.accept(round, proposal);
                                     }
                                     
                                     if (v != null) 
                                     {
                                         Logger.log("Server " + a + " has accepted " + proposal + ".");
                                         accepted.add(v);
                                     }
                                 }
                                 catch (PaxosException e)
                                 {
                                     Logger.debug(e);
                                 }
                                 catch (RemoteException e)
                                 {
                                     Logger.warning("Replicated server " + a + " didn't respond in time.", e);
                                     unresponsive.add(a);
                                 }
                             });
            exclude(unresponsive);
        }
        
        if (Paxos.mightFail())
        {
            throw new PaxosFailure("The distinguished learner decides to fail.");
        }
        
        // learn the accepted value
        Logger.debug("Accepted values: " + accepted);
        final Request agreed = accepted.stream().findAny().get(); // note the accepted cannot be empty
        learn(round, agreed);
        unresponsive.clear();
        state.replicas.forEach((EndPoint a, ReplicaService r) ->
                                {
                                    try
                                    {
                                        r.learn(round, agreed);
                                        Logger.log("Server " + a + " has learned value " + agreed + " in round " + round + ".");
                                    }
                                    catch (RemoteException e)
                                    {
                                        unresponsive.add(a);
                                    }
                                });
        exclude(unresponsive);
        
        return !highest.isPresent() || highest.get().getProposal().getValue().equals(value);
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
        final HashMap<EndPoint, Boolean> votes = new HashMap<EndPoint, Boolean>(state.replicas.size());
        {
            boolean vote = readset.validate(request);
            if (vote)
            {
                votes.put(local, vote);
                Logger.log("Validated request " + request + " on coordinator with result " + vote + ".");
            }
        }

        final ArrayList<EndPoint> unresponsive = new ArrayList<EndPoint>();
        state.replicas.forEach((a, r) -> 
                               {
                                   try
                                   {
                                       boolean vote = r.validate(request);
                                       Logger.log("Validated request " + request + " on server " + a + " with result " + vote + ".");
                                       votes.put(a, vote);
                                   }
                                   catch (RemoteException e)
                                   {
                                       Logger.warning("Replicated server " + a + " didn't respond in time.", e);
                                       unresponsive.add(a);
                                   } 
                               });
        exclude(unresponsive);

        // 2. completion phase
        unresponsive.clear();
        if (votes.values().stream().allMatch((Boolean b) -> { return b; }))
        {
            // run PAXOS concurrently
            Future<Void> f = pool.submit(() -> 
                                        {
                                           int round = state.paxos.getNextRound();
                                           while (true)
                                           {
                                               try
                                               {
                                                   boolean behind = !propose(round, request);
                                                   if (behind)
                                                   {
                                                       Logger.debug("PAXOS round " + round + " finished but behind.");
                                                       round++;
                                                   }
                                                   else
                                                   {
                                                       Logger.debug("PAXOS round " + round + " finished.");
                                                       return null;
                                                   }
                                               }   
                                               catch (PaxosFailure e)
                                               {
                                                   Logger.warning(e);
                                                   // retry current round
                                               }
                                           }
                                        });
            
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

            final String response = request.accept(new ProcessRequest(state));
            Logger.log("Request " + request + " has been committed.");
            
            // run PAXOS concurrently
            pool.submit(() -> 
                        {
                            try
                            {
                                f.get(Config.defaultPaxosTimeout(), TimeUnit.MILLISECONDS);
                            }
                            catch (TimeoutException e)
                            {
                                Logger.warning("PAXOS didn't complete in time.");
                                f.cancel(true);
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                            catch (ExecutionException e)
                            {
                                Logger.warning("PAXOS didn't complete properly.", e);
                            }
                        });
            
            return response;
        }
        else
        {
            Logger.log("Aborting request " + request);
            votes.forEach((EndPoint p, Boolean v) ->
                          {
                              final ReplicaService r = state.replicas.get(p);
                              if (r != null) // replicas
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
                              }
                          });
            exclude(unresponsive);
            Logger.log("Request " + request + " has been aborted.");
            throw new TransactionAbortException();
        }
    }

    @Override
    public Promise<Request> prepare(int round, long id) throws RemoteException
    {
        return logs.prepare(round, id);
    }

    @Override
    public Request accept(int round, Proposal<Request> proposal) throws RemoteException
    {
        return logs.accept(round, proposal);
    }

    @Override
    public void learn(int round, Request value) throws RemoteException
    {
        logs.learn(round, value);
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
public class Server
{
    private static ServiceRegistry registry;
    private static boolean shutdownByCoordinator = false;

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
            
            final EndPoint local;
            final ReadSet readset;
            final ServerState state;
            
            final CoordinatorService coordinator;
            final StoreService store;

            try
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
                
                    coordinator = new Coordinator(id, state, readset, local);
                    Logger.log("Initialized coordinator service.");
                    registry.start(coordinator);
                    
                    store = new Store(coordinator, state, readset);
                    Logger.log("Initialized store service.");
                }
                else
                {
                    try
                    {
                        coordinator = ServiceRegistry.connect(addr, CoordinatorService.class);
                        Logger.log("Found coordinator service on " + addr + ".");
                    }
                    catch (RemoteException | NotBoundException e1)
                    {
                        Logger.error("Failed to find the coordinator service on " + addr + ".");
                        throw e1;
                    }
                
                    state = coordinator.connect(local);
                    Logger.log("Connected coordinator service and initialized replicated server state.\n" + state);
                
                    store = new Store(coordinator, state, readset);
                    Logger.log("Initialized store service.");

                    ReplicaService replica = new Replica(state, readset, 
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
                registry.start(store);

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