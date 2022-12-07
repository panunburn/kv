package protocol;

import java.io.Serializable;

/**
 * 
 * This is the base class for all the requests.
 *
 */
public abstract class Request implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Return true if a key or value is valid.
     * 
     * @param kv a key or value
     * @return true if kv is not null or empty.
     */
    protected static boolean isValid(String kv)
    {
        return kv == null || kv.isEmpty();
    }

    /**
     * Apply a request visitor.
     * 
     * @param <T> the return type of the Request visitor
     * @param v   a request visitor
     * @return an object with type T.
     */
    public abstract <T, E extends Throwable> T accept(RequestVisitor<T, E> v) throws E;
    
    public abstract String toString();
    
    public abstract boolean equals(Object object);
    
    public abstract int hashCode();
}
