package protocol;

/**
 * 
 * This class represents the DELETE requests with format "DELETE \<key\>".
 * 
 */
public class DeleteRequest extends Request
{
    private static final long serialVersionUID = 1L;

    public String key;
    
    /**
     * Create a DeleteRequest to delete a specific key.
     * @param key the argument to the request
     * @throws InvalidRequestException if the input key is not valid.
     */
    public DeleteRequest(String key) throws InvalidRequestException
    {
    	if (isValid(key))
        {
            throw new InvalidRequestException("Request key shouldn't be null or empty.");
        }
        else
        {
            this.key = key;
        }
    }

    /**
     * Apply a RequestVisitor.
     * @param <T> the return type
     * @param <E> the exception type
     * @return an object with type T.
     */
    @Override
    public <T, E extends Throwable> T accept(RequestVisitor<T, E> v) throws E
    {
        return v.visit(this);
    }

    /**
     * Convert the Request to String.
     */
    @Override
    public String toString()
    {
        return "DELETE " + key;
    }
}
