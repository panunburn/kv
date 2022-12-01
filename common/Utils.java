package common;

public class Utils
{
    /**
     * Safe append two strings separated by a delimiter.
     * 
     * @param a     the first string
     * @param delim the delimiter, which should not be null
     * @param b     the second string
     * @return the combined message.
     */
    public static String combine(String a, String delim, String b)
    {
        if (a == null)
        {
            return (b == null) ? "" : b;
        }
        else
        {
            return (b == null) ? a : a + delim + b;
        }
    }

    /**
     * Combine a message with an exception message with a space in between.
     * 
     * @param m any message
     * @param e an exception where the message will be appended to m
     * @return the combined message.
     */
    public static String combine(String m, Exception e)
    {
        return Utils.combine(m, " ", e.getMessage());
    }

    public static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

    public static boolean isBlank(String s)
    {
        return isEmpty(s) || isEmpty(s.trim());
    }
}
