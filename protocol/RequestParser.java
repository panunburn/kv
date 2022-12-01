package protocol;

import java.util.StringTokenizer;

public class RequestParser
{
    /**
     * Parse the Request from the input string.
     * 
     * @param input the string to be parsed
     * @return A valid Request.
     * @throws InvalidRequestException if the parser fails to parse the string.
     */
    public static Request parse(String input) throws InvalidRequestException
    {
        StringTokenizer t = new StringTokenizer(input);

        if (t.countTokens() > 0)
        {
            String hd = t.nextToken();

            if (hd.equals("GET"))
            {
                if (t.countTokens() == 1)
                {
                    return new GetRequest(t.nextToken());
                }
                else
                {
                    throw new InvalidRequestException("GET expects one argument. Got: " + input + ".");
                }
            }
            else if (hd.equals("PUT"))
            {
                if (t.countTokens() == 2)
                {
                    return new PutRequest(t.nextToken(), t.nextToken());
                }
                else
                {
                    throw new InvalidRequestException("PUT expects two arguments. Got: " + input + ".");
                }
            }
            else if (hd.equals("DELETE"))
            {
                if (t.countTokens() == 1)
                {
                    return new DeleteRequest(t.nextToken());
                }
                else
                {
                    throw new InvalidRequestException("DELETE expects one argument. Got: " + input + ".");
                }
            }
            else if (hd.equals("PRINT"))
            {
                if (t.countTokens() == 0)
                {
                    return new PrintRequest();
                }
                else
                {
                    throw new InvalidRequestException("PRINT expects zero arguments. Got: " + input + ".");
                }
            }
            else
            {
                throw new InvalidRequestException(hd + " is an invalid request.");
            }
        }
        else
        {
            throw new InvalidRequestException("Input request is empty.");
        }
    }
}
