package server;

import java.io.*;
import java.util.HashMap;
import common.*;

/**
 * The key value store.
 */
public class KVStore implements Serializable
{
    private static final long serialVersionUID = 1L;

    private HashMap<String, String> map;

    /**
     * Create a key value Store by first trying to load a predefined store file
     * specified by the searchPath parameter. If the file cannot be loaded, then an
     * empty store will be created.
     * 
     * @param path the predefined store file path.
     */
    @SuppressWarnings("unchecked")
    public KVStore(String path)
    {
        File f = new File(path);
        map = (HashMap<String, String>) Utils.restore(f);
        if (map != null)
        {
            if (!map.isEmpty())
            {
                Logger.log("Predefined key value store read:\n" + this.toString());
            }
            else
            {
                Logger.log("Predefined key value store is empty.");
            }
        }
        else
        {
            Logger.warning("Failed to restore the key value store from " + path + ".");
            map = new HashMap<String, String>();
        }
    }

    /**
     * Construct the Store from another Store
     * 
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

    /**
     * Save the key value store by serializing its content based on the storePath.
     * 
     * @param path the path to save the key value store. 
     */
    public void save(String path)
    {
        Utils.save(map, new File(path));
    }
}
