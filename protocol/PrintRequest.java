package protocol;

/**
 * 
 * This class represents the PRINT request.
 *
 */
public class PrintRequest extends Request
{
    private static final long serialVersionUID = 1L;

    public PrintRequest()
    {
        super();
    }

    @Override
    public <T, E extends Throwable> T accept(RequestVisitor<T, E> v) throws E
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return "PRINT";
    }
}
