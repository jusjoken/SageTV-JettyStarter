package net.sf.sageplugins.jetty.filters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class CompressionResponseWrapper extends HttpServletResponseWrapper
{
    private GZIPServletOutputStream servletGzipOS = null;
    private PrintWriter printWriter = null;
    private Object streamUsed = null;

    public CompressionResponseWrapper(HttpServletResponse resp)
    {
        super(resp);
    }

    /**
     * Ignore this method, the output will be compressed.
     */
    @Override
    public void setContentLength(int len) { }

    /**
     * Flushes all buffered data to the client.
     */
    @Override
    public void flushBuffer() throws IOException
    {
        if (printWriter != null)
        {
            // wraps both servletGzipOS and getResponse().getOutputStream()
            printWriter.flush();
        }
        else if (servletGzipOS != null)
        {
            // wraps getResponse().getOutputStream()
            servletGzipOS.flush();
        }
        else
        {
            getResponse().flushBuffer();
        }
    }

    public void finish() throws IOException
    {
        flushBuffer();
        // will be null if resource is '304 Not Modified'
        if (servletGzipOS != null)
        {
            servletGzipOS.finish();
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        // Allow the servlet to access a servlet output stream, only if
        // the servlet has not already accessed the print writer.
        if ((streamUsed != null) && (streamUsed != servletGzipOS))
        {
            throw new IllegalStateException();
        }

        if (servletGzipOS == null)
        {
            servletGzipOS = new GZIPServletOutputStream(getResponse().getOutputStream());
            streamUsed = servletGzipOS;
        }

        return servletGzipOS;
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        // Allow the servlet to access a print writer, only if the servlet has
        // not already accessed the servlet output stream.
        if ((streamUsed != null) && (streamUsed != printWriter))
        {
            throw new IllegalStateException();
        }

        if (printWriter == null)
        {
            // To make a print writer, we have to first wrap the servlet output stream
            // and then wrap the compression servlet output stream in two additional output
            // stream decorators: OutputStreamWriter which converts characters into bytes,
            // and the a PrintWriter on top of the OutputStreamWriter object.
            servletGzipOS = new GZIPServletOutputStream(getResponse().getOutputStream());

            OutputStreamWriter osw = new OutputStreamWriter(servletGzipOS,
                    getResponse().getCharacterEncoding());

            printWriter = new PrintWriter(osw);
            streamUsed = printWriter;
        }

        return printWriter;
    }
}
