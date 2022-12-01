/**
 * 
 */
package server;

import java.rmi.RemoteException;

/**
 * A remote exception for transaction abort.
 *
 */
public class TransactionAbortException extends RemoteException
{

	private static final long serialVersionUID = 1L;

	/**
	 * Construct a TransactionAbortException.
	 */
	public TransactionAbortException() {}
}
