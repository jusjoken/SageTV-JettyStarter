package sagex.jetty.starter;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandlerContainer;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;

import sagex.jetty.properties.JettyProperties;

/**
 * Provides access to the Jetty configuration objects (Server, Connector, etc)
 */
public class JettyInstance extends AbstractLifeCycle
{
    private static final JettyInstance instance = new JettyInstance();

    private Class<? extends JettyProperties> jettyPropertiesClass = null;
    private List<Object> configurationObjects = new ArrayList<Object>();
    private List<Server> servers = new ArrayList<Server>();
    private List<Connector> connectors = new ArrayList<Connector>();

    /**
     * Restrict creation access to enforce singleton pattern
     */
    private JettyInstance() {}

    public static JettyInstance getInstance()
    {
        return instance;
    }
    
    public void setPropertyProvider(Class<? extends JettyProperties> jettyPropertiesClass)
    {
        this.jettyPropertiesClass = jettyPropertiesClass;
    }

    public List<Object> getConfigurationObjects()
    {
        return configurationObjects;
    }

    public List<Server> getServers()
    {
        return servers;
    }

    public List<Connector> getConnectors()
    {
        return connectors;
    }

    /**
     * 
     * @param args An array of file names that conform to the Jetty XmlConfiguration syntax.
     * @see XmlConfiguration.main(String[])
     */
//    public void configure(String[] configFiles) throws Exception
//    {
//        if (isRunning())
//        {
//            throw new IllegalStateException("Jetty cannot be reconfigured while running");
//        }
//
//        Properties properties = jettyPropertiesClass.newInstance().getProperties();
//        configureInternal(configFiles, properties);
//    }
    
    /**
     * Avoid reinitializing JettyProperties if they've been loaded by a public function in this class 
     * @param configFiles
     * @param properties
     * @throws Exception
     */
    @SuppressWarnings("unchecked") // XmlConfiguration.getIdMap() is not typed
    private void configureInternal(String[] configFiles, Properties properties) throws Exception
    {
        XmlConfiguration last = null;
        List<Object> objects = new ArrayList<Object>();

        for (int i = 0; i < configFiles.length; i++)
        {
            if (configFiles[i].toLowerCase().endsWith(".properties"))
            {
                properties.load(Resource.newResource(configFiles[i]).getInputStream());
            }
            else
            {
                XmlConfiguration configuration = new XmlConfiguration(Resource.newResource(configFiles[i]).getURL());
                if (last != null)
                {
                    configuration.getIdMap().putAll(last.getIdMap());
                }
                if (properties.size() > 0)
                {
                    // Properties come from JettyStarter.properties and other properties files 
                    // specified in the config files property in JettyStarter.properties.
                    // Usually it's just JettyStarter.properties.
                    // Jetty 9 doesn't have configuration.setProperties(properties) so we need to do it the long way.
                    Map<String, String> xmlConfigProperties = configuration.getProperties();
                    Set<Map.Entry<Object, Object>> propertySet = properties.entrySet();
                    for (Map.Entry<Object, Object> entry : propertySet)
                    {
                        xmlConfigProperties.put((String) entry.getKey(), (String) entry.getValue());
                    }
                }

                Object object = configuration.configure();
                if (!objects.contains(object))
                {
                    // object will already exist if another with the same id was already parsed
                    objects.add(object);
                }
                last = configuration;
            }
        }

        configureInternal(objects);
    }

//    public void configure(List<Object> configurationObjects)
//    {
//        if (isRunning())
//        {
//            throw new IllegalStateException("Jetty cannot be reconfigured while running");
//        }
//        
//        configureInternal(configurationObjects);
//    }
    
    private void configureInternal(List<Object> configurationObjects)
    {

        this.configurationObjects.clear();
        this.configurationObjects.addAll(configurationObjects);
        this.servers.clear();
        this.connectors.clear();

        for (Object configurationObject : configurationObjects)
        {
            if (configurationObject instanceof Server)
            {
                Server server = (Server) configurationObject;
                servers.add(server);
                connectors.addAll(Arrays.asList(server.getConnectors()));
            }
            if (configurationObject instanceof Connector)
            {
                Connector connector = (Connector) configurationObject;
                connectors.add(connector);
            }
        }
    }

