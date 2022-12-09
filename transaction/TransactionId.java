package transaction;

import java.io.Serializable;
import java.util.Objects;

public class TransactionId implements Serializable, Comparable<TransactionId>
{
    private static final long serialVersionUID = 1L;
    private long id;
    
    TransactionId(long id)
    {
        this.id = id;
    }
    
    public long getId()
    {
        return id;
    }
    
    @Override
    public String toString()
    {
        return "{tid " + id + "}";
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof TransactionId))
            return false;
        TransactionId other = (TransactionId) obj;
        return id == other.id;
    }

    @Override
    public int compareTo(TransactionId o)
    {
        return Long.compare(this.id, o.id);
    }
}
