<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="sagex.jetty.starter.JettyInstance" %>
<%-- /*
<%@ page import="org.eclipse.jetty.server.*" %>
<%@ page import="org.eclipse.jetty.util.resource.*" %>
<%@ page import="sagex.api.PluginAPI"%>
*/ --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
   <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
   <meta http-equiv="Cache-Control" content="no-cache"/> 
   <meta http-equiv="Pragma" content="no-cache"/> 
   <meta http-equiv="Expires" content="0"/> 
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

   <div class="header">
      <div class="titlebar"><img class="logo" src="SageLogo256small.png" alt="SageTV" title="SageTV"/></div>
      <div class="title">Web Applications</div>
   </div>

   <div class="content">
   
   <%
   JettyInstance instance = JettyInstance.getInstance();
   %>


      <ul>
      
      <%
      List<String> outList = instance.getAppList();

      if (outList.size() == 0)
      {
          out.write("<li class=\"appinfo\">No web applications are installed.</li>");
       }
       else
       {
          for (int i = 0; i < outList.size(); i++)
          {
        	  out.write(outList.get(i));
          }
      }
      %>
      
      
      
      </ul>
      
   </div>

   <div class="footer">
      <p>Page Generated <%= new Date() %></p>
      <p>Jetty Web Server Plugin Version <%= sagex.jetty.starter.JettyPlugin.class.getPackage().getImplementationVersion() %></p>
      <p>Jetty Web Server Version <%= instance.getServerVersion() %></p>
   </div>

</body>
</html>
