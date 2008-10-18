package net.sf.sageplugins.jetty.starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.resource.Resource;
import org.mortbay.xml.XmlConfiguration;

/**
 * Provides access to the Jetty configuration objects (Server, Connector, etc)
 */
public class JettyInstance extends AbstractLifeCycle
{
    private static final JettyInstance instance = new JettyInstance();

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
    @SuppressWarnings("unchecked") // XmlConfiguration.getIdMap() is not typed
    public void configure(String[] configFiles) throws Exception
    {
        if (isRunning())
        {
            throw new IllegalStateException("Jetty cannot be reconfigured while running");
        }

        Properties properties = new Properties();
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
                    configuration.setProperties(properties);
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

        configure(objects);
    }

    public void configure(List<Object> configurationObjects)
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
    }

    /**
     * Stop all objects that implement {@link LifeCycle}
     */
    @Override
    protected void doStop() throws Exception
    {
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
}
