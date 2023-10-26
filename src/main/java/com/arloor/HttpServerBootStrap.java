package com.arloor;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;


/**
 * 使用虚拟线程池开启java文件服务器
 * Java内置的HttpServer使用的是BIO，使用虚拟线程后，性能应该能有提升
 */
public class HttpServerBootStrap {
    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(Inet4Address.getLoopbackAddress(),7777), 0);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        Path path = Path.of(".").toAbsolutePath();
        HttpHandler fileHandler = SimpleFileServer.createFileHandler(path);
        httpServer.createContext("/", fileHandler);
        httpServer.start();
    }
}
