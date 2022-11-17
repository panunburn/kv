/**
 * 
 */
package client;

import java.io.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;

import server.StoreService;
import common.*;
import protocol.*;

/**
 * The Client.
 */
class Client
{
	public static void main(String[] args)
	{
		try
		{
			final EndPoint server;
			
			if (args.length == 0)
			{
				server = new EndPoint(CmdLineParser.parseHostname("localhost"), Config.defaultServerPortNumber());
			}
			else if (args.length == 1)
			{
				server = new EndPoint(CmdLineParser.parseHostname("localhost"), CmdLineParser.parsePort(args[0], Config.defaultServerPortNumber()));
			}
			else if (args.length == 2)
			{
				server = new EndPoint(CmdLineParser.parseHostname(args[0]), CmdLineParser.parsePort(args[1], Config.defaultServerPortNumber()));
			}
			else
			{
                throw new CmdLineParserException("Invalid client inputs. Usage: java client.Client <ip address | host name>? <port number>?.");
			}
			
			StoreService store = (StoreService) LocateRegistry.getRegistry(server.getHost().getHostAddress(), server.getPort()).lookup(Config.defaultStoreServiceName());
			Logger.log("Connected to the server.");
			
	        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			while (true)
			{
				try 
    			{
    				// read input from STDIN
		            Logger.input("QUERY> ");
		            
	            	String input = br.readLine();
	            	
	            	// if user inputs EOF, then quit 
	            	if (input == null)
	            	{
	            		Logger.newline();
	            		Logger.log("Exiting ...");
	            		return;
	            	}
	            	else
	            	{
		            	Request request = RequestParser.parse(input);
		            	String value = store.process(request);
		            	request.accept(new RequestVisitor<Void, NoThrow>()
		            				   {
						            	    @Override
						            	    public Void visit(GetRequest r)
						            	    {
						            	        if (value == null)
						            	        {
						            	            Logger.warning("Key " + r.key + " doesn't exist.");
						            	        }
						            	        else 
						            	        {
						            	        	Logger.log("Got " + value + ".");
						            	        }
						            	    	
						            	    	return null;
						            	    }
				
						            	    @Override
						            	    public Void visit(DeleteRequest r)
						            	    {
						            	    	if (value == null)
						            	        {
						            	            Logger.warning("Key " + r.key + " doesn't exist.");
						            	        }
						            	        else
						            	        {
						            	            Logger.log("Deleted (" + r.key + ", " + value + ")");
						            	        }
						            	    	
						            	        return null;
						            	    }
				
						            	    @Override
						            	    public Void visit(PutRequest r)
						            	    {
						            	    	if (value == null)
						            	        {
						            	        	Logger.log("Inserted (" + r.key + ", " + r.val + ")");
						            	        }
						            	        else if (value.equals(r.val))
						            	        {
						            	        	Logger.warning("(" + r.key + ", " + r.val + ") already exists.");
						            	        }
						            	        else
						            	        {
						            	        	Logger.log("Replaced (" + r.key + ", " + value + ") with (" + r.key + ", " + r.val + ")");
						            	        }
				
						            	        return null;
						            	    }
				
						            		@Override
						            		public Void visit(PrintRequest r)
						            		{
						            			return null;
						            		}
		            				   });
	            	}
    			}
				catch (InvalidRequestException e)
				{
					Logger.warning(e);
				}
				catch (RemoteException e)
				{					
					Logger.warning("Service exception.", e);
				}
	            catch (IOException e)
				{
					Logger.error("Failed to read input query.", e);
					System.exit(-1);
				}
			}
		}
		catch (RemoteException | NotBoundException e)
		{
			Logger.error("Service unavailable. Please try again later or try a different server.");
			Logger.debug(e);
			System.exit(-1);
		}
		catch (CmdLineParserException e)
		{
			Logger.error(e);
			System.exit(-1);
		}
	}
}
