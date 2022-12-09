package protocol;

import java.util.Objects;

import common.Utils;

/**
 * 
 * The response representing a server error.
 *
 */
public class ErrorResponse extends Response
{
    private static final long serialVersionUID = 1L;
    
    public String error;
    
    /**
     * Create an ErrorResponse from an error message after processing the request.
     * @param error the error message
     */
    public ErrorResponse(String error)
    {
        this(error, null);
    }

    /**
     * Create an ErrorResponse from an exception message after processing the request.
     * @param e an exception
     */
    public ErrorResponse(Exception e)
    {
    	this("", e);
    }
    
    /**
     * Create an ErrorResponse from an error message and an exception message after processing the request.
     * @param error the error message
     * @param e an exception
     */
    public ErrorResponse(String error, Exception e)
    {
        this.error = (Utils.combine(error, e));
    }
    
    /**
     * Convert to string representation.
     */
    @Override
    public String toString()
    {
        if (error.isEmpty())
        {
            return "Unknown server failure.";
        }
        else
        {
            return error;
        }
    }

    @Override
    public <T, E extends Throwable> T accept(ResponseVisitor<T, E> v) throws E
    {
        return v.visit(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof ErrorResponse))
            return false;
        ErrorResponse other = (ErrorResponse) obj;
        return Objects.equals(error, other.error);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(error);
    }
}
