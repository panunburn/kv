package protocol;

import java.io.Serializable;

/**
 * 
 * This class represents the server responses.
 *
 */
public abstract class Response implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Apply a response visitor.
     * 
     * @param <T> the return type of the Request visitor
     * @param v   a request visitor
     * @return an object with type T.
     */
    public abstract <T, E extends Throwable> T accept(ResponseVisitor<T, E> v) throws E;
    
    @Override
    public abstract String toString();
 
    @Override
    public abstract boolean equals(Object object);
    
    @Override
    public abstract int hashCode();
}
