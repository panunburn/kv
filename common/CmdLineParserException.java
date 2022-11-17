package common;

/**
 * 
 * An exception of this type will be thrown when the CmdLineParser fails to handle an input.
 *
 */
@SuppressWarnings("serial")
public class CmdLineParserException extends Exception
{
	public CmdLineParserException(String message)
	{
		super(message);
	}
}
