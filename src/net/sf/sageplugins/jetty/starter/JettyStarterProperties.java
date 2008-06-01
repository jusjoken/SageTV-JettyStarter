package net.sf.sageplugins.jetty.starter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO resolve properties and detect circular references
public class JettyStarterProperties
{
    // in JettyStarter.properties, system properties are enclosed in $()
    private static final Pattern SYSTEM_PROPERTY_PATTERN = Pattern.compile("\\$\\(.*?\\)");
    public static final String JETTY_HOME_PROPERTY = "jetty.home";
    public static final String JETTY_CONFIG_FILES_PROPERTY = "jetty.configfiles";
    public static final String JETTY_LOGS_PROPERTY = "jetty.logs";
    //public static final String JETTY_ALLOW_RESTART_PROPERTY = "jetty.restart.allow";
    //public static final String JETTY_RESTART_PORT_PROPERTY = "jetty.restart.port";
    //public static final String JETTY_RESTART_BIND_ADDRESS_PROPERTY = "jetty.restart.bindaddress";

    private File jettyPropertiesFile = null;
    private Properties starterProperties = new Properties();

    public JettyStarterProperties()
    {
        // get sage home dir
        File sageHome = new File(System.getProperty("user.dir"));
        jettyPropertiesFile = new File(sageHome, "JettyStarter.properties");

        // load Jetty starter properties from a file and replace system properties in values
        loadProperties();
        System.out.println("JettyStarter properties before system property replace");
        starterProperties.list(System.out);

        // make sure to do jetty.home first because other properties may depend on it
        // and properties are not in the order they appear in the file
        String jettyHomeProperty = starterProperties.getProperty(JETTY_HOME_PROPERTY, "jetty");
        jettyHomeProperty = replaceSystemProperties(jettyHomeProperty);
        starterProperties.setProperty(JETTY_HOME_PROPERTY, jettyHomeProperty);

        // get jetty home dir
        String jettyHomePath = starterProperties.getProperty(JETTY_HOME_PROPERTY);
        System.out.println("jetty.home property: " + jettyHomePath);
        File jettyHome = new File(jettyHomePath);
        if (!jettyHome.isAbsolute())
        {
            System.out.println("jetty.home property is not absolute");
            // if the property was not an absolute path, make it relative to SAGE_HOME
            jettyHome = new File(sageHome, jettyHomePath);
            System.out.println("jetty.home property: " + jettyHome.getAbsolutePath());
        }
        // set system property required by Jetty
        System.setProperty(JETTY_HOME_PROPERTY, jettyHome.getAbsolutePath());

        // replace system properties after jetty.home has been set
        replaceSystemProperties(starterProperties);
        System.out.println("JettyStarter properties after system property replace");
        starterProperties.list(System.out);

        // get jetty log dir
        String jettyLogsPath = starterProperties.getProperty(JETTY_LOGS_PROPERTY, "logs");
        System.out.println("jetty.logs property: " + jettyLogsPath);
        File jettyLogs = new File(jettyLogsPath);
        if (!jettyLogs.isAbsolute())
        {
            // if the property was not an absolute path, make it relative to JETTY_HOME
            System.out.println("jetty.logs property is not absolute");
            jettyLogs = new File(jettyHome, jettyLogsPath);
            System.out.println("jetty.logs property: " + jettyLogs.getAbsolutePath());
        }
        // set system property required by Jetty
        System.setProperty(JETTY_LOGS_PROPERTY, jettyLogs.getAbsolutePath());
    }

    public String getProperty(String key)
    {
        if (key.equals(JETTY_HOME_PROPERTY))
        {
            return System.getProperty(JETTY_HOME_PROPERTY);
        }
        else
        {
            return (String) starterProperties.get(key);
        }
        //return (String) tempMap.get(key);
    }

    public void list(PrintStream out)
    {
        starterProperties.list(out);
    }

    private void loadProperties()
    {
        if (jettyPropertiesFile.exists())
        {
            FileInputStream fis = null;
            try
            {
                fis = new FileInputStream(jettyPropertiesFile);
                starterProperties.load(fis);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (fis != null)
                {
                    try
                    {
                        fis.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Replaces Java system properties in the values of the properties object with their actual values.
     * @param properties
     */
    private void replaceSystemProperties(Properties properties)
    {
        // do other properties
        Enumeration names = properties.propertyNames();

        while (names.hasMoreElements())
        {
            String name = (String) names.nextElement();
            String value = properties.getProperty(name);

            if (value == null)
            {
                continue;
            }

            value = replaceSystemProperties(value);
            properties.setProperty(name, value);
        }
    }

    /**
     * Replaces Java system properties in the values of the properties object with their actual values.
     * @param properties
     */
    private String replaceSystemProperties(String value)
    {
        StringBuffer sb = new StringBuffer();
        int lastEnd = 0; // copy characters between matches
        Matcher m = SYSTEM_PROPERTY_PATTERN.matcher(value);

        while (m.find())
        {
            if (lastEnd < m.start())
            {
                // copy characters between matches
                sb.append(value.substring(lastEnd, m.start()));
            }
            String systemProperty = System.getProperty(value.substring(m.start() + 2, m.end() - 1));
            sb.append(systemProperty);
            // record the end of the match
            lastEnd = m.end();
        }
        sb.append(value.substring(lastEnd));

        return sb.toString();
    }

    public static void main(String[] args)
    {
        StringBuffer sb = new StringBuffer();
        int lastEnd = 0;
        //String property = "$(jetty.home)/etc/jetty.xml $(jetty.home)/etc/jetty-sage.xml";
        //String property = "$(user.dir)/etc/jetty.xml $(user.dir)/etc/jetty-sage.xml";
        //String property = "abc $(user.dir)/etc/jetty.xml $(user.dir)/etc/jetty-sage.xml";
        //String property = "abc $(user.dir)$(user.dir)/etc/jetty.xml $(user.dir)/etc/jetty-sage.xml";
        //String property = "$(user.dir)";
        String property = "a$(user.dir)";
        Matcher m = SYSTEM_PROPERTY_PATTERN.matcher(property);
        while (m.find())
        {
            System.out.println(m.start());
            System.out.println(m.end());
            if (lastEnd < m.start())
            {
                // copy characters between matches
                sb.append(property.substring(lastEnd, m.start()));
            }
            String systemProperty = System.getProperty(property.substring(m.start() + 2, m.end() - 1));
            sb.append(systemProperty);
            lastEnd = m.end();
        }
        sb.append(property.substring(lastEnd));
        System.out.println(property);
        System.out.println(sb.toString());
    }
}
