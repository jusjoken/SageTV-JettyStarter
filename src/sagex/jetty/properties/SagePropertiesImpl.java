package sagex.jetty.properties;

import java.io.File;
import java.util.Properties;

import sagex.api.Configuration;

/**
 * Retrieves Jetty properties from SAGE_HOME/Sage.properties for SageTV V7 and later.
 */
public class SagePropertiesImpl extends JettyProperties
{
    private final static String JETTY_PROPERTY_PREFIX = "jetty";
    
    public SagePropertiesImpl()
    {
        // set up defaults if they don't exist (same defaults as the deprecated JettyStarter.properties file
        String jettyHome = getProperty(JettyProperties.JETTY_HOME_PROPERTY);
        if (jettyHome == null)
        {
            jettyHome = new File(System.getProperty("user.dir"), "jetty").getAbsolutePath();
            setProperty(JettyProperties.JETTY_HOME_PROPERTY, jettyHome);
        }

        String configFiles = getProperty(JettyProperties.JETTY_CONFIG_FILES_PROPERTY);
        if (configFiles == null)
        {
            configFiles = new File(jettyHome, "etc/jetty.xml").getAbsolutePath();
            setProperty(JettyProperties.JETTY_CONFIG_FILES_PROPERTY, "\"" + configFiles + "\"");
        }
        
        String logs = getProperty(JettyProperties.JETTY_LOGS_PROPERTY);
        if (logs == null)
        {
            logs = new File(jettyHome, "logs").getAbsolutePath();
            setProperty(JettyProperties.JETTY_LOGS_PROPERTY, logs);
        }
    }

    public Properties getProperties()
    {
        Properties jettyProperties = new Properties();

        String[] jettyPropertyArray = Configuration.GetSubpropertiesThatAreLeaves(JETTY_PROPERTY_PREFIX);
        for (String propertyName : jettyPropertyArray)
        {
            String propertyValue = getProperty(propertyName);
            if (propertyValue != null)
            {
                jettyProperties.put(propertyName, propertyValue);
            }
        }
        return jettyProperties;
    }
    
    public String getProperty(String propertyName)
    {
        return Configuration.GetProperty(JETTY_PROPERTY_PREFIX + "/" + propertyName, null);
    }

    private void setProperty(String propertyName, String value)
    {
        Configuration.SetProperty(JETTY_PROPERTY_PREFIX + "/" + propertyName, value);
    }
}
