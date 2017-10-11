package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

public class MyService implements KVService {
    private static final String PREFIX = "id=";

    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyFileDAO dao;

    public MyService(int port, @NotNull final MyFileDAO dao) throws IOException {

        this.server = HttpServer.create(new InetSocketAddress(port),0);
        this.dao = dao;

        this.server.createContext("/v0/status", this::statusContextHandle);
        this.server.createContext("/v0/entity", this::entityContextHandle);
    }

    private void entityContextHandle(@NotNull HttpExchange http) throws IOException{
        final String id;

        try {
              id = extractId(http.getRequestURI().getQuery());
        }catch(IllegalArgumentException e){
            http.sendResponseHeaders(404, 0);
            http.close();
            return;
        }

        if(id.isEmpty()) {
            requestWithEmptyId(http);
            return;
        }

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
                requestDefault(http, id);
                break;
        }

        http.close();
    }

    private void statusContextHandle(HttpExchange http) throws IOException{
        final String response = "ONLINE";

        http.sendResponseHeaders(200, response.length());
        http.getResponseBody().write(response.getBytes());

        http.close();
    }


    @NotNull
    private static String extractId(@NotNull final String query){
        if(!query.startsWith(PREFIX)){
            throw  new IllegalArgumentException("Query without correct PREFIX!");
        }
        return  query.substring(PREFIX.length());
    }

    private void requestGet(@NotNull HttpExchange http, String id) throws IOException{
        try{
            final byte[] getValue = dao.get(id);

            http.sendResponseHeaders(200, getValue.length);
            http.getResponseBody().write(getValue);

        }catch (NoSuchElementException e){
            http.sendResponseHeaders(404, 0);
        }

    }

    private void requestDefault(@NotNull HttpExchange http, String id) throws  IOException{
        http.sendResponseHeaders(405, 0);
    }

    private void requestDelete(@NotNull HttpExchange http, String id) throws IOException{
        dao.delete(id);
        http.sendResponseHeaders(202, 0);
    }

    private void requestPut(@NotNull HttpExchange http, String id) throws IOException{

        String ctLength = http.getRequestHeaders().getFirst("Content-Length");

        int contentLength = 0;

        //если "Content-Length" не был отправлен в запросе, то по дефолту принимаем тело равным 0
        if(ctLength != null) contentLength =  Integer.valueOf(ctLength);

        FileReading readFile = new FileReading(http.getRequestBody(),contentLength);

        dao.upsert(id, readFile.getFindByteArray());
        http.sendResponseHeaders(201, 0);
    }

    private void requestWithEmptyId(@NotNull HttpExchange http) throws IOException{
        http.sendResponseHeaders(400, 0);
        http.close();
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