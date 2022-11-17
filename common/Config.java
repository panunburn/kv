package common;

import java.rmi.registry.Registry;

/**
 * 
 * This class contains some global configurations for both the client and server.
 *
 */
public class Config
{
	/**
	 * 
	 * @return predefined key value store path.
	 */
    public static String defaultStorePath()
    {
        return "./store";
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
	
	public static String defaultStoreServiceName()
	{
		return "StoreService";
	}

	public static String defaultReplicaServiceName()
	{
		return "ReplicaService";
	}
	
	public static String defaultCoordinatorServiceName()
	{
		return "CoordinatorService";
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
	 * @return the default timeout used for sun.rmi.transport.tcp.responseTimeout in milliseconds.
	 */
	public static int defaultResponseTimeout()
	{
		return 3000;
	}
}
