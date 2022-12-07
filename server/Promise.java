package server;

import java.io.Serializable;
import java.util.Objects;

/**
 * The PAXOS Promise.
 * @param <V> the value type
 */
public class Promise<V extends Serializable> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final long id;
    private final Proposal<V> proposal;
    
    public Promise(long id)
    {
        this(id, null);
    }
    
    public Promise(long id, Proposal<V> proposal)
    {
        this.id = id;
        this.proposal = proposal;
    }

    public long getId()
    {
        return id;
    }
    
    public Proposal<V> getProposal()
    {
        return proposal;
    }
    
    @Override
    public String toString()
    {
        return "Promise [id=" + id + ", proposal=" + proposal + "]";
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(id, proposal);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof Promise))
            return false;
        Promise<?> other = (Promise<?>) obj;
        return id == other.id && Objects.equals(proposal, other.proposal);
    }
}