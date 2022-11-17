package server;

import java.io.*;
import java.util.HashMap;

import common.Logger;

/**
 * The key value store.
 */
public class KVStore implements Serializable
{
    private static final long serialVersionUID = 1L;
    
	private HashMap<String, String> map;

    /**
     * Create a key value Store by first trying to load a predefined store file specified by the searchPath parameter.
     * If the file cannot be loaded, then an empty store will be created.
     * @param path the predefined store file path. 
     */
	@SuppressWarnings("unchecked") // for unmarshalling the map
	public KVStore(String path) 
    {
        File f = new File(path);
        path = f.getAbsolutePath();
		Logger.log("Trying to find predefined key value store in " + path + ".");

    	if (f.isFile())
    	{
    		if (f.canRead())
    		{
	    		Logger.log("Predefined key value store found.");
	    		
				try 
				{
					try (FileInputStream fis = new FileInputStream(f);
					     ObjectInputStream ois = new ObjectInputStream(fis))
					{
						map = (HashMap<String, String>) ois.readObject();
			    		if (!map.isEmpty()) 
			    		{
				    		Logger.log("Predefined key value store read:\n" + this.toString());
			    		}
			    		else
			    		{
				    		Logger.log("Predefined key value store is empty.");
			    		}
						return;
					}
				} 
				catch (FileNotFoundException e) 
				{
		    		Logger.warning(path + " cannot be opened for reading.");
				} 
				catch (IOException e) 
				{
		    		Logger.warning(path + " cannot be read properly. ", e);
				} 
				catch (ClassNotFoundException e) 
				{
					Logger.warning(path + " is incompatible or corrupted.");				
				}
	    	}
    		else
	    	{
	    		Logger.warning(path + " is not readable.");
	    	}
    	}
    	else
    	{
    		Logger.warning(path + " doesn't exist or is not a file.");
    	}
		
        map = new HashMap<String, String>();
    }
    
	/**
	 * Construct the Store from another Store 
	 * @param store
	 */
	public KVStore(KVStore store)
	{
		this.map = store.map;
	}
	
    /**
     * Convert the Store to String.
     */
    @Override
    public synchronized String toString()
    {
        return map.toString();
    }
    
	public synchronized String get(String key)
	{
		return map.get(key);
	}

	public synchronized String put(String key, String val)
	{
		return map.put(key, val);
	}

	public synchronized String delete(String key)
	{
		return map.remove(key);
	}
	
    private void writeFile(File f) throws IOException 
    {
    	try (FileOutputStream fos = new FileOutputStream(f);
			 ObjectOutputStream oos = new ObjectOutputStream(fos))
    	{
			oos.writeObject(map);
			Logger.log("Data has been written to " + f.getAbsolutePath() + ".");
    	}
    }
    
    /**
     * Save the Store by serializing its content based on the storePath.
     * @param path the path to save the Store. If null, then no actual file on disk will be written.
     */
    public void save(String path)
    {    	    	
    	if (path != null)
    	{
	    	File f = new File(path);
	    	
	    	try 
	    	{
		    	if (f.exists())
		    	{
			    	if (f.isFile())
			    	{
			    		if (f.canWrite())
			    		{
							writeFile(f);
			    		}
			    		else
			    		{
			    			Logger.warning(path + " is not writable.");
			    		}
			    	}
			    	else
			    	{
			    		Logger.warning(path + " is not a file.");
			    	}  
		    	}
		    	else
		    	{
		    		writeFile(f);
		    	}
	    	}
	    	catch (IOException e)
	    	{
	    		Logger.warning("Failed to save store to " + path + ".", e);
	    	}
    	}
    }
}

