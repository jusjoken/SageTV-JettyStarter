package sagex.jetty.log;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


public class JettyStarterLogger implements Logger
{
    private Logger logger = null;

    public static void init()
    {
        // Handle all logging for Jetty and Jetty Starter
        // EXCEPTION, DEBUG, VERBOSE, IGNORED
        System.setProperty("org.eclipse.jetty.util.log.class", JettyStarterLogger.class.getName());
        
        // Initialize Jetty's logging through Log's static initializer.  By setting the
        // property "org.eclipse.jetty.util.log.class" to this class, all logging will be routed
        // through this class.
        Log.getLog();
        
        // Attempt to load log4j logger.  If log4j is not in the classpath, log to stderr
        // via Jetty's implementation.
        // TODO Log4j, load config file
//        try
//        {
//            Class<Logger> loggerClass = Loader.loadClass(Logger.class, "sagex.jetty.log.Log4jLog");
//            logger = loggerClass.newInstance().getLogger("sagex.jetty");// = (Logger) loggerClass.newInstance();
//            Log.info("Jetty logging: Log4j");
//        }
//        catch (Throwable e)
//        {
//            logger = new StdErrLog();
//            Log.info("Jetty logging: Java StdErr");
//        }
    }
    
//    public JettyStarterLogger() throws Exception
//    {
//        this("org.mortbay.log");
//    }
//

//    public JettyStarterLogger(String name)
//    {
//        logger = org.slf4j.LoggerFactory.getLogger( name );
//    }

    public JettyStarterLogger()
    {
        logger = new SageStdOutLog();
    }

    @Override
    public void debug(String msg, Throwable ex)
    {
        if (msg == null) msg = "Null Message";
        logger.debug(msg, ex);
    }

    public void debug(String msg, Object arg1, Object arg2)
    {
        if (msg == null) msg = "Null Message";
        logger.debug(msg, arg1, arg2);
    }

    @Override
    public void debug(String msg, Object... objects)
    {
        if (msg == null) msg = "Null Message";
        logger.debug(msg, objects);
    }

    @Override
    public void debug(String msg, long l)
    {
        if (msg == null) msg = "Null Message";
        logger.debug(msg, l);
    }

    @Override
    public String getName()
    {
        return logger.getName();
    }

    @Override
    public void debug(Throwable throwable)
    {
        logger.debug(throwable);
    }

    @Override
    public Logger getLogger(String name)
    {
        return this;
    }

    public void info(String msg, Object arg1, Object arg2)
    {
        if (msg == null) msg = "Null Message";
        logger.info(msg, arg1, arg2);
    }

    @Override
    public void info(String msg, Object... objects)
    {
        if (msg == null) msg = "Null Message";
        logger.info(msg, objects);
    }

    @Override
    public void info(Throwable throwable)
    {
        logger.info(throwable);
    }

    @Override
    public void info(String msg, Throwable throwable)
    {
        if (msg == null) msg = "Null Message";
        logger.info(msg, throwable);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    @Override
    public void setDebugEnabled(boolean enabled)
    {
        logger.setDebugEnabled(enabled);
    }

    @Override
    public void warn(String msg, Throwable ex)
    {
        if (msg == null) msg = "Null Message";
        logger.warn(msg, ex);
    }

    public void warn(String msg, Object arg1, Object arg2)
    {
        if (msg == null) msg = "Null Message";
        logger.warn(msg, arg1, arg2);
    }

    @Override
    public void warn(String msg, Object... objects)
    {
        if (msg == null) msg = "Null Message";
        logger.warn(msg, objects);
    }

    @Override
    public void warn(Throwable throwable)
    {
        logger.warn(throwable);
    }

    @Override
    public void ignore(Throwable throwable)
    {
        logger.ignore(throwable);
    }
}
