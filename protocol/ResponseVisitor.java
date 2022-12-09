package protocol;

/**
 * 
 * The Response visitor returning an object with type T.
 *
 * @param <T> the return type
 * @param <E> the exception type
 */
public interface ResponseVisitor<T, E extends Throwable>
{
    public T visit(TransactionResponse r) throws E;

    public T visit(ErrorResponse r) throws E;

    public T visit(ProcessResponse r) throws E;
}
