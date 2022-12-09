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

    public T visit(DeleteRequest r) throws E;

    public T visit(PutRequest r) throws E;

    public T visit(PrintRequest r) throws E;
    
    public T visit(OpenRequest r) throws E;

    public T visit(CommitRequest r) throws E;

    public T visit(AbortRequest r) throws E;
}
