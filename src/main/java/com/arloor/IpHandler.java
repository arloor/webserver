package com.arloor;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.net.InetSocketAddress;

// https://eclipse.dev/jetty/documentation/jetty-12/programming-guide/index.html#pg-server-http-handler-impl
class IpHandler extends Handler.Abstract {
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String pathInContext = Request.getPathInContext(request);
        if ("/ip".equals(pathInContext)) {
            response.setStatus(200);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain; charset=UTF-8");

            // Write a Hello World response.
            Content.Sink.write(
                    response,
                    true,
                    ((InetSocketAddress) request.getConnectionMetaData().getRemoteSocketAddress()).getAddress().getHostAddress(),
                    callback
            );
            return true;
        }
        return false;
    }
}
