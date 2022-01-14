package sagex.jetty.starter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.rewrite.handler.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.PropertiesConfigurationManager;
import org.eclipse.jetty.deploy.bindings.DebugListenerBinding;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.handler.AbstractHandlerContainer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.xml.XmlConfiguration;

import sagex.api.PluginAPI;
import sagex.jetty.log.JettyStarterLogger;
import sagex.jetty.properties.JettyProperties;
import sagex.util.Log4jConfigurator;

/**
 * Provides access to the Jetty configuration objects (Server, Connector, etc)
 */
public class JettyInstance extends AbstractLifeCycle
{
    private static final JettyInstance instance = new JettyInstance();

    private static Class<? extends JettyProperties> jettyPropertiesClass = null;
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
    
    public String getServerVersion() {
    	return Server.class.getPackage().getImplementationVersion();
    }
    
    public List<String> getAppList() {
    	Log.getLog().info("getAppList: started");
    	Server oneServer = instance.getServers().get(0);
        ContextHandler consoleContext = null;

    	Handler[] handlerArray = (oneServer == null) ? null : oneServer.getChildHandlersByClass(ContextHandler.class);
        List<Handler> handlerList = new ArrayList<Handler>();
        
        if (handlerArray != null)
        {
           for (int i = 0; handlerArray != null && i < handlerArray.length; i++)
           {
               ContextHandler context = (ContextHandler)handlerArray[i];
               if (("/".equals(context.getContextPath())) ||
                   ("/apps".equals(context.getContextPath())))
               {
                   continue;
               }
               if ("/console".equals(context.getContextPath()))
               {
                   consoleContext = context;
                   continue;
               }
               Log.getLog().info("getAppList - adding context: " + context.getDisplayName() + " : " + context.getContextPath());
               handlerList.add(context);
           }

           // Sort the applications by name
           Collections.sort(handlerList, new Comparator<Object>()
           {
              public int compare(Object handler1, Object handler2)
              {
                 String handlerName1 = ((ContextHandler) handler1).getDisplayName();
                 if (handlerName1 == null)
                 {
                    handlerName1 = ((ContextHandler) handler1).getContextPath().substring(1);
                 }
                 String handlerName2 = ((ContextHandler) handler2).getDisplayName();
                 if (handlerName2 == null)
                 {
                    handlerName2 = ((ContextHandler) handler2).getContextPath().substring(1);
                 }
                 return handlerName1.compareToIgnoreCase(handlerName2);
              }
           });
        }
        //create a list that the jsp will output
        List<String> outList = new ArrayList<String>();
        
        if (handlerList.size() == 0)
        {
           outList.add("<li class=\"appinfo\">No web applications are installed.</li>");
        }
        else
        {
           Object[] installedPlugins = PluginAPI.GetInstalledPlugins();

           for (int i = 0; i < handlerList.size(); i++)
           {
              ContextHandler context = (ContextHandler) handlerList.get(i);

              Object pluginIdAttribute = context.getAttribute("pluginid");
              String pluginId = (pluginIdAttribute == null) ? null : pluginIdAttribute.toString();
              Object webpageAttribute = context.getAttribute("webpage");
              String webpage = (webpageAttribute == null) ? null : webpageAttribute.toString();
              Object contextVersion = context.getAttribute("version");
              String version = (contextVersion == null) ? null : contextVersion.toString();
              
              String favIconPath = getFavIconPath(context);

              //Default the version to the version attribute in the context or jetty-web.xml
              String installedPluginVersion = version;
              //Default the pluginName to the context DisplayName
              String installedPluginName = context.getDisplayName();
              if (pluginId != null)
              {
                  for (Object installedPlugin : installedPlugins)
                  {
                      //installedPluginName = PluginAPI.GetPluginName(installedPlugin);
                      String installedPluginId = PluginAPI.GetPluginIdentifier(installedPlugin);
                      
                      if (pluginId.equals(installedPluginId))
                      {
                          installedPluginName = PluginAPI.GetPluginName(installedPlugin);
                          installedPluginVersion = PluginAPI.GetPluginVersion(installedPlugin);
                          break;
                      }
                  }
              }

              if (context.isRunning())
              {
            	  outList.add("<li><a href=\"");
            	  outList.add(context.getContextPath());
                 if (context.getContextPath().length()>1 && context.getContextPath().endsWith("/"))
                 {
                	 outList.add("/");
                 }
                 if (webpage != null)
                 {
                	 outList.add(webpage);
                 }
                 outList.add("\">");
                 outList.add("<div class=\"appimg\"><img class=\"app\" src=\"" + favIconPath + "\"/></div>");
                 outList.add("<div class=\"appinfo\">");
                 if (installedPluginName != null)
                 {
                	 //outList.add(context.getDisplayName());
                	 outList.add(installedPluginName);
                 }
                 else
                 {
                	 outList.add(context.getContextPath().substring(1));
                 }
                 outList.add("<div class=\"appdetails\">");
                 if (installedPluginVersion != null)
                 {
                	 outList.add("Version " + installedPluginVersion + "\n");
                 }
                 outList.add("</div></div></a></li>\n");
              }
              else
              {
            	  outList.add("<li>");
            	  outList.add("<div class=\"appimg\"><img class=\"app\" src=\"" + favIconPath + "\"/></div>");
            	  outList.add("<div class=\"appinfo\">");
                 if (installedPluginName != null)
                 {
                	 outList.add(installedPluginName);
                 }
                 else
                 {
                	 outList.add(context.getContextPath().substring(1));
                 }
                 outList.add("<div class=\"appdetails\">");
                 if (context.isFailed())
                	 outList.add("<br> [failed]");
                 if (context.isStopped())
                	 outList.add("<br> [stopped]");
                 if (installedPluginVersion != null)
                 {
                	 outList.add("Version " + installedPluginVersion + "\n");
                 }
                 outList.add("</div></div></li>\n");
              }
           }
        }
		return outList;
    	
    }
    
