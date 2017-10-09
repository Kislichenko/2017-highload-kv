package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MyService implements KVService {
    private static final String PREFIX = "id=";

    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyDAO dao;

    public MyService(int port, @NotNull final MyDAO dao) throws IOException {

        this.server = HttpServer.create(new InetSocketAddress(port),0);

        this.dao=dao;

        this.server.createContext("/v0/status", this::statusContextHandle);
        this.server.createContext("/v0/entity", this::entityContextHandle);
    }

    private void entityContextHandle(@NotNull HttpExchange http) throws IOException{
        final String id = extractId(http.getRequestURI().getQuery());

        if(emptyId(http,id)) return;

        switch (http.getRequestMethod()) {
            case "GET":
                requestGet(http, id);
                break;

            case "DELETE":
                requestDelete(http,id);
                break;

            case "PUT":
                requestPut(http,id);
                break;

            default:
                http.sendResponseHeaders(405, 0);
        }

        http.close();
    }

    private void statusContextHandle(HttpExchange http) throws IOException{
        final String response ="ONLINE";
        http.sendResponseHeaders(200, response.length());
        http.getResponseBody().write(response.getBytes());
        http.close();
    }

    @NotNull
    private static String extractId(@NotNull final String query){
        if(!query.startsWith(PREFIX)){
            throw  new IllegalArgumentException("Shitty string");
        }

        return  query.substring(PREFIX.length());
    }

    private void requestGet(@NotNull HttpExchange http, String id) throws IOException{
        try{
            final byte[] getValue = dao.get(id);

            http.sendResponseHeaders(200, getValue.length);
            http.getResponseBody().write(getValue);

        } catch (IOException e){
            http.sendResponseHeaders(404, 0);
        }
    }

    private void requestDelete(@NotNull HttpExchange http, String id) throws IOException{
        dao.delete(id);
        http.sendResponseHeaders(202, 0);
    }

    private void requestPut(@NotNull HttpExchange http, String id) throws IOException{

        final int contentLength =
                Integer.valueOf(http.getRequestHeaders().getFirst("Content-Length"));

        final byte[] putValue = new byte[contentLength];

        if (contentLength>0&&http.getRequestBody().read(putValue) != putValue.length) {
            throw new IOException("Can't read at once");
        }

        dao.upsert(id, putValue);
        http.sendResponseHeaders(201, 0);
    }

    private boolean emptyId(@NotNull HttpExchange http, String id) throws IOException{
        if(id.isEmpty()) {
            http.sendResponseHeaders(400, 0);
            http.close();
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.server.start();
    }

    @Override
    public void stop() {
        this.server.stop(0);
    }
}