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

    private HashMap<String, String> store;

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
        store = (HashMap<String, String>) Utils.restore(f);
        if (store != null)
        {
            if (!store.isEmpty())
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
            store = new HashMap<String, String>();
        }
    }

    /**
     * Construct the Store from another Store
     * 
     * @param store
     */
    public KVStore(KVStore store)
    {
        this.store = store.store;
    }

    @Override
    public String toString()
    {
        return "KV Store [store=" + store + "]";
    }

    public synchronized String get(String key)
    {
        return store.get(key);
    }

    public synchronized String put(String key, String val)
    {
        return store.put(key, val);
    }

    public synchronized String delete(String key)
    {
        return store.remove(key);
    }

    /**
     * Save the key value store by serializing its content based on the storePath.
     * 
     * @param path the path to save the key value store. 
     */
    public void save(String path)
    {
        Utils.save(store, new File(path));
    }
}
