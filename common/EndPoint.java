package common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

/**
 * An endpoint location is of a valid pair IP address and port number. 
 */
public class EndPoint implements Serializable
{
	private static final long serialVersionUID = 1L;
	private final InetAddress host;
	private final int port;
	
	public EndPoint(InetAddress host, int port)
	{
		this.host = host;
		this.port = port;
	}
	
	public InetAddress getHost()
	{
		return host;
	}
	
	public int getPort()
	{
		return port;
	}
	
	@Override
	public String toString()
	{
		return host + Config.defaultAddrDelim() + port;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(host, port);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EndPoint other = (EndPoint) obj;
		return Objects.equals(host, other.host) && port == other.port;
	}
}