    /**
     * Start all objects that implement {@link LifeCycle}
     */
    @Override
    protected void doStart() throws Exception
    {
        Log.getLog().info("Starting Jetty");
        try
        {
            JettyProperties jettyProperties = jettyPropertiesClass.newInstance();
            
            // Set up log levels
            String logLevel = jettyProperties.getProperty(JettyProperties.JETTY_LOG_LEVEL);
            if ((logLevel == null) || (logLevel.toLowerCase().equals("info")))
            {
                Log.getLog().info("Jetty Plugin log level: INFO");
                Log.getLog().setDebugEnabled(false);
            }
            else if (logLevel.toLowerCase().equals("debug"))
            {
                Log.getLog().info("Jetty Plugin log level: DEBUG");
                Log.getLog().setDebugEnabled(true);
            }
            else if (logLevel.toLowerCase().equals("warn"))
            {
                Log.getLog().info("Jetty Plugin log level: WARN");
                Log.getLog().setDebugEnabled(false);
            }
            else if (logLevel.toLowerCase().equals("ignore"))
            {
                Log.getLog().info("Jetty Plugin log level: IGNORE");
                Log.getLog().setDebugEnabled(false);
            }
            else
            {
                // the default is INFO
                Log.getLog().info("Jetty Plugin log level: INFO");
                Log.getLog().setDebugEnabled(false);
            }

            // Print properties
            if (Log.getLog().isDebugEnabled())
            {
                Log.getLog().debug("Jetty properties provided by class " + jettyPropertiesClass.getName());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                jettyProperties.getProperties().list(pw);
                Log.getLog().debug(sw.getBuffer().toString());
            }
            
            // make sure the log dir exists
            // get jetty log dir
            String jettyLogsPath = jettyProperties.getProperty(JettyProperties.JETTY_LOGS_PROPERTY);
            File jettyLogs = new File(jettyLogsPath);
            if (!jettyLogs.exists())
            {
                jettyLogs.mkdirs();
            }

            // get jetty config files
            String configFiles = jettyProperties.getProperty(JettyProperties.JETTY_CONFIG_FILES_PROPERTY);

            // don't parse on spaces because SageTV is installed in C:\Program Files in Windows
            String[] configFileArgs = JettyProperties.parseConfigFilesSetting(configFiles);
            String msg = "Starting Jetty from the following configuration files:";
            for (int i = 0; i < configFileArgs.length; i++)
            {
                msg += " '" + configFileArgs[i] + "'";
            }
            Log.getLog().debug(msg);

            JettyInstance.getInstance().configureInternal(configFileArgs, jettyProperties.getProperties());
        }
        catch (Throwable t)
        {
            Log.getLog().info(t.getMessage(), t);
            Log.getLog().ignore(t);
        }

//        does not work.  WebAppContexts are loaded immediately        
//        configureTempDirs();

        // Start all Jetty LifeCycle objects
        for (Object configurationObject : configurationObjects)
        {
            if (configurationObject instanceof LifeCycle)
            {
                LifeCycle lc = (LifeCycle) configurationObject;
                if (!lc.isRunning())
                {
                    lc.start();
                    if (configurationObject instanceof Server)
                    {
                        // add the server to the shutdown hook
                        ((Server) configurationObject).setStopAtShutdown(true);
                    }
                }
            }
        }
        
        printStartupErrors();
    }

    /**
     * Stop all objects that implement {@link LifeCycle}
     */
    @Override
    protected void doStop() throws Exception
    {
        Log.getLog().info("Stopping Jetty");
        for (Object configurationObject : configurationObjects)
        {
            if (configurationObject instanceof LifeCycle)
            {
                LifeCycle lc = (LifeCycle) configurationObject;
                if (lc.isRunning())
                {
                    lc.stop();
                    if (configurationObject instanceof Server)
                    {
                        // remove the server from the shutdown hook
                        ((Server) configurationObject).setStopAtShutdown(false);
                    }
                }
            }
        }
    }

