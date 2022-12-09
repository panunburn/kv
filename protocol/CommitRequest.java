package protocol;

import java.util.Objects;

import transaction.TransactionId;

public class CommitRequest extends Request
{
    private static final long serialVersionUID = 1L;

    public TransactionId tid;
    
    public CommitRequest(TransactionId tid)
    {
        this.tid = tid;
    }
    
    @Override
    public <T, E extends Throwable> T accept(RequestVisitor<T, E> v) throws E
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return "COMMIT " + tid;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof CommitRequest))
            return false;
        CommitRequest other = (CommitRequest) obj;
        return Objects.equals(tid, other.tid);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tid);
    }
}
