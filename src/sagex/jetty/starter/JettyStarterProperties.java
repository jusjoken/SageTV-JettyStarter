package sagex.jetty.starter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mortbay.log.Log;

// TODO resolve properties and detect circular references
public class JettyStarterProperties
{
    // in JettyStarter.properties, system properties are enclosed in $()
    private static final Pattern SYSTEM_PROPERTY_PATTERN = Pattern.compile("\\$\\(.*?\\)");
    public static final String JETTY_HOME_PROPERTY = "jetty.home";
    public static final String JETTY_CONFIG_FILES_PROPERTY = "jetty.configfiles";
    public static final String JETTY_LOGS_PROPERTY = "jetty.logs";
    public static final String JETTY_HOST_PROPERTY = "jetty.host";
    public static final String JETTY_PORT_PROPERTY = "jetty.port";
    public static final String JETTY_SSL_PORT_PROPERTY = "jetty.ssl.port";
    public static final String JETTY_SSL_KEYSTORE_PROPERTY = "jetty.ssl.keystore";
    public static final String JETTY_SSL_PASSWORD_PROPERTY = "jetty.ssl.password";
    public static final String JETTY_SSL_KEYPASSWORD_PROPERTY = "jetty.ssl.keypassword";
    public static final String JETTY_SSL_TRUSTSTORE_PROPERTY = "jetty.ssl.truststore";
    public static final String JETTY_SSL_TRUSTPASSWORD_PROPERTY = "jetty.ssl.trustpassword";
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

        // make sure to do jetty.home first because other properties may depend on it
        // and properties are not in the order they appear in the file
        String jettyHomeProperty = starterProperties.getProperty(JETTY_HOME_PROPERTY, "jetty");
        jettyHomeProperty = replaceSystemProperties(jettyHomeProperty);

        // get jetty home dir
        String jettyHomePath = jettyHomeProperty;
        File jettyHome = new File(jettyHomePath);
        if (!jettyHome.isAbsolute())
        {
            // if the property was not an absolute path, make it relative to SAGE_HOME
            jettyHome = new File(sageHome, jettyHomePath);
        }
        starterProperties.setProperty(JETTY_HOME_PROPERTY, jettyHome.getAbsolutePath());
        // set system property required by Jetty and other properties
        System.setProperty(JETTY_HOME_PROPERTY, jettyHome.getAbsolutePath());

        // replace system properties after jetty.home has been set
        replaceSystemProperties(starterProperties);

        // get jetty log dir
        String jettyLogsPath = starterProperties.getProperty(JETTY_LOGS_PROPERTY, "logs");
        File jettyLogs = new File(jettyLogsPath);
        if (!jettyLogs.isAbsolute())
        {
            // if the property was not an absolute path, make it relative to JETTY_HOME
            jettyLogs = new File(jettyHome, jettyLogsPath);
        }
        starterProperties.setProperty(JETTY_LOGS_PROPERTY, jettyLogs.getAbsolutePath());

        // get jetty keystore file
        String jettyKeystorePath = starterProperties.getProperty(JETTY_SSL_KEYSTORE_PROPERTY, "etc/keystore");
        File jettyKeystore = new File(jettyKeystorePath);
        if (!jettyKeystore.isAbsolute())
        {
            // if the property was not an absolute path, make it relative to JETTY_HOME
            jettyKeystore = new File(jettyHome, jettyKeystorePath);
        }
        starterProperties.setProperty(JETTY_SSL_KEYSTORE_PROPERTY, jettyKeystore.getAbsolutePath());

        // get jetty truststore file
        String jettyTruststorePath = starterProperties.getProperty(JETTY_SSL_TRUSTSTORE_PROPERTY, "etc/keystore");
        File jettyTruststore = new File(jettyTruststorePath);
        if (!jettyTruststore.isAbsolute())
        {
            // if the property was not an absolute path, make it relative to JETTY_HOME
            jettyTruststore = new File(jettyHome, jettyTruststorePath);
        }
        starterProperties.setProperty(JETTY_SSL_TRUSTSTORE_PROPERTY, jettyTruststore.getAbsolutePath());

        // set system properties for Jetty
        setSystemProperties(starterProperties);

        Log.debug("JettyStarter properties");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        starterProperties.list(pw);
        Log.debug(pw.toString());
    }

    public String getProperty(String key)
    {
        return (String) starterProperties.get(key);
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
                Log.info(e.getMessage());
                Log.ignore(e);
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
                        Log.info(e.getMessage());
                        Log.ignore(e);
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

    private void setSystemProperties(Properties starterProperties)
    {
        // do other properties
        Enumeration names = starterProperties.propertyNames();

        while (names.hasMoreElements())
        {
            String name = (String) names.nextElement();
            String value = starterProperties.getProperty(name);

            if ((value == null) || (value.trim().length() == 0))
            {
                continue;
            }

            // prevent overwriting of existing values
            if (System.getProperty(name) == null)
            {
                System.setProperty(name, value);
            }
        }
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
