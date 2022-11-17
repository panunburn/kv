package protocol;

/**
 * 
 * The Request visitor returning an object with type T.
 *
 * @param <T> the return type
 * @param <E> the exception type
 */
public interface RequestVisitor<T, E extends Throwable>
{
    public T visit(GetRequest r) throws E;

    public T visit(DeleteRequest deleteRequest) throws E;

    public T visit(PutRequest putRequest) throws E;

	public T visit(PrintRequest printRequest) throws E;
}
