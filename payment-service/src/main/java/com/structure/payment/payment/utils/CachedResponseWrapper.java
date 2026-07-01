package com.structure.payment.payment.utils;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class CachedResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);
    private PrintWriter writer;


    public CachedResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(buffer,
                    getCharacterEncoding()));
        }
        return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStream() {
            @Override public void write(int b) { buffer.write(b); }
            @Override public boolean isReady()  { return true; }
            @Override public void setWriteListener(WriteListener l) {}
        };
    }

    String getCapturedBody() {
        if (writer != null) writer.flush();
        return buffer.toString();
    }

    /** Write the buffered body to the real response. */
    void copyBodyToResponse() throws IOException, IOException {
        if (writer != null) writer.flush();
        byte[] bytes = buffer.toByteArray();
        getResponse().setContentLength(bytes.length);
        getResponse().getOutputStream().write(bytes);
    }
}
