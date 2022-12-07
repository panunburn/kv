package common;

import java.rmi.registry.Registry;

/**
 * 
 * This class contains some global configurations for both the client and
 * server.
 *
 */
public class Config
{
    /**
     * 
     * @return path to the previously saved key value store.
     */
    public static String defaultKVStorePath()
    {
        return "./kv.store";
    }

    /**
     * 
     * @return path to the previously saved id store.
     */
    public static String defaultIdStorePath()
    {
        return "./id.store";
    }
    
    /**
     * @return the default port for the server RMI registry.
     */
    public static int defaultServerPortNumber()
    {
        return Registry.REGISTRY_PORT;
    }

    /**
     * @return the default host name for the server RMI registry.
     */
    public static String defaultServerHostName()
    {
        return "localhost";
    }

    public static String defaultAddrDelim()
    {
        return ":";
    }
    
    public static boolean debugMode()
    {
        return true;
    }

    public static boolean exitWhenCoordinatorFails()
    {
        return true;
    }

    /**
     * @return the default timeout used for sun.rmi.transport.tcp.responseTimeout in
     *         milliseconds.
     */
    public static int defaultResponseTimeout()
    {
        return 3000;
    }
    
    /**
     * @return the default number of threads used by PAXOS.
     */
    public static int defaultPaxosThreads()
    {
        return 4;
    }
    
    /**
     * @return the default failure rate in percentage for PAXOS acceptors.
     */
    public static int defaultPaxosFailureRate()
    {
        return 20; 
    }
    
    /**
     * @return the default timeout for PAXOS in milliseconds 
     */
    public static int defaultPaxosTimeout()
    {
        return 5000;
    }
}
