package common;

import java.io.*;

public class Utils
{
    /**
     * Safe append two strings separated by a delimiter.
     * 
     * @param a     the first string
     * @param delim the delimiter, which should not be null
     * @param b     the second string
     * @return the combined message.
     */
    public static String combine(String a, String delim, String b)
    {
        if (a == null)
        {
            return (b == null) ? "" : b;
        }
        else
        {
            return (b == null) ? a : a + delim + b;
        }
    }

    /**
     * Combine a message with an exception message with a space in between.
     * 
     * @param m any message
     * @param e an exception where the message will be appended to m
     * @return the combined message.
     */
    public static String combine(String m, Exception e)
    {
        return Utils.combine(m, " ", e.getMessage());
    }

    public static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

    public static boolean isBlank(String s)
    {
        return isEmpty(s) || isEmpty(s.trim());
    }
    
    private static void writeFile(Object o, File f) throws IOException
    {
        try (FileOutputStream fos = new FileOutputStream(f); 
             ObjectOutputStream oos = new ObjectOutputStream(fos))
        {
            oos.writeObject(o);
            Logger.log("Data has been written to " + f.getAbsolutePath() + ".");
        }
    }

    /**
     * Save an object to a file.
     * @param o the object to be saved
     * @param f the file with the saved object
     */
    public static void save(Serializable o, File f)
    {
        String path = f.getAbsolutePath();
        try
        {
            if (f.exists())
            {
                if (f.isFile())
                {
                    if (f.canWrite())
                    {
                        writeFile(o, f);
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
                writeFile(o, f);
            }
        }
        catch (IOException e)
        {
            Logger.warning("Failed to save data to " + path + ".", e);
        }
    }

    /**
     * Restore a saved object from a file.
     * @param f the file with the saved object
     * @return the restored object.
     */
    public static Object restore(File f)
    {
        String path = f.getAbsolutePath();
        if (f.isFile())
        {
            if (f.canRead())
            {
                try
                {
                    try (FileInputStream fis = new FileInputStream(f); 
                         ObjectInputStream ois = new ObjectInputStream(fis))
                    {
                        return ois.readObject();
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
        
        return null;
    }
    
}
