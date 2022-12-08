/**
 * 
 */
package client;

import java.io.*;
import java.rmi.*;

import server.*;
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
                server = CmdLineParser.parseEndPoint(Config.defaultServerHostName(), Config.defaultServerPortNumber());
            }
            else if (args.length == 1)
            {
                server = CmdLineParser.parseEndPoint(args[0], Config.defaultServerPortNumber());
            }
            else
            {
                throw new CmdLineParserException("Invalid client inputs. Usage: java client.Client <endpoint>?.");
            }

            System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(Config.defaultResponseTimeout()));            
            StoreService store = ServiceRegistry.connect(server, StoreService.class);
            Logger.log("Connected to the server.");

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true)
            {
                try
                {
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
                catch (TransactionAbortException e)
                {
                    Logger.warning("Request aborted.", e);
                }
                catch (RemoteException e)
                {
                    // TODO Automatically retry other available severs.
                    Logger.warning("Service exception.", e);
                    
                    while (true)
                    {
                        String input;
                        try
                        {
                            Logger.input("Please input another endpoint to reconnect: ");
                            input = br.readLine();
    
                            // if user inputs EOF, then quit
                            if (input == null)
                            {
                                Logger.newline();
                                Logger.log("Exiting ...");
                                return;
                            }
                            else
                            {
                                final EndPoint retry = CmdLineParser.parseEndPoint(input, Config.defaultServerPortNumber());
                                store = ServiceRegistry.connect(retry, StoreService.class);
                                Logger.log("Reconnected to the server.");
                                break;
                            }
                        }
                        catch (RemoteException | NotBoundException e1)
                        {
                            Logger.warning("Service unavailable.");
                        }
                        catch (CmdLineParserException e1)
                        {
                            Logger.warning("Invalid endpoint.");
                        }
                        catch (IOException e1)
                        {
                            Logger.error("Failed to read input query.", e);
                            System.exit(-1);
                        }
                    }
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
