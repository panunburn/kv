package protocol;

import java.util.Objects;

import transaction.TransactionId;

public class TransactionResponse extends Response
{
    private static final long serialVersionUID = 1L;
    
    public TransactionId tid;
    
    public TransactionResponse(TransactionId tid)
    {
        this.tid = tid;
    }
    
    @Override
    public <T, E extends Throwable> T accept(ResponseVisitor<T, E> v) throws E
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return tid.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof TransactionResponse))
            return false;
        TransactionResponse other = (TransactionResponse) obj;
        return Objects.equals(tid, other.tid);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tid);
    }

}
