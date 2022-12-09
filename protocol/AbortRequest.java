package protocol;

import java.util.Objects;
import transaction.TransactionId;

public class AbortRequest extends Request
{
    private static final long serialVersionUID = 1L;

    public TransactionId tid;
    
    public AbortRequest(TransactionId tid)
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
        return "ABORT " + tid;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof AbortRequest))
            return false;
        AbortRequest other = (AbortRequest) obj;
        return Objects.equals(tid, other.tid);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tid);
    }
}
