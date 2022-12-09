package protocol;

public class OpenRequest extends Request
{
    private static final long serialVersionUID = 1L;

    @Override
    public <T, E extends Throwable> T accept(RequestVisitor<T, E> v) throws E
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return "OPEN";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof OpenRequest))
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        return PrintRequest.class.hashCode();
    }

}
