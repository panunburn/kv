package id;

import java.io.File;
import java.rmi.*;
import java.rmi.server.RemoteServer;

import common.*;

public class Server implements UniqueIdService
{
    private static ServiceRegistry registry; 
    private static Long id;
    
    Server(int port, String path) throws RemoteException
    {
        registry = new ServiceRegistry(port);
        
        File f = new File(path);
        id = (Long) Utils.restore(f);
        if (id != null)
        {
            Logger.log("Previous id " + id + " restored from " + f.getAbsolutePath() + ".");
        }
        else
        {
            Logger.warning("Failed to restore the previous id from " + f.getAbsolutePath() + ".");
            id = new Long(1);
        }
    }
    
    public static void main(String[] args)
    {
        try
        {
            final int port;
            
            if (args.length == 0)
            {
                port = Config.defaultServerPortNumber();
            }
            else if (args.length == 1)
            {
                port = CmdLineParser.parsePort(args[0], Config.defaultServerPortNumber());
            }
            else
            {
                throw new CmdLineParserException("Invalid id server input. Usage: java id.Server <port>?.");
            }            

            if (Config.debugMode())
            {
                System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tY.%1$tm.%1$td.%1$tH.%1$tM.%1$tS.%1$tL %2$s]: %5$s%n");
                RemoteServer.setLog(Logger.getLogStream());
            }
            
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
                                                 {
                                                     Utils.save(id, new File(Config.defaultIdStorePath()));
                                                 }));
            
            Server server = new Server(port, Config.defaultIdStorePath());
            registry.start(server);
            Logger.log("Server is up.");
        }
        catch (ServiceRegistryException e)
        {
            Logger.error("Failed to start the service.", e);
            System.exit(-1);
        }
        catch (RemoteException e)
        {
            Logger.error("Failed to start the server.", e);
            System.exit(-1);
        }
        catch (CmdLineParserException e)
        {
            Logger.error(e);
            System.exit(-1);
        }
    }

    @Override
    public synchronized long next() throws RemoteException
    {
        return id++;
    }
}
