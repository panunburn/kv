/**
 * 
 */
package client;

import java.io.*;
import java.rmi.*;

import server.*;
import transaction.TransactionId;
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

            TransactionId tid = null;
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
                        Response response = store.process(request, tid);
                        tid = response.accept(new ResponseVisitor<TransactionId, NoThrow>()
                                              {
                                                    @Override
                                                    public TransactionId visit(TransactionResponse r) throws NoThrow
                                                    {
                                                        if (request instanceof OpenRequest)
                                                        {
                                                            Logger.log("Transaction " + r.tid + " opened.");
                                                            return r.tid;
                                                        }
                                                        else if (request instanceof CommitRequest)
                                                        {
                                                            Logger.log("Transaction " + r.tid + " committed.");
                                                        }
                                                        else
                                                        {
                                                            Logger.error("Unexpected response type for request " + request + ".");
                                                        }
        
                                                        return null;
                                                    }
        
                                                    @Override
                                                    public TransactionId visit(ErrorResponse r) throws NoThrow
                                                    {
                                                        Logger.warning(r.toString());
                                                        return null;
                                                    }
        
                                                    @Override
                                                    public TransactionId visit(ProcessResponse r) throws NoThrow
                                                    {
                                                        if (request instanceof GetRequest)
                                                        {
                                                            GetRequest get = (GetRequest) request;
                                                            if (r.value == null)
                                                            {
                                                                Logger.warning("Key " + get.key + " doesn't exist.");
                                                            }
                                                            else
                                                            {
                                                                Logger.log("Got " + r.value + ".");
                                                            }
                                                        }
                                                        else if (request instanceof DeleteRequest)
                                                        {
                                                            DeleteRequest del = (DeleteRequest) request;
                                                            if (r.value == null)
                                                            {
                                                                Logger.warning("Key " + del.key + " doesn't exist.");
                                                            }
                                                            else
                                                            {
                                                                Logger.log("Deleted (" + del.key + ", " + r.value + ")");
                                                            }
                                                        }
                                                        else if (request instanceof PutRequest)
                                                        {
                                                            PutRequest put = (PutRequest) request;
                                                            if (r.value == null)
                                                            {
                                                                Logger.log("Inserted (" + put.key + ", " + put.val + ")");
                                                            }
                                                            else if (r.value.equals(put.val))
                                                            {
                                                                Logger.warning("(" + put.key + ", " + put.val + ") already exists.");
                                                            }
                                                            else
                                                            {
                                                                Logger.log("Replaced (" + put.key + ", " + r.value + ") with (" + put.key + ", " + put.val + ")");
                                                            }
                                                        }
                                                        else
                                                        {
                                                            Logger.error("Unexpected response type for request " + request + ".");
                                                        }
                                                        
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
                    tid = null;
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
