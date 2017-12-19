package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

public class ClusterService implements KVService {

    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyFileDAO dao;
    @NotNull
    private final int[] ports;
    @NotNull
    private final String[] hosts;
    @NotNull
    private final int myPort;

    public ClusterService(int port, @NotNull final MyFileDAO dao, @NotNull final Set<String> topology) throws IOException {

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;

        this.server.createContext("/v0/status", this::statusContextHandle);
        this.server.createContext("/v0/entity", this::entityContextHandle);

        this.ports = URLReader.getPorts(topology);
        this.hosts = URLReader.getHosts(topology);

        this.myPort = port;
    }

    private void entityContextHandle(@NotNull HttpExchange http) throws IOException {
        EntityContextHandle entityContext = new EntityContextHandle(myPort, dao, ports, hosts);
        entityContext.entityContextHandle(http);
    }

    private void statusContextHandle(HttpExchange http) throws IOException {
        StatusContextHandle statusContext = new StatusContextHandle();
        statusContext.contextStatus(http);
    }

    @Override
    public void start() {
        this.server.start();
    }

    @Override
    public void stop() {
        server.stop(0);
    }
}