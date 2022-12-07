package common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Generic Pair data type.
 */
public class Pair<T extends Serializable, U extends Serializable> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private T t;
    private U u;
    
    public Pair(T t, U u)
    {
        this.t = t;
        this.u = u;
    }
    
    public T getFirst()
    {
        return t;
    }
    
    public void setFirst(T t)
    {
        this.t = t;
    }
    
    public U getSecond()
    {
        return u;
    }
    
    public void setSecond(U u)
    {
        this.u = u;
    }
    
    @Override
    public String toString()
    {
        return "(" + t + ", " + u + ")";
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(t, u);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof Pair))
            return false;
        Pair<?,?> other = (Pair<?,?>) obj;
        return Objects.equals(t, other.t) && Objects.equals(u, other.u);
    }
}
