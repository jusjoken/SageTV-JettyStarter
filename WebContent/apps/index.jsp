<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//WAPFORUM//DTD XHTML Mobile 1.0//EN" "http://www.wapforum.org/DTD/xhtml-mobile10.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.List" %>
<%@ page import="org.mortbay.jetty.Handler" %>
<%@ page import="org.mortbay.jetty.handler.ContextHandler" %>
<%@ page import="org.mortbay.jetty.Server" %>
<%@ page import="org.mortbay.resource.Resource" %>
<%@page import="sagex.api.PluginAPI"%>
<%@ page import="sagex.jetty.starter.JettyInstance" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
   <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
   <title>SageTV Web Applications</title>
   <link rel="stylesheet" type="text/css" href="apps.css"/>
   <link rel="Shortcut Icon" href="favicon.ico" type="image/x-icon"/>
   <%-- iphone headers --%>
   <meta name="viewport" content="user-scalable=no, width=device-width" /> <%-- iDevices --%>
   <%--link rel="apple-touch-icon" href="${cp}/images/SageIcon64.png" /> <!-- iPhone home screen icon -->
   <link rel="apple-touch-startup-image" href="${cp}/images/SageStartupLogo256-mgopenmodata13.png" /> <!-- iPhone startup graphic -->
   <meta name="apple-mobile-web-app-capable" content="yes" />
   <meta name="apple-mobile-web-app-status-bar-style" content="black" /--%>
</head>
<body>
   <div class="content">
      <div class="header">
         <div><img src="SageLogo256small.png" alt="SageTV" title="SageTV"/></div>
         <div>Web Applications</div>
      </div>

      <%
      JettyInstance instance = JettyInstance.getInstance();
      Server server = instance.getServers().get(0);

      Handler[] handlerArray = (server == null) ? null : server.getChildHandlersByClass(ContextHandler.class);
      List<Handler> handlers = new ArrayList<Handler>();
      for (Handler handler : handlerArray)
      {
         handlers.add(handler);
      }

      if (handlers != null)
      {
         // Sort the applications by name
         Collections.sort(handlers, new Comparator<Object>()
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
      %>

      <ul>
      <%
      for (int i = 0; handlers != null && i < handlers.size(); i++)
      {
         ContextHandler context = (ContextHandler)handlers.get(i);
         if (("/".equals(context.getContextPath())) ||
             ("/apps".equals(context.getContextPath())))
         {
             continue;
         }

         Resource faviconResource = context.getResource("/favicon.ico");
         String favIconPath = "/apps/favicon.ico";
         if (faviconResource != null)
         {
            java.io.File file = faviconResource.getFile();
            if (file != null)
            {
                if (file.exists())
                {
                    favIconPath = context.getContextPath() + "/favicon.ico";
                }
            }
         }

         if (context.isRunning())
         {
            out.write("<li><a href=\"");
            out.write(context.getContextPath());
            if (context.getContextPath().length()>1 && context.getContextPath().endsWith("/"))
            {
               out.write("/");
            }
            if (context.getAttribute("webpage") != null)
            {
               out.write(context.getAttribute("webpage").toString());
            }
            out.write("\">");
            out.write("<div class=\"appimg\"><img class=\"app\" src=\"" + favIconPath + "\"/></div>");
            out.write("<div class=\"appinfo\">");
            if (context.getDisplayName() != null)
            {
               out.write(context.getDisplayName());
            }
            else
            {
               out.write(context.getContextPath().substring(1));
            }
            out.write("<div class=\"appdetails\">");
            if (context.getAttribute("pluginid") != null)
            {
               Object plugin = PluginAPI.GetAvailablePluginForID(context.getAttribute("pluginid").toString());
               out.write("Version " + PluginAPI.GetPluginVersion(plugin));
            }
            out.write("</div></div></a></li>\n");
         }
         else
         {
            out.write("<li>");
            out.write("<img class=\"app\" src=\"" + favIconPath + "\"/>");
            out.write("<div class=\"appinfo\">");
            if (context.getDisplayName() != null)
            {
               out.write(context.getDisplayName());
            }
            else
            {
               out.write(context.getContextPath().substring(1));
            }
            out.write("<div class=\"appdetails\">");
            if (context.isFailed())
               out.write("<br> [failed]");
            if (context.isStopped())
               out.write("<br> [stopped]");
            if (context.getAttribute("version") != null)
            {
               out.write("Version " + context.getAttribute("version"));
            }
            out.write("</div></div></li>\n");
         }
      }
      %>
      </ul>
      <%      
         for (int i = 0; i < 10; i++)
         {
             // this comes from Jetty's DefaultHandler class
             out.write("\n<!-- Padding for IE                  -->");
         }
      %>
      <div class="footer">
         Jetty Web Server Plugin Version <%= sagex.jetty.starter.JettyPlugin.class.getPackage().getImplementationVersion() %><br/>
         Jetty Web Server Version <%= Server.class.getPackage().getImplementationVersion() %>
      </div>
   </div>
</body>
</html>

