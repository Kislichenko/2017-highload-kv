package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class RequestPut {

    private final int myPort;
    @NotNull
    private final MyFileDAO dao;
    private int[] ports;
    private String[] hosts;

    public RequestPut(int port, @NotNull final MyFileDAO dao, int[] ports, String[] hosts) {

        this.hosts = hosts;
        this.ports = ports;
        this.dao = dao;
        this.myPort = port;
    }

    public void simplePut(@NotNull HttpExchange http, String id) throws IOException {

        final int contentLength = http.getRequestBody().available();
        dao.upsert(id, new StreamReading(http.getRequestBody(), contentLength).getByteArray());
        http.sendResponseHeaders(201, 0);
    }

    public void topologyPut(@NotNull HttpExchange http, String id, int ack, int from) throws IOException {
        int goodReplics = 0;
        final int contentLength = http.getRequestBody().available();

        byte[] myValue = new StreamReading(http.getRequestBody(), contentLength).getByteArray();

        int checkIdPut = -1;

        for (int i = 0; i < hosts.length; i++) {
            if (ports[i] == myPort) {
                if (dao.containsDeletedKey(id) || dao.checkId(id)) {
                    checkIdPut = i;
                    break;
                }
                continue;
            }
            try {
                int tmpCode = Request.Put(URLCreating.urlNodesIdPut(ports[i], id)).execute().
                        returnResponse().getStatusLine().getStatusCode();

                if (tmpCode == 201) {
                    checkIdPut = i + 1;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        boolean checkGoods = false;

        if (checkIdPut == -1) checkIdPut = 0;
        else checkGoods = true;

        if (dao.containsDeletedKey(id) || dao.checkId(id) || !checkGoods) dao.upsert(id, myValue);
        goodReplics++;

        for (int i = checkIdPut; goodReplics < from && i < hosts.length; i++) {

            if (ports[i] == myPort) continue;

            HttpResponse tmpStatus;

            try {
                if (!checkGoods) tmpStatus = Request.Put(URLCreating.urlNodesIdInterior(ports[i], id))
                        .bodyByteArray(myValue).execute().returnResponse();
                else tmpStatus = Request.Put(URLCreating.urlNodesIdPut(ports[i], id))
                        .bodyByteArray(myValue).execute().returnResponse();

            } catch (IOException e) {
                continue;
            }

            int code = tmpStatus.getStatusLine().getStatusCode();

            if (code == 201) goodReplics++;
        }

        if (goodReplics >= ack) http.sendResponseHeaders(201, 0);
        else http.sendResponseHeaders(504, 0);

        http.close();
    }

    public void checkPut(@NotNull HttpExchange http, String id) throws IOException {
        if (dao.containsDeletedKey(id) || dao.checkId(id)) simplePut(http, id);
        else http.sendResponseHeaders(404, 0);
    }

}