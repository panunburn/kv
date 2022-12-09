package protocol;

import java.util.Objects;

/**
 * 
 * The response representing the value obtained from the server, 
 * which might be null depending on the request type.
 *
 */
public class ProcessResponse extends Response
{
    private static final long serialVersionUID = 1L;
    
    public String value;
    
    public ProcessResponse()
    {
        this(null);
    }
    
    public ProcessResponse(String value)
    {
        this.value = value;
    }
    
    @Override
    public <T, E extends Throwable> T accept(ResponseVisitor<T, E> v) throws E
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof ProcessResponse))
            return false;
        ProcessResponse other = (ProcessResponse) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(value);
    }
}
