package sagex.jetty.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jetty.util.log.Log;

import sagex.jetty.starter.JettyPlugin;
import sagex.jetty.util.PrefItem;
import sagex.util.Log4jConfigurator;
import sagex.util.Log4jConfigurator.LogStruct;


public class JettyStarterLogger
{
    private static String logID = "jettystarter";
    private static String logAppenderName = "JETTY";
    private static String defaultLevel = JettyPlugin.LOGLEVEL_CHOICE_INFO;
    private static ArrayList<String> levelKeys = new ArrayList<String>();
    private static ArrayList<PrefItem> jettyLogProps = new ArrayList<PrefItem>();

    /*
     * Initialize and configure the Log4j system based on the log4j.properties
     */
    public static void init()
    {
        // EXCEPTION, DEBUG, VERBOSE, IGNORED

    	//set the classes that we need to set the levels on
    	levelKeys.add("log4j.logger.org.eclipse.jetty");
    	levelKeys.add("log4j.logger.sagex.jetty");
    	
    	//Use sagex Log4j configurator
    	Log4jConfigurator.configureQuietly(logID, JettyPlugin.class.getClassLoader());
        jettyLogProps = getLog4jProperties(logID);

        //TODO:: remove the below
        for (PrefItem pi : jettyLogProps) {
            Log.getLog().info("Jetty Log item: Key:" + pi.getKey() + " Value:" + pi.getValue());
        }      
        
        //TODO:: remove the below
        /* output all the logger details to validate loggers
        Enumeration<Category> loggers = LogManager.getCurrentLoggers();
        for(; loggers.hasMoreElements(); ) {
        	Category logger = loggers.nextElement();
        	Log.getLog().info( "Logger:" + logger.getName() ); 
        	Log.getLog().info( " - Logger additivity:" + logger.getAdditivity() ); 
        	Log.getLog().info( " - Logger appenders:" + logger.getAllAppenders().toString() ); 
        	Log.getLog().info( " - Logger parent:" + logger.getParent() ); 
        	Log.getLog().info( " - Logger level:" + logger.getLevel() ); 
        	Log.getLog().info( " - Logger repo:" + logger.getLoggerRepository().toString() ); 
        }
        */
        
    }
    
    private static LogStruct getLog(String log) {
        Log4jConfigurator.LogStruct[] logs =  Log4jConfigurator.getConfiguredLogs();
        if (logs==null) {
            return null;
        }
        for (int i=0;i<logs.length;i++) {
            if (log.equals(logs[i].id)) return logs[i];
        }
        return null;
    }
    
    private static ArrayList<PrefItem> getLog4jProperties(String logId) {
        ArrayList<PrefItem> items = new ArrayList<PrefItem>();
        LogStruct log = getLog(logId);
        for (Map.Entry<Object, Object> me: log.properties.entrySet()) {
            PrefItem pi = new PrefItem();
            pi.setKey(String.valueOf(me.getKey()));
            pi.setValue(String.valueOf(me.getValue()));
            items.add(pi);
        }
        Collections.sort(items, new Comparator<PrefItem>() {
            public int compare(PrefItem p1, PrefItem p2) {
                return p1.getKey().compareTo(p2.getKey());
            }
        });
        return items;
    }
    
    public static void setLogLevel(String newLevel) {
    	if(getLogLevel().equalsIgnoreCase(newLevel)) {
    		Log.getLog().info("setLog called but log already at new value:" + newLevel);
    	}else {
    		//set new level and save the configuration
            Properties props = new Properties();
            for (PrefItem p: jettyLogProps) {
            	Boolean found = false;
            	for(String levelKey: levelKeys) {
            		if(p.getKey().equals(levelKey)){
            			found = true;
            		}
            	}
            	if(found) {
            		Log.getLog().info("setLog changing '" + p.getKey() + "' to '" + newLevel + "'");
                    props.setProperty(p.getKey(), newLevel + ", " + logAppenderName);
            	}else {
                    props.setProperty(p.getKey(), p.getValue());
            	}
            }
    		Log.getLog().info("setLog reconfiguring log");
            Log4jConfigurator.reconfigure(logID, props);
            jettyLogProps = getLog4jProperties(logID);
    	}
    }
    
    /*
     * Get the current log level - if not set then return default
     */
    public static String getLogLevel() {
    	for(PrefItem item:jettyLogProps) {
    		if(item.getKey().equals(levelKeys.get(0))){
    			String level = item.getValue(); 
    			String parts[] = level.split(",");
    			return parts[0];
    		}
    	}
    	return defaultLevel;
    }
    
    public static Boolean isDebugEnabled() {
    	if (getLogLevel().contains(JettyPlugin.LOGLEVEL_CHOICE_DEBUG)) {
    		return true;
    	}
    	return false;
    }
    
    public JettyStarterLogger()
    {
    }

}
