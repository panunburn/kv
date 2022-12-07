package server;

import java.io.Serializable;
import java.util.*;
import common.Pair;

/**
 * The accepted PAXOS states for multiple rounds.
 *
 */
public class PaxosState<V extends Serializable> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private HashMap<Integer, Pair<Long, Proposal<V>>> state;
    
    public PaxosState()
    {
        state = new HashMap<>();
    }
    
    public Pair<Long, Proposal<V>> get(int round)
    {
        return state.get(round);
    }
    
    public void init(int round, long id)
    {
        state.put(round, new Pair<>(id, null));
    }
    
    public int getMaxRound()
    {
        return state.isEmpty() ? 0 : Collections.max(state.keySet());
    }
    
    public int getNextRound()
    {
        return getMaxRound() + 1;
    }
    
    @Override
    public String toString()
    {
        return "PaxosState [state=" + state + "]";
    }
}