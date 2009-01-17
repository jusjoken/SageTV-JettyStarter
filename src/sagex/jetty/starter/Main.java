package sagex.jetty.starter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mortbay.log.Log;

// TODO listen on a port for restart - look at 'Jetty start' source code
public class Main implements Runnable
{
    private JettyStarterProperties starterProperties = null;

    /**
     * Called by SageTV to start the Jetty application server
     */
    public void run()
    {
        Thread.currentThread().setName("Jetty Starter");

        Log.info("Jetty Starter plugin version " + Version.CURRENT_VERSION);
        Log.info("Starting Jetty");
        restartJetty();

        //if (Boolean.valueOf(starterProperties.getProperty(JettyStarterProperties.JETTY_ALLOW_RESTART_PROPERTY)).booleanValue())
        //{
        //    listenForRestart();
        //}
    }

    private void restartJetty()
    {
        try
        {
            starterProperties = new JettyStarterProperties();

            // make sure the log dir exists
            // get jetty log dir
            String jettyLogsPath = starterProperties.getProperty(JettyStarterProperties.JETTY_LOGS_PROPERTY);
            File jettyLogs = new File(jettyLogsPath);
            if (!jettyLogs.exists())
            {
                jettyLogs.mkdirs();
            }

            // get jetty config files
            String configFiles = starterProperties.getProperty(JettyStarterProperties.JETTY_CONFIG_FILES_PROPERTY);

            // don't parse on spaces because SageTV is installed in C:\Program Files in Windows
            String[] configFileArgs = parseConfigFilesSetting(configFiles);
            String msg = "Starting Jetty from the following configuration files:";
            for (int i = 0; i < configFileArgs.length; i++)
            {
                msg += " '" + configFileArgs[i] + "'";
            }
            Log.debug(msg);

            JettyInstance.getInstance().configure(configFileArgs);
            JettyInstance.getInstance().start();
        }
        catch (Throwable t)
        {
            Log.info(t.getMessage());
            Log.ignore(t);
        }
    }

    /**
     * Listen for a restart command on a separate port.  This allows the Jetty configuration in
     * JettyInstance.properties to be modified and take effect without restarting SageTV.
     */
    /*
     * Don't allow restarts for now.  It won't be common anyway, once the user gets the server
     * running how they want it.  A method for clean shutdown needs to be implemented.
     * The function probably needs some other fixes, too.
     */
    /*private void listenForRestart()
    {
        ServerSocket serverSocket = null;

        try
        {
            String portString = starterProperties.getProperty(JettyStarterProperties.JETTY_RESTART_PORT_PROPERTY);
            int port = Integer.parseInt(portString);
            log(JettyStarterProperties.JETTY_RESTART_PORT_PROPERTY + " = " + port);
// TODO move inside while loop
            serverSocket = new ServerSocket(port);
 
            /*if (arguments.get(ARG_BINDADDRESS) != null)
                hostName = serverSocket.getInetAddress().getHostName();
            else
                hostName = InetAddress.getLocalHost().getHostName();*

            while (true)
            {

                Socket socket = null;
                BufferedReader reader = null;
                PrintStream printer = null;

                try
                {
                    log("Waiting to accept connection on port " + port);
                    socket = serverSocket.accept();
                    log("Connection from " + socket.getInetAddress() + ":" + socket.getPort() + " accepted");

                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    printer = new PrintStream(socket.getOutputStream());

                    String s = reader.readLine();

                    log("Command received from client: " + s);

                    log("Restarting Jetty");
                    restartJetty();

                    printer.println("<h3>Restarting Jetty Server</h3>");
                    printer.println("<h3>JettyStarter.properties</h3>");
                    printer.println("<pre>");
                    starterProperties.list(printer);
                    printer.println("</pre>");
                    printer.flush();
                }
                finally
                {
                    if (printer != null)
                    {
                        printer.close();
                    }
                    if (reader != null)
                    {
                        try
                        {
                            reader.close();
                        }
                        catch (IOException e)
                        {
                        }
                    }
                    if (socket != null)
                    {
                        try
                        {
                            socket.close();
                        }
                        catch (IOException e)
                        {
                        }
                    }
                }
            }
        }
        catch (Throwable e)
        {
            log(e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException e)
            {
            }
        }
    }*/

    /**
     * 
     */
    private static String[] parseConfigFilesSetting(String configFiles)
    {
        String UQ = "(?<!\\\\)\\\""; // unescaped quotes
        Pattern p = Pattern.compile(UQ + ".*?" + UQ);
        Matcher m = p.matcher(configFiles);
        List<String> configFileList = new ArrayList<String>();
        int previousEnd = 0;
        while (m.find())
        {
            if (m.start() > previousEnd)
            {
                // there is some text between the previous match and this match
                // (or before this match if it's the first match)
                // split it on spaces
                String textBetweenMatches = configFiles.substring(previousEnd, m.start()).trim();
                String[] textArray = textBetweenMatches.split(" ");
                for (String text : textArray)
                {
                    if (text.trim().length() > 0)
                    {
                        configFileList.add(text);
                    }
                }
            }
            String matchingText = configFiles.substring(m.start() + 1, m.end() - 1).trim();
            configFileList.add(matchingText);

            previousEnd = m.end();
        }

        if (configFiles.length() > previousEnd)
        {
            // text after the last match, split it on spaces
            String tailText = configFiles.substring(previousEnd, configFiles.length()).trim();
            String[] textArray = tailText.split(" ");
            for (String text : textArray)
            {
                if (text.trim().length() > 0)
                {
                    configFileList.add(text);
                }
            }
        }

        String[] configFileArray = new String[configFileList.size()];
        configFileArray = configFileList.toArray(configFileArray);
        return configFileArray;
    }
}
