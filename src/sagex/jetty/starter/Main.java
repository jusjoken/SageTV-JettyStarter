package sagex.jetty.starter;

import org.eclipse.jetty.util.log.Log;

import sagex.jetty.log.JettyStarterLogger;
import sagex.jetty.properties.JettyStarterPropertiesImpl;

//@deprecated as no longer needed in SageTV7 or higher
@Deprecated
public class Main implements Runnable
{
    static
    {
        // Handle all logging for Jetty and Jetty Starter
        JettyStarterLogger.init();

        Log.getLog().info("Jetty Starter plugin version " + Main.class.getPackage().getImplementationVersion());
    }
    
    /**
     * Called by SageTV 6 or earlier to start the Jetty application server
     */
    public void run()
    {
        Thread.currentThread().setName("Jetty Starter");

        try
        {
            JettyInstance.getInstance().setPropertyProvider(JettyStarterPropertiesImpl.class);
            JettyInstance.getInstance().start();
        }
        catch (Exception e)
        {
            Log.getLog().info(e.getMessage(), e);
            Log.getLog().ignore(e);
        }
    }
}