    private static String getFavIconPath(ContextHandler context) {
        String defFavIconPath = "/favicon.ico";
        String favIconPath = defFavIconPath;
        Resource faviconResource = null;
        try {
			faviconResource = context.getResource("/favicon.ico");
	    	Log.getLog().debug("getAppList: favicon from:" + context.getContextPath() + " resource:" + faviconResource);
		  } catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  }
        java.io.File file = null;
        if (faviconResource != null)
        {
			try {
				file = faviconResource.getFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
           if (file != null)
           {
              if (file.exists())
              {
                 favIconPath = context.getContextPath() + "/favicon.ico";
              }
           }
        }
        if (defFavIconPath.equals(favIconPath)){
	    	Log.getLog().info("getAppList: faviconPath using default:" + favIconPath);
        }else {
	    	Log.getLog().info("getAppList: faviconPath returning: " + favIconPath);
        }
    	return favIconPath;
    }
    
    public static void listApps(Server oneServer) {
        //ContextHandler consoleContext = null;

    	Handler[] handlerArray = (oneServer == null) ? null : oneServer.getChildHandlersByClass(ContextHandler.class);
        //List<Handler> handlerList = new ArrayList<Handler>();

        if (handlerArray == null){
            Log.getLog().info("JettyInstance: listApps - handlerArray is NULL");
        }else{
           for (int i = 0; handlerArray != null && i < handlerArray.length; i++)
           {
               ContextHandler context = (ContextHandler)handlerArray[i];
               Log.getLog().info("JettyInstance: listApps - CONTEXT SESSION " + context.getDisplayName() + " found:" + context.getServer().getSessionIdManager().getSessionHandlers());
               if(JettyStarterLogger.isDebugEnabled()) {
             	   Log.getLog().debug("CONTEXT " + context.getDisplayName() + " found:" + context.dump());
               }
           }

        }
    }

