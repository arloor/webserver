package com.arloor;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory;
import org.eclipse.jetty.http3.server.HTTP3ServerConnector;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JettyBootStrap {
    private static final Logger log = LoggerFactory.getLogger(JettyBootStrap.class);
    private static final boolean ENABLE_HTTP_3 = Boolean.parseBoolean(System.getProperty("enable.http3", "true"));
    private static final String KEYSTORE_PATH = System.getProperty("keystore.path", "cert/cert.p12");
    private static final String KEYSTORE_PASSWORD = System.getProperty("keystore.password", "123456");
    private static final String TCP_PORT = System.getProperty("tcp.port", "8443");
    private static final String UDP_PORT = System.getProperty("udp.port", "8443");
    private static final String CONTENT_PATH = System.getProperty("content.path", ".");

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniHostCheck(false); //兼容自签发证书，生产环境请去掉
        httpConfig.addCustomizer(customizer);// Add the SecureRequestCustomizer because TLS is used.
        // Configure the SslContextFactory with the keyStore information.
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(KEYSTORE_PATH);
        sslContextFactory.setKeyStorePassword(KEYSTORE_PASSWORD);

        server.addConnector(buildTcpConnector(server, httpConfig, sslContextFactory));
        HTTP3ServerConnector http3ServerConnector = null;
        if (ENABLE_HTTP_3) {
            http3ServerConnector = buildHttp3Connector(server, httpConfig, sslContextFactory);
            server.addConnector(http3ServerConnector);
        }
        scheduleSslContextFactoryRefresh(sslContextFactory, http3ServerConnector);
        GzipHandler gzipHandler = buildHandler(server);
        server.setHandler(gzipHandler);
        server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), /*CustomRequestLog.EXTENDED_NCSA_FORMAT*/"%{client}a - %u %r %s %O"));
        server.start();
    }

    private static void scheduleSslContextFactoryRefresh(SslContextFactory.Server sslContextFactory, HTTP3ServerConnector http3ServerConnector) {
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(() -> {
                    try {
                        sslContextFactory.reload(scf ->
                        {
                            log.info("reloading ssl certificates successfully!");
                        });
                        if (http3ServerConnector != null) {
                            http3ServerConnector.stop();
                            http3ServerConnector.start();
                        }
                    } catch (Throwable t) {
                        log.warn("Keystore Reload Failed", t);
                    }
                }, 1, 1, TimeUnit.DAYS);
    }

    private static GzipHandler buildHandler(Server server) {
        GzipHandler gzipHandler = new GzipHandler();
        server.setHandler(gzipHandler);
        Handler.Sequence sequence = new Handler.Sequence();
        gzipHandler.setHandler(sequence);
        gzipHandler.setMinGzipSize(1024);
        sequence.addHandler(new IpHandler());
        sequence.addHandler(buildResourceHandler());
        return gzipHandler;
    }

    private static ResourceHandler buildResourceHandler() {
        // Create and configure a ResourceHandler.
        ResourceHandler handler = new ResourceHandler();
        // Configure the directory where static resources are located.
        handler.setBaseResource(ResourceFactory.of(handler).newResource(CONTENT_PATH));
        // Configure directory listing.
        handler.setDirAllowed(false);
        // Configure welcome files.
        handler.setWelcomeFiles(List.of("index.html"));
        // Configure whether to accept range requests.
        handler.setAcceptRanges(true);
        return handler;
    }

    private static HTTP3ServerConnector buildHttp3Connector(Server server, HttpConfiguration httpConfig, SslContextFactory.Server sslContextFactory) {
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server, sslContextFactory, /*用于增加响应头 Alt-Svc: h3=":843"*/new HTTP3ServerConnectionFactory(httpConfig));
        /**
         * https://stackoverflow.com/questions/77317395/java-lang-illegalstateexception-no-pem-work-directory-configured
         * Jetty's implementation, like many others, use the quiche library as the underlying implementation of QUIC, the protocol at the base of HTTP/3.
         * Quiche (written in Rust), does not use Java KeyStores, so you have to provide the public and private key as PEM files.
         * Jetty will take care of converting your KeyStore to PEM files, but it needs a directory to save the PEM files to. Since one of the PEM files is the private key, the PEM directory must be adequately protected using file system permissions, and that is why Jetty cannot use a default PEM directory (for example, /tmp/ would be a terrible choice because anyone will have access to your PEM files).
         * You just to specify a directory to store your PEM files (make sure its file permission are adequate), and Jetty will do the rest.
         */
        connector.getQuicConfiguration().setPemWorkDirectory(Path.of("/tmp"));
        connector.setPort(Integer.parseInt(UDP_PORT));
        return connector;
    }

    private static ServerConnector buildTcpConnector(Server server, HttpConfiguration httpConfig, SslContextFactory.Server sslContextFactory) {
        // The ConnectionFactory for HTTP/1.1.
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);
        // The ConnectionFactory for HTTP/2.
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
        // The ALPN ConnectionFactory.
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        // The default protocol to use in case there is no negotiation.
        alpn.setDefaultProtocol(http11.getProtocol());
        // The ConnectionFactory for TLS.
        SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
        // The ServerConnector instance.
        ServerConnector connector = new ServerConnector(server, tls, alpn, h2, http11);
        connector.setPort(Integer.parseInt(TCP_PORT));
        return connector;
    }
}
