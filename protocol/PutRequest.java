package protocol;

/**
 * 
 * This class represents the PUT request with format "PUT \<key\> \<value\>".
 *
 */
public class PutRequest extends Request
{
    private static final long serialVersionUID = 1L;

    public String key;
    public String val;

    /**
     * Construct a PutRequest with a key and val pair.
     * 
     * @param key the key
     * @param val the value
     * @throws InvalidRequestException if either key or val is not valid.
     */
    public PutRequest(String key, String val) throws InvalidRequestException
    {
        if (isValid(key))
        {
            throw new InvalidRequestException("Request key shouldn't be null or empty.");
        }
        else
        {
            this.key = key;
        }

        if (isValid(val))
        {
            throw new InvalidRequestException("Request value shouldn't be null or empty.");
        }
        else
        {
            this.val = val;
        }
    }

    /**
     * Apply a RequestVisitor.
     * 
     * @param <T> the return type
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
        return "PUT " + key + " " + val;
    }

}
