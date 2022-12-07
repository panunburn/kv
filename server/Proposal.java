package server;

import java.io.Serializable;

/**
 * The PAXOS proposal type.
 * @param <V> the value type
 */
public class Proposal<V extends Serializable> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final long id;
    private final V value;
    
    public Proposal(long id, V value)
    {
        this.id = id;
        this.value = value;
    }

    public long getId()
    {
        return id;
    }
    
    public V getValue()
    {
        return value;
    }
    
    @Override
    public String toString()
    {
        return "Proposal [id=" + id + ", value=" + value + "]";
    }
}