package common;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 
 * These are the thread-safe logging interfaces. It logs regular messages to
 * STDOUT, warnings and errors to STDERR with the current time stamp (in
 * milliseconds precision).
 *
 */
public class Logger
{
    /**
     * 
     * @return the current time stamp in milliseconds precision.
     */
    public static String timeStamp()
    {
        return "[" + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date()) + "]";
    }

    public static PrintStream getLogStream()
    {
        return System.out;
    }

    public static PrintStream getWarningStream()
    {
        return System.err;
    }

    public static PrintStream getErrorStream()
    {
        return System.err;
    }

    /**
     * Log regular messages to STDOUT.
     * 
     * @param msg the message to be logged.
     */
    public static void log(String msg)
    {
        synchronized (Logger.getLogStream())
        {
            getLogStream().println(timeStamp() + " " + msg);
        }
    }

    /**
     * Log the input prompt to STDOUT.
     * 
     * @param msg the input prompt
     */
    public static void input(String msg)
    {
        synchronized (Logger.getLogStream())
        {
            getLogStream().print(timeStamp() + " " + msg);
        }
    }

    /**
     * Log warning messages to STDERR.
     * 
     * @param msg the message to be logged.
     */
    public static void warning(String msg)
    {
        synchronized (Logger.getWarningStream())
        {
            getWarningStream().println(timeStamp() + " Warning: " + msg);
        }
    }

    /**
     * Log an exception by logging its message. If the message is null, then the
     * actual exception will be logged.
     * 
     * @param e the exception to be logged
     */
    public static void warning(Exception e)
    {
        String m = e.getMessage();
        Logger.warning(m == null ? ("Caught unknown exception:" + e) : m);
    }

    /**
     * Log a message along with an exception. If the exception message is null, then
     * only the message will be logged.
     * 
     * @param msg the message to be logged
     * @param e   the exception to be logged
     */
    public static void warning(String msg, Exception e)
    {
        Logger.warning(Utils.combine(msg, e));
    }

    /**
     * Log error messages to STDERR.
     * 
     * @param msg the message to be logged
     */
    public static void error(String msg)
    {
        synchronized (Logger.getErrorStream())
        {
            getErrorStream().println(timeStamp() + " Error: " + msg);
        }
    }

    /**
     * Log an exception by logging its message. If the message is null, then the
     * actual exception will be logged.
     * 
     * @param e the exception to be logged
     */
    public static void error(Exception e)
    {
        String m = e.getMessage();
        Logger.error(m == null ? ("Caught unknown exception: " + e) : m);
    }

    /**
     * Log a message along with an exception. If the exception message is null, then
     * only the message will be logged.
     * 
     * @param msg the message to be logged
     * @param e   the exception to be logged
     */
    public static void error(String msg, Exception e)
    {
        Logger.error(Utils.combine(msg, e));
    }

    /**
     * Print a new line in STDOUT.
     */
    public static void newline()
    {
        synchronized (Logger.getLogStream())
        {
            getLogStream().println();
        }
    }

    /**
     * Log a debug message in STDERR.
     * 
     * @param msg the message to be logged
     */
    public static void debug(String msg)
    {
        if (Config.debugMode())
        {
            synchronized (Logger.getErrorStream())
            {
                getErrorStream().println(timeStamp() + " Debug: " + msg);
            }
        }
    }

    /**
     * Log a debug exception by logging its message. If the message is null, then
     * the actual exception will be logged.
     * 
     * @param e the exception to be logged
     */
    public static void debug(Exception e)
    {
        String m = e.getMessage();
        Logger.debug(m == null ? ("Caught unknown exception:" + e) : m);
    }

    /**
     * Log a debug message along with a debug exception. If the exception message is
     * null, then only the message will be logged.
     * 
     * @param msg the message to be logged
     * @param e   the exception to be logged
     */
    public static void debug(String msg, Exception e)
    {
        Logger.debug(Utils.combine(msg, e));
    }
}