    public static String[] getAllContexts() {
        Log.getLog().info("getAllContexts: started");
        if(instance.getServers().size()>0){
            Log.getLog().info("getAllContexts: server found - getting contexts");
            Server oneServer = instance.getServers().get(0);
            List<String> contextList = new ArrayList<String>();

            Handler[] handlerArray = (oneServer == null) ? null : oneServer.getChildHandlersByClass(ContextHandler.class);

            if (handlerArray == null){
                Log.getLog().info("JettyInstance: getAllContexts - handlerArray is NULL - no Contexts found");
                return new String[0];
            }else{
                for (int i = 0; handlerArray != null && i < handlerArray.length; i++)
                {
                    ContextHandler context = (ContextHandler)handlerArray[i];
                    Log.getLog().info("JettyInstance: getAllContexts - CONTEXT SESSION " + context.getDisplayName() + " found:" + context.getServer().getSessionIdManager().getSessionHandlers());
                    contextList.add(context.getContextPath());
                }
            }

            // Sort the contexts by name
            Collections.sort(contextList);

            return contextList.toArray(new String[0]);
        }else{
            Log.getLog().info("getAllContexts: server not available yet");
            return new String[0];
        }
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
        Log.getLog().info("***** configInternal fromFiles: configFiles:" + configFiles.toString() + " Properties:" + properties);
        XmlConfiguration last = null;
        List<Object> objects = new ArrayList<Object>();

        for (int i = 0; i < configFiles.length; i++)
        {
            Log.getLog().info("***** configInternal fromFiles: configFile:" + configFiles[i]);
            if (configFiles[i].toLowerCase().endsWith(".properties"))
            {
                Log.getLog().info("***** configInternal fromFiles: loading properties");
                properties.load(Resource.newResource(configFiles[i]).getInputStream());
            }
            else
            {
                Log.getLog().info("***** configInternal fromFiles: loading xml file URL:" + Resource.newResource(configFiles[i]).getURI().toURL());
                XmlConfiguration configuration = new XmlConfiguration(Resource.newResource(configFiles[i]).getURI().toURL());
                if (last != null)
                {
                    Log.getLog().info("***** configInternal fromFiles: xml: storing Map:" + last.getIdMap());
                    configuration.getIdMap().putAll(last.getIdMap());
                }
                if (properties.size() > 0)
                {
                    Log.getLog().info("***** configInternal fromFiles: xml: storing properties:" + properties.toString());
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
                    Log.getLog().info("***** configInternal fromFiles: xml: adding object:" + object);
                    // object will already exist if another with the same id was already parsed
                    objects.add(object);
                }
                last = configuration;
            }
        }

        Log.getLog().info("***** configInternal fromFiles: completed - calling configInternal with objects:" + objects);
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
        Log.getLog().info("***** configInternal fromObjects: configurationObjects:" + configurationObjects);

        //this.configurationObjects.clear();
        this.configurationObjects.addAll(configurationObjects);
        //this.servers.clear();
        //this.connectors.clear();

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
    	Log.getLog().info("JettyInstance: Starting Jetty");
        try
        {
            JettyProperties jettyProperties = jettyPropertiesClass.newInstance();
            
            // Set log level and Print properties if debug
            if (JettyStarterLogger.isDebugEnabled())
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
            	Log.getLog().info("JettyInstance: Creating Jetty Log folder:" + jettyLogsPath);
                jettyLogs.mkdirs();
            }
            
            //create the server rather than loading it from XML configs
            // - other XMLs can still be loaded below AFTER the server is created with the base config we know works for SageTV
            //Need to add the server to the object list so the below will start it
            int defaultPort = 8080;
            String sPort = jettyProperties.getProperty(JettyProperties.JETTY_PORT_PROPERTY);
            int port = defaultPort;
            try {
                port = Integer.parseInt(sPort);
    	        Log.getLog().info("JettyInstance: Setting http port to: " + sPort);
            }
            catch (NumberFormatException e)	{
            	   port = defaultPort;
        	       Log.getLog().info("JettyInstance: Setting http port to: " + sPort + " FAILED. Using default port:" + defaultPort);
            }
            
            int defaultSecurePort = 8443;
            int securePort = defaultSecurePort;
            String sSecurePort = jettyProperties.getProperty(JettyProperties.JETTY_SSL_PORT_PROPERTY);
            try {
                securePort = Integer.parseInt(sSecurePort);
    	        Log.getLog().info("JettyInstance: Setting https port to: " + sSecurePort);
            }
            catch (NumberFormatException e)	{
            	   securePort = defaultSecurePort;
        	       Log.getLog().info("JettyInstance: Setting https port to: " + sSecurePort + " FAILED. Using default port:" + defaultSecurePort);
            }

            Server server = null;
    		try {
    	        Log.getLog().info("JettyInstance: Calling createServer");
    			server = createServer(port, securePort, true);
    		} catch (Exception e1) {
    			// TODO Auto-generated catch block
    	        Log.getLog().info("JettyInstance: Create server failed: ");
    			e1.printStackTrace();
    		}

            servers.add(server);
            configurationObjects.addAll(servers);

            /*
            // get any extra jetty config files specified in the sage properties
            String configFiles = jettyProperties.getProperty(JettyProperties.JETTY_EXTRA_CONFIG_FILES_PROPERTY);

            // don't parse on spaces because SageTV is installed in C:\Program Files in Windows
            Log.getLog().info("JettyInstance: Determine what extra xml config files need to be processed");
            String[] configFileArgs = JettyProperties.parseConfigFilesSetting(configFiles);
            Log.getLog().info("JettyInstance: Returned from parseConfigFilesSetting - configFileArgs:" + configFileArgs);
            String msg = "Starting Jetty from the following configuration files:";
            for (int i = 0; i < configFileArgs.length; i++)
            {
                msg += " '" + configFileArgs[i] + "'";
            }
            Log.getLog().info(msg);

            //TODO: may want to make sure the base jetty.xml cannot be loaded ???
            Log.getLog().info("JettyInstance: Calling Configure internal");
            //TODO:: need to flesh out configinternal - skipped for now
            JettyInstance.getInstance().configureInternal(configFileArgs, jettyProperties.getProperties());
            Log.getLog().info("JettyInstance: Returned from Configure internal");
            */
        }
        catch (Throwable t)
        {
            Log.getLog().info("JettyInstance: Base server config failed");
            Log.getLog().info(t.getMessage(), t);
            Log.getLog().ignore(t);
        }

//        does not work.  WebAppContexts are loaded immediately        
//        configureTempDirs();
        
