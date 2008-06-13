package net.sf.sageplugins.jetty.filters;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Compresses HTTP server response data using gzip.  The filter must be
 * specified in the web.xml of the application being deployed.
 * Adapted from Head First Servlets & JSP, First Edition
 */
public class CompressionFilter implements Filter
{
    private FilterConfig filterConfig;
    //private List<String> excludeContentTypes = new ArrayList<String>();

    public void init(FilterConfig config) throws ServletException
    {
        this.filterConfig = config;

        /*String excludeContentTypesParameter = filterConfig.getInitParameter("excludeContentTypes");
        String[] excludeContentTypesArray = excludeContentTypesParameter.split(",");
        for (String excludeContentType : excludeContentTypesArray)
        {
            excludeContentType = excludeContentType.trim();
            if (excludeContentType.length() > 0)
            {
                excludeContentTypes.add(excludeContentType);
            }
        }*/
    }

    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String validEncodings = request.getHeader("Accept-Encoding");
        //String contentType = response.getContentType();
        if ((validEncodings.indexOf("gzip") > -1)/* && (!excludeContentTypes.contains(contentType))*/)
        {
            CompressionResponseWrapper wrappedResponse = new CompressionResponseWrapper(response);
            wrappedResponse.setHeader("Content-Encoding", "gzip");
            wrappedResponse.setHeader("Vary", "Accept-Encoding");
            wrappedResponse.setHeader("Transfer-Encoding", "chunked");

            chain.doFilter(req, wrappedResponse);

            wrappedResponse.flushBuffer();
            GZIPOutputStream gzos = wrappedResponse.getGZIPOutputStream();
            gzos.finish();
        }
    }

    public void destroy()
    {
        this.filterConfig = null;
    }
}
