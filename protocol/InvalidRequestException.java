package protocol;

/**
 * 
 * This exception will be thrown when the RequestParser fails to parse the
 * request.
 *
 */
@SuppressWarnings("serial")
public class InvalidRequestException extends Exception
{
    /**
     * Construct an InvalidRequestException with a specific message.
     * 
     * @param message the exception message
     */
    public InvalidRequestException(String message)
    {
        super(message);
    }
}
