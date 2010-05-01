package sagex.jetty.starter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mortbay.log.Log;

import sage.SageTVPlugin;
import sage.SageTVPluginRegistry;
import sagex.api.Configuration;
import sagex.jetty.log.JettyStarterLogger;
import sagex.jetty.properties.JettyProperties;
import sagex.jetty.properties.SagePropertiesImpl;
import sagex.jetty.properties.persistence.SslEnabledPersistence;
import sagex.jetty.properties.persistence.UserRealmPersistence;
import sagex.plugin.AbstractPlugin;
import sagex.plugin.ButtonClickHandler;

public class JettyPlugin extends AbstractPlugin
{
    public static final String PROP_NAME_RESTART    = "jetty/restart";
    public static final String PROP_NAME_USER       = "jetty/user";
    public static final String PROP_NAME_PASSWORD   = "jetty/password";
    public static final String PROP_NAME_HTTP_PORT  = "jetty/" + JettyProperties.JETTY_PORT_PROPERTY;
    public static final String PROP_NAME_SSL_ENABLE = "jetty/ssl.enable";
    public static final String PROP_NAME_HTTPS_PORT = "jetty/" + JettyProperties.JETTY_SSL_PORT_PROPERTY;
    public static final String PROP_NAME_LOG_LEVEL  = "jetty/" + JettyProperties.JETTY_LOG_LEVEL;
    // TODO more PROP_NAMEs
    
    public static final String UPNP_CHOICE_MANUAL_CONFIGURATION = "Manual Configuration";
    public static final String UPNP_CHOICE_AUTO_CONFIGURATION   = "Automatic Configuration";

    public static final String LOGLEVEL_CHOICE_INFO    = "INFO";
    public static final String LOGLEVEL_CHOICE_DEBUG   = "DEBUG";
    public static final String LOGLEVEL_CHOICE_VERBOSE = "VERBOSE";
    //    private boolean restartNeeded = false;// *property name

    static
    {
        // Handle all logging for Jetty and Jetty Starter
        JettyStarterLogger.init();

        Log.info("Jetty Starter plugin version " + JettyPlugin.class.getPackage().getImplementationVersion());
    }
    
    public JettyPlugin(SageTVPluginRegistry registry)
    {
        super(registry);
        
        addProperty(SageTVPlugin.CONFIG_BUTTON,
                    PROP_NAME_RESTART,
                    "Restart Web Server",
                    "Restart Web Server",
                    "Click to restart Jetty after changing its settings.");

        addProperty(SageTVPlugin.CONFIG_TEXT,
                    PROP_NAME_USER,
                    "sage",
                    "User",
                    "The user name for logging in to web applications.")
                    .setPersistence(new UserRealmPersistence());
    
        addProperty(SageTVPlugin.CONFIG_TEXT,
                    PROP_NAME_PASSWORD,
                    "frey",
                    "Password",
                    "The user's password.")
                    .setPersistence(new UserRealmPersistence());

        addProperty(SageTVPlugin.CONFIG_INTEGER,
                    PROP_NAME_HTTP_PORT,
                    "8080",
                    "HTTP Port",
                    "The web server's HTTP port.");// TODO .setValidationProvider(new JettyPortValidator());

        addProperty(SageTVPlugin.CONFIG_BOOL,
                    PROP_NAME_SSL_ENABLE,
                    "false",
                    "Enable SSL",
                    "Enable use of an encrypted HTTPS connection.  Keystore must be set up manually.")
                    .setPersistence(new SslEnabledPersistence());

        addProperty(SageTVPlugin.CONFIG_INTEGER,
                    PROP_NAME_HTTPS_PORT,
                    "8443",
                    "HTTPS Port",
                    "The web server's HTTPS port.")
                    .setVisibleOnSetting(this, PROP_NAME_SSL_ENABLE);// TODO .setValidationProvider(new JettyPortValidator());

//        addProperty(SageTVPlugin.CONFIG_DIRECTORY,
//                    "jetty/createkeystore",///*"jetty/https/port",*/ JettyStarterProperties.JETTY_SSL_PORT_PROPERTY,
//                    "Create...",
//                    "Create Keystore",
//                    "Create a keystore and SSL certificate.")
//                    .setVisibleOnSetting(this, PROP_NAME_SSL_ENABLE);// TODO .setValidationProvider(new JettyPortValidator());
//
//        addProperty(SageTVPlugin.CONFIG_FILE,
//                    "jetty/selectkeystore",///*"jetty/https/port",*/ JettyStarterProperties.JETTY_SSL_PORT_PROPERTY,
//                    "Select...",
//                    "Select Keystore",
//                    "Select an existing keystore.")
//                    .setVisibleOnSetting(this, PROP_NAME_SSL_ENABLE);// TODO .setValidationProvider(new JettyPortValidator());

//        PluginProperty upnpProperty =
//            addProperty(SageTVPlugin.CONFIG_CHOICE,
//                    "jetty/upnp",
//                    "Manual Configuration",
//                    "Internet Connection",
//                    "Choose whether to automatically or manually configure your router or firewall to allow access to SageTV's web server from outside your home network.",
//                    new String[] {"Automatic UPnP Configuration", "Manual Configuration"});
//        upnpProperty.setPersistence(new UpnpPersistence(this, upnpProperty));
//        upnpProperty.setVisibility(new UpnpPropertyVisibility(this, upnpProperty));
//
//        addProperty(SageTVPlugin.CONFIG_TEXT,
//                    "jetty/upnp",
//                    "Unavailable",
//                    "Internet Connection",
//                    "Choose whether to automatically or manually configure your router/firewall to allow access to SageTV's web server from outside your home network.")
//                    .setVisibility(new UPnPVisibility()).setPersistence(new UPnPPersistence());
        addProperty(SageTVPlugin.CONFIG_CHOICE,
                    PROP_NAME_LOG_LEVEL,
                    LOGLEVEL_CHOICE_INFO,
                    "Log Level",
                    "Choose the level of detail for Jetty messages written to SageTV's log file.",
                    new String[] {LOGLEVEL_CHOICE_INFO, LOGLEVEL_CHOICE_DEBUG, LOGLEVEL_CHOICE_VERBOSE});
    }