        // Start all Jetty LifeCycle objects
        Log.getLog().info("JettyInstance: Start all Jetty LifeCycle objects (" + configurationObjects.size() + ")");
        for (Object configurationObject : configurationObjects)
        {
            Log.getLog().info("JettyInstance: Start all Jetty LifeCycle objects - checking:" + configurationObject);
            if (configurationObject instanceof LifeCycle)
            {
                Log.getLog().info("JettyInstance: Start all Jetty LifeCycle objects - found a LifeCycle object");
                LifeCycle lc = (LifeCycle) configurationObject;
                if (!lc.isRunning())
                {
                    Log.getLog().info("JettyInstance: Start all Jetty LifeCycle objects - calling start on LC object");
                    lc.start();
                    if (configurationObject instanceof Server)
                    {
                        // add the server to the shutdown hook
                        Log.getLog().info("JettyInstance: Start all Jetty LifeCycle objects - adding server to shutdown hook");
                        ((Server) configurationObject).setStopAtShutdown(true);
                    }
                    Log.getLog().info("JettyInstance: Listing apps after server start");
                    listApps(((Server) configurationObject));

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
        Log.getLog().info("JettyInstance: Stopping all Jetty LifeCycle objects (" + configurationObjects.size() + ")");
        for (Object configurationObject : configurationObjects)
        {
            Log.getLog().info("JettyInstance: Stopping all Jetty LifeCycle objects - checking:" + configurationObject);
            if (configurationObject instanceof LifeCycle)
            {
                Log.getLog().info("JettyInstance: Stopping all Jetty LifeCycle objects - found a LifeCycle object");
                LifeCycle lc = (LifeCycle) configurationObject;
                if (lc.isRunning())
                {
                    Log.getLog().info("JettyInstance: Listing apps before server stop");
                    listApps(((Server) configurationObject));

                    Log.getLog().info("JettyInstance: Stopping all Jetty LifeCycle objects - calling stop on LC object");
                    lc.stop();
                    if (configurationObject instanceof Server)
                    {
                        // remove the server from the shutdown hook
                        Log.getLog().info("JettyInstance: Stopping all Jetty LifeCycle objects - removing server from shutdown hook");
                        ((Server) configurationObject).setStopAtShutdown(false);
                    }

                    Log.getLog().info("JettyInstance: Listing apps after server stop");
                    listApps(((Server) configurationObject));
                }
            }
        }
        this.configurationObjects.clear();
        this.servers.clear();
        this.connectors.clear();

    }

    public static Server createServer(int port, int securePort, boolean addDebugListener) throws Exception
    {
        Log.getLog().info("Create Server started: Port:" + port + " securePort:" + securePort);
        //set this to false to bypass login
        Boolean secureLogin = sagex.api.Configuration.GetProperty(JettyPlugin.PROP_NAME_SECURITY_ENABLE,true);

        // Note that if you set this to port 0 then a randomly available port
        // will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
   	
        Log.getLog().info("jetty.home:" + System.getProperty("jetty.home"));
        Log.getLog().info("jetty.base:" + System.getProperty("jetty.base"));
        String jettyHome = System.getProperty("jetty.home");
        String jettyBase = System.getProperty("jetty.base");

        // === jetty.xml ===
        // Setup Threadpool
        QueuedThreadPool threadPool = new QueuedThreadPool();
        //threadPool.setMaxThreads(500);
        threadPool.setMaxThreads(65536);

        // Server
        Server server = new Server(threadPool);
        Log.getLog().info("Setup Threadpool");

        // Scheduler
        server.addBean(new ScheduledExecutorScheduler("jetty.scheduler.name", false, -1));
        Log.getLog().info("Setup Scheduler");

        // HTTP Configuration
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(securePort);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(true);
        httpConfig.setSendDateHeader(false);
        httpConfig.setHeaderCacheSize(1024);
        httpConfig.setDelayDispatchUntilContent(true);
        httpConfig.setMaxErrorDispatches(10);
        httpConfig.setBlockingTimeout(-1);
        httpConfig.setPersistentConnectionsEnabled(true);
        httpConfig.setRelativeRedirectAllowed(false);
        // httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        Log.getLog().info("Setup HTTP");

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        // === SageTVRealm ===
        Log.getLog().info("Setup SageTVRealm - BASIC");
        HashLoginService login = new HashLoginService();
        login.setName("SageTVRealm");
        login.setConfig(jettyBase + "/etc/realm.properties");
        login.setHotReload(false);
        server.addBean(login);

        securityHandler.setLoginService(login);
        securityHandler.setHandler(server.getHandler());
        Log.getLog().info("Setup SageTVRealm - BASIC done");

        // === Rewrite Handler
        RewriteHandler rewrite = new RewriteHandler();
        rewrite.setHandler(server.getHandler());
        rewrite.addRule(new MsieSslRule());
        rewrite.addRule(new ValidUrlRule());
        RedirectPatternRule rprOldtoNew = new RedirectPatternRule();
        rprOldtoNew.setPattern("");
        String defaultContext = sagex.api.Configuration.GetProperty(JettyPlugin.PROP_NAME_ROOTCONTEXT,JettyPlugin.PROP_DEFAULT_ROOTCONTEXT);
        Log.getLog().info("Rewrite handler: setting default root context to:" + defaultContext);
        rprOldtoNew.setLocation(defaultContext);
        rewrite.addRule(rprOldtoNew);
        Log.getLog().info("Setup Rewrite handler");

        // Handler Structure
        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        DefaultHandler defaultHandler = new DefaultHandler();

        //GZIP Handler
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setIncludedMethods("POST", "GET");
        gzipHandler.setInflateBufferSize(2048);
        gzipHandler.setIncludedMimeTypes("text/html", "text/plain", "text/xml", "text/css", "application/javascript", "text/javascript","text/json","application/x-javascript","application/json","application/xml","application/xml+xhtml","image/svg+xml");
        gzipHandler.setHandler(contexts);
        Log.getLog().info("Setup gzipHandler");

        //Complete handler setup
        handlers.setHandlers(new Handler[]{securityHandler,rewrite, contexts, defaultHandler});
        server.setHandler(handlers);
        Log.getLog().info("Setup Handler");

        //TODO: not in example
        // === jetty-jmx.xml ===
        MBeanContainer mbContainer = new MBeanContainer(
            ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);
        Log.getLog().info("Setup jetty-jmx.xml");

        // === jetty-http.xml ===
        ServerConnector http = new ServerConnector(server,
            new HttpConnectionFactory(httpConfig));
        http.setPort(port);
        http.setIdleTimeout(30000);
        http.setReuseAddress(true);
        http.setAcceptedTcpNoDelay(true);
        http.setAcceptedReceiveBufferSize(-1);
        http.setAcceptedSendBufferSize(-1);
        http.getSelectorManager().setConnectTimeout(15000);
        server.addConnector(http);
        Log.getLog().info("Setup jetty-http.xml");

        // === jetty-deploy.xml ===
        DeploymentManager deployer = new DeploymentManager();
        if (addDebugListener)
        {
            DebugListener debug = new DebugListener(System.err, true, true, true);
            server.addBean(debug);
            deployer.addLifeCycleBinding(new DebugListenerBinding(debug));
        }
        deployer.setContexts(contexts);
        deployer.setContextAttribute(
            "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
        		".*\\/JARs\\/jetty-.*\\.jar|.*\\/JARs\\/org.apache.*\\.jar|.*\\/JARs\\/apache.*\\.jar|.*\\/JARs\\/javax[^/]*\\.jar");

        //".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$"
        //".*\/jetty-.*\.jar|.*\/org.apache.*\.jar|.*\/apache.*\.jar|.*\/javax[^/]*\.jar"
        //".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");
    	//".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/org.apache.taglibs.taglibs-standard-impl-.*\\.jar$");

        WebAppProvider webAppProvider = new WebAppProvider();
        webAppProvider.setMonitoredDirName(jettyBase + "/webapps");

        //use a different webdefault depending on if security is enabled
        if(secureLogin){
            //use webdefault that secures all apps
            webAppProvider.setDefaultsDescriptor(jettyHome + "/etc/webdefault.xml");
        }else{
            //use webdefault that allows all apps full access without login
            webAppProvider.setDefaultsDescriptor(jettyHome + "/etc/webdefault_ns.xml");
        }
        webAppProvider.setScanInterval(1);
        webAppProvider.setExtractWars(true);
        webAppProvider.setConfigurationManager(new PropertiesConfigurationManager());

        deployer.addAppProvider(webAppProvider);
        server.addBean(deployer);
        Log.getLog().info("Setup jetty-deploy.xml");

        // === setup jetty plus ==
        Configuration.ClassList classlist = Configuration.ClassList
            .setServerDefault(server);
        classlist.addAfter(
            "org.eclipse.jetty.webapp.FragmentConfiguration",
            "org.eclipse.jetty.plus.webapp.EnvConfiguration",
            "org.eclipse.jetty.plus.webapp.PlusConfiguration");

        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
            "org.eclipse.jetty.annotations.AnnotationConfiguration");
        Log.getLog().info("Setup jetty plus");

        
        /*
        //TODO: not in example
        // === jetty-stats.xml ===
        StatisticsHandler stats = new StatisticsHandler();
        stats.setHandler(server.getHandler());
        server.setHandler(stats);
        server.addBeanToAllConnectors(new ConnectionStatistics());
        Log.getLog().info("Setup jetty-stats.xml");
        */

        //TODO: not in example
        // === jetty-requestlog.xml ===
        AsyncRequestLogWriter logWriter = new AsyncRequestLogWriter(jettyBase + "/logs/yyyy_mm_dd.request.log");
        CustomRequestLog requestLog = new CustomRequestLog(logWriter, CustomRequestLog.EXTENDED_NCSA_FORMAT + " \"%C\"");
        logWriter.setFilenameDateFormat("yyyy_MM_dd");
        logWriter.setRetainDays(90);
        logWriter.setTimeZone("GMT");
        server.setRequestLog(requestLog);
        Log.getLog().info("Setup jetty-requestlog.xml");

        /*
        //TODO: not in example
        // === jetty-lowresources.xml ===
        LowResourceMonitor lowResourcesMonitor = new LowResourceMonitor(server);
        lowResourcesMonitor.setPeriod(1000);
        lowResourcesMonitor.setLowResourcesIdleTimeout(200);
        lowResourcesMonitor.setMonitorThreads(true);
        lowResourcesMonitor.setMaxMemory(0);
        lowResourcesMonitor.setMaxLowResourcesTime(5000);
        server.addBean(lowResourcesMonitor);
        Log.getLog().info("Setup jetty-lowresource.xml");
        */
        

        //TODO: can probably remove the below
        DefaultSessionIdManager session = new DefaultSessionIdManager(server);
        Log.getLog().info("Setup DefaultSessionManager");

        /*
        WebAppContext context = new WebAppContext();
        //context.setResourceBase( "src/main/webapp" );
        //context.setContextPath( "/" );
        context.setParentLoaderPriority( false );
        //context.setInitParams( Collections.singletonMap( "org.mortbay.jetty.servlet.Default.aliases", "true" ) );
        context.setHandler(server.getHandler());
        server.setHandler(context);
        */

        // apply extra XML (provided on command line)
        /*
        // get any extra jetty config files specified in the sage properties
        JettyProperties jettyProperties = jettyPropertiesClass.newInstance();
        String configFiles = jettyProperties.getProperty(JettyProperties.JETTY_EXTRA_CONFIG_FILES_PROPERTY);


        // don't parse on spaces because SageTV is installed in C:\Program Files in Windows
        Log.getLog().info("JettyInstance: Determine what extra xml config files need to be processed");
        String[] configFileArgs = JettyProperties.parseConfigFilesSetting(configFiles);
        Log.getLog().info("JettyInstance: Returned from parseConfigFilesSetting - configFileArgs:" + configFileArgs);

        String msg = "Starting Jetty from the following configuration files:";
        for (int i = 0; i < configFileArgs.length; i++)
        {
            msg += " '" + configFileArgs[i] + "'";
        }
        Log.getLog().info(msg);
        */

        Boolean configHTTPS = JettyProperties.getPropertyAsBoolean(JettyPlugin.PROP_NAME_SSL_ENABLE,false);
        if (configHTTPS)
        {
            Log.getLog().info("HTTPS/SSL configuration is ENABLED in plugin configuration: Loading from XML");
            //define the xml files that are needed to load for HTTPS and SSL config
            String[] configHTTPSXMLFiles = {"jetty/etc/add-https.xml"};

            // Map some well known objects via an id that can be referenced in the XMLs
            Map<String, Object> idMap = new HashMap<>();
            idMap.put("Server", server);
            idMap.put("httpConfig", httpConfig);
            idMap.put("httpConnector", http);
            idMap.put("Handlers", handlers);
            idMap.put("Contexts", contexts);
            //idMap.put("Context", context);
            idMap.put("DefaultHandler", defaultHandler);

            // Map some well known properties
            Map<String, String> globalProps = new HashMap<>();
            //URI resourcesUriBase = webRootUri.resolve("..");
            //System.err.println("ResourcesUriBase is " + resourcesUriBase);
            globalProps.put("resources.location", jettyBase);

            try{
                List<Object> configuredObjects = new ArrayList<>();
                XmlConfiguration lastConfig = null;
                for (int i = 0; i < configHTTPSXMLFiles.length; i++)
                {
                    String xml = configHTTPSXMLFiles[i];
                    Log.getLog().info("Loading HTTPS configuration from XML:" + xml);
                    URL url = new File(xml).toURI().toURL();
                    Log.getLog().info("Loading HTTPS configuration from url:" + url);
                    XmlConfiguration configuration = new XmlConfiguration(Resource.newResource(url));
                    Log.getLog().info("Loading HTTPS configuration = " + configuration);
                    if (lastConfig != null){
                        configuration.getIdMap().putAll(lastConfig.getIdMap());
                    }
                    configuration.getProperties().putAll(globalProps);
                    configuration.getIdMap().putAll(idMap);
                    idMap.putAll(configuration.getIdMap());
                    configuredObjects.add(configuration.configure());
                    lastConfig = configuration;
                }

                // Dump what was configured
                for (Object configuredObject : configuredObjects)
                {
                    Log.getLog().info("Configured: " + configuredObject.getClass().getName() + ":" + configuredObject.toString());
                }

            }catch (Exception e){
                Log.getLog().info("HTTPS/SSL configuration FAILED:" + e);
            }
        }else{
            Log.getLog().info("HTTPS/SSL configuration is DISABLED in plugin configuration");
        }

        
        //Extra options
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);
        server.setDumpAfterStart(true);
        server.setDumpBeforeStop(true);
        
        Log.getLog().info("Calling ListApps from createServer");
        listApps(server);
        
        return server;
    }

    /*
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

     */
    
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
