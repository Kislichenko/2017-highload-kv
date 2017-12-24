package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    @NotNull
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public ClusterService(int port, @NotNull final MyFileDAO dao, @NotNull final Set<String> topology) throws IOException {

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;

        this.ports = URLReader.getPorts(topology);
        this.hosts = URLReader.getHosts(topology);

        this.myPort = port;

        this.server.createContext("/v0/status", http -> executor.execute(() -> {
            try {
                new StatusContextHandle().contextStatus(http);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        this.server.createContext("/v0/entity", http -> executor.execute(() -> {
            try {
                new EntityContextHandle(myPort, dao, ports, hosts).entityContextHandle(http);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
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