    private void configureTempDirs()
    {
        Log.getLog().info("Number of configuration objects " + configurationObjects.size());
        for (Object object : configurationObjects)
        {
            Log.getLog().info("object class " + object.getClass().getName());
            if (object instanceof AbstractHandlerContainer)
            {
                Log.getLog().info("object is instance of AbstractHandlerContainer");
                configureTempDirs((AbstractHandlerContainer) object);
            }
            else
            {
                Log.getLog().info("object is not instance of AbstractHandlerContainer");
            }
        }
    }
    
    private void configureTempDirs(AbstractHandlerContainer handlers)
    {
        Log.getLog().info("Number of child handlers " + handlers.getChildHandlers().length);
        for (Handler handler : handlers.getChildHandlers())
        {
            Log.getLog().info("handler class " + handler.getClass().getName());
            if (handler instanceof WebAppContext)
            {
                WebAppContext context = (WebAppContext) handler;
                Log.getLog().info("context.getDisplayName() " + context.getDisplayName());
//                Log.getLog().info("context.getAttribute(ServletHandler.__J_S_CONTEXT_TEMPDIR) " + context.getAttribute(ServletHandler.__J_S_CONTEXT_TEMPDIR));
                Log.getLog().info("context.getBaseResource() " + context.getBaseResource());
                Log.getLog().info("context.getDefaultsDescriptor() " + context.getDefaultsDescriptor());
                Log.getLog().info("context.getInitParameter(\"useFileMappedBuffer\") " + context.getInitParameter("useFileMappedBuffer"));
                Log.getLog().info("context.getInitParameter(\"cacheControl\") " + context.getInitParameter("cacheControl"));
                Log.getLog().info("context.getInitParameter(\"cacheType\") " + context.getInitParameter("cacheType"));
                Log.getLog().info("context.getResourceBase() " + context.getResourceBase());
                Log.getLog().info("context.getTempDirectory() " + context.getTempDirectory());
                Log.getLog().info("context.getWar() " + context.getWar());

                if (WebAppContext.WEB_DEFAULTS_XML.equals(context.getDefaultsDescriptor()))
                {
                    context.setDefaultsDescriptor(new File(System.getProperty(JettyProperties.JETTY_HOME_PROPERTY), "etc/webdefault.xml").getAbsolutePath());
                    Log.getLog().info("context.getDefaultsDescriptor() new " + context.getDefaultsDescriptor());
                }
            }

            if (handler instanceof AbstractHandlerContainer)
            {
                Log.getLog().info("calling configureTempDirs recursively");
                configureTempDirs((AbstractHandlerContainer) handler);
            }
            else
            {
                Log.getLog().info("handler is not instance of AstractHandlerContainer");
            }
        }
    }
    
    /**
     * Print startup errors held in any WebAppContext object
     */
    private void printStartupErrors()
    {
        for (Object object : configurationObjects)
        {
            if (object instanceof AbstractHandlerContainer)
            {
                printStartupErrors((AbstractHandlerContainer) object);
            }
        }
    }

    private void printStartupErrors(AbstractHandlerContainer handlers)
    {
        for (Handler handler : handlers.getChildHandlers())
        {
            if (handler instanceof WebAppContext)
            {
                WebAppContext context = (WebAppContext) handler;
                Throwable t = context.getUnavailableException();
                if (t != null)
                {
                    String displayName = context.getDisplayName();
                    if (displayName == null)
                    {
                        displayName = context.getContextPath().substring(1);
                    }
                    Log.getLog().info("Error starting web application " + context.getDisplayName());
                    Log.getLog().info(t.getMessage(), t);
                    Log.getLog().ignore(t);
                }
            }
            if (handler instanceof AbstractHandlerContainer)
            {
                printStartupErrors((AbstractHandlerContainer) handler);
            }
        }
    }
}
