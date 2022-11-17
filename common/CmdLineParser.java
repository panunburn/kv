package common;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CmdLineParser
{
	/**
	 * Parse an IP address or a host name.
	 * @param arg the string to be parsed.
	 * @return a valid IP address
	 * @throws CmdLineParserException if arg is an invalid IP address or host name.
	 */
	public static InetAddress parseHostname(String arg) throws CmdLineParserException
	{
		try
		{
			return InetAddress.getByName(arg);
		}
		catch (UnknownHostException e)
		{
			throw new CmdLineParserException(arg + " is an invalid IP address or host name.");
		}
	}
	
	/**
	 * Parse a port number.
	 * If arg is not an positive integer, then defaultPort will be used.
	 * if arg is less than 1024, then a warning will be generated since [0, 1024) is the well-known port number range.
	 * @param arg the string to be parsed.
	 * @param defaultPort the default port number that will be used if arg is invalid
	 * @return a port number.
	 */
	public static int parsePort(String arg, int defaultPort)
    {
        try 
        {
            final int n = Integer.valueOf(arg);
            
            if (n < 0)
            {
                Logger.warning(arg + " is an invalid port number. Use " + defaultPort + " instead.");
                return defaultPort;
            }
            else if (n < 1024)
            {
                Logger.warning(arg + " might be reserved by system applications.");
                return n;
            }
            else
            {
                return n;
            }
        }
        catch (NumberFormatException e)
        {
            Logger.warning(arg + " is an invalid port number. Use " + defaultPort + " instead.");
            return defaultPort;
        }
    }
	
	public static EndPoint parseEndPoint(String arg, int defaultPort) throws CmdLineParserException
	{
		String[] r = arg.split(Config.defaultAddrDelim());
		if (r.length == 2)
		{
			return new EndPoint(parseHostname(r[0]), parsePort(r[1], defaultPort));
		}
		else if (arg.chars().allMatch((int c) -> { return Character.isDigit(c); }))
		{
			return new EndPoint(parseHostname(Config.defaultServerHostName()), parsePort(arg, defaultPort));
		}
		else
		{
			return new EndPoint(parseHostname(arg), defaultPort);
		}
	}
}
