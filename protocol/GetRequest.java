package protocol;

import java.util.Objects;

/**
 * 
 * This class represents the GET request with format "GET \<key\>".
 *
 */
public class GetRequest extends Request
{
    private static final long serialVersionUID = 1L;

    public String key;

    /**
     * Create a DeleteRequest with a key.
     * 
     * @param key the key
     * @throws InvalidRequestException if the input key is not valid.
     */
    public GetRequest(String key) throws InvalidRequestException
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
     * 
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
        return "GET " + key;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(key);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GetRequest other = (GetRequest) obj;
        return Objects.equals(key, other.key);
    }
}
