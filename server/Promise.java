package server;

import java.io.Serializable;

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
}