    /**
     * Called by SageTV 7 or later to start the Jetty application server
     */
    public void start()
    {
        super.start();

        try
        {
            cleanRunnableClasses();
            migrateProperties();
            JettyInstance.getInstance().setPropertyProvider(SagePropertiesImpl.class);
            JettyInstance.getInstance().start();
        }
        catch (Exception e)
        {
            Log.info(e.getMessage());
            Log.ignore(e);
        }
    }

    /**
     * Called by SageTV 7 or later to stop the Jetty application server
     */
    public void stop()
    {
        super.stop();
        
        try
        {
            JettyInstance.getInstance().stop();
        }
        catch (Exception e)
        {
            Log.info(e.getMessage());
            Log.ignore(e);
        }
    }

    public void setConfigValue(String setting, String value)
    {
        super.setConfigValue(setting, value);

//        if ("UPNP".equals(setting))
//        {
//            try
//            {
//                if ("Automatic UPnP Configuration".equals(value))
//                {
//                    UpnpProperties.enableUpnp(getConfigValue(JettyStarterProperties.JETTY_PORT_PROPERTY), getConfigValue(JettyStarterProperties.JETTY_SSL_PORT_PROPERTY));
////                    JettyStarterProperties.writeProperty(setting, "AUTOMATIC");
//                }
//                else
//                {
//                    UpnpProperties.disableUpnp();
////                    JettyStarterProperties.writeProperty(setting, "MANUAL");
//                }
//            }
//            catch (InvocationTargetException e)
//            {
//                Log.info(e.getMessage());
//                Log.ignore(e);
//            }
//        }
//        else
        {
//            JettyStarterProperties.writeProperty(setting, value);
//            scheduleRestart();
        }
    }

    /**
     * Strip sagex.jetty.starter.Main from load_at_startup_runnable_classes in Sage.properties.
     * The SageTV 7 plugin architecture will start Jetty via the start() method on the SageTVPlugin interface.
     */
    private void cleanRunnableClasses()
    {
        String prop = Configuration.GetProperty("load_at_startup_runnable_classes", "");
        String[] propArray = prop.split(";");
        
        if (propArray != null)
        {
            List<String> propList = Arrays.asList(propArray);
            // the List implementation returned by Arrays.asList does not support remove.  Create a list that does.
            propList = new ArrayList<String>(propList);
            StringBuilder sb = new StringBuilder();

            // remove all occurrences of Jetty's runnable
            while (propList.remove("sagex.jetty.starter.Main"));

            if (propList.size() > 0)
            {
                sb.append(propList.get(0));
            }
            for (int i = 1; i < propList.size(); i++)
            {
                sb.append(";").append(propList.get(i));
            }
            prop = sb.toString();

            Configuration.SetProperty("load_at_startup_runnable_classes", prop);
        }
    }

    /**
     * Migrate properties from JettyStarter.properties to Sage.properties when upgrading to version 7.
     * Remove the JettyStarter.properties file.
     */
    private void migrateProperties()
    {
        String configFiles = Configuration.GetProperty("jetty/" + JettyProperties.JETTY_CONFIG_FILES_PROPERTY, null);
        if (configFiles != null)
        {
            // properties have already been copied from JettyStarter.properties to Sage.properties
            return;
        }
    
        Log.info("Moving Jetty plugin properties from JettyStarter.properties to Sage.properties");
        File propertiesFile = JettyStarterProperties.getPropertiesFile();
        if (propertiesFile.exists())
        {
            JettyStarterProperties properties = new JettyStarterProperties();
            // TODO delete file
            migrateProperty(properties, JettyProperties.JETTY_HOME_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_CONFIG_FILES_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_LOGS_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_HOST_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_PORT_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_SSL_PORT_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_SSL_KEYSTORE_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_SSL_PASSWORD_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_SSL_KEYPASSWORD_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_SSL_TRUSTSTORE_PROPERTY);
            migrateProperty(properties, JettyProperties.JETTY_SSL_TRUSTPASSWORD_PROPERTY);
            
//          propertiesFile.remove();
        }
    }
    
    private void migrateProperty(JettyStarterProperties properties, String propertyName)
    {
        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue != null)
        {
            Configuration.SetProperty("jetty/" + propertyName, propertyValue);
        }
    }

//    @ConfigValueChangeHandler()
//    public void onValueChanged(String setting)
//    {
//    }

    @ButtonClickHandler(value=PROP_NAME_RESTART)
    public void onRestartButtonClicked(String setting, String value)
    {
        stop();
        start();
    }
}
