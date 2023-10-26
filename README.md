# 基于JDK21下的各种WEBSERVER

- 基于Jetty12的Http/1.1 Http/2 Http/3文件服务器(TLS)，[参考文档](https://eclipse.dev/jetty/documentation/jetty-12/programming-guide/index.html#pg-server-http)
- 基于Virtual Thread的Thread-Per-Request风格（BIO风格）的EchoServer/EchoClient
- 基于com.sun.net下的HttpServer的文件服务器（无TLS支持）

## Jetty Demo的运行说明

1. 运行这里的Jetty Demo需要在cert文件夹下存放pkcs12格式的SSL证书。执行下面的命令一键生成

```shell
bash genCert.sh
```

2. 配置Chrome

在Chrome地址栏输入 `chrome://flags/#allow-insecure-localhost` 并打开，让chrome跳过对localhost的SSL证书验证。

3. 访问[https://localhost:8443](https://localhost:8443)

配置项：

```shell
java \
-Dkeystore.path=cert/cert.p12 \
-Dkeystore.password=123456 \
-Dtcp.port=8443 \
-Dudp.port=8443 \
-Dcontent.path=. \
-jar webserver-1.0-SNAPSHOT-all.jar
```

4. jetty handler开发

- [Server Handlers](https://eclipse.dev/jetty/documentation/jetty-12/programming-guide/index.html#pg-server-http-handler-use)
- [Implementing Handler](https://eclipse.dev/jetty/documentation/jetty-12/programming-guide/index.html#pg-server-http-handler-impl)

## podman

```shell
bash podman.sh
```


