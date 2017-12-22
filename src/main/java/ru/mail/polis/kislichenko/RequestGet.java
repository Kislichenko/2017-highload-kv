package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;


public class RequestGet {

    private final int[] ports;
    private final String[] hosts;
    private final int myPort;

    @NotNull
    private final MyFileDAO dao;

    public RequestGet(int port, @NotNull final MyFileDAO dao, int[] ports, String[] hosts) {

        this.hosts = hosts;
        this.ports = ports;
        this.dao = dao;
        this.myPort = port;
    }

    public void topologyGet(@NotNull HttpExchange http, String id, int ack, int from) throws IOException {

        int goodReplics = 0;
        int replicsWithoutData = 0;
        byte[] getValue = {};
        int ansDeleted = 0;

        for (int i = 0; goodReplics + replicsWithoutData < from && i < hosts.length; i++) {


            if (ports[i] == myPort) {

                if (dao.checkId(id) || dao.containsDeletedKey(id)) {

                    try {

                        if (!dao.containsDeletedKey(id)) {

                            if (dao.get(id).length > getValue.length) getValue = dao.get(id);
                            if (dao.get(id).length == 0) replicsWithoutData++;
                            goodReplics++;
                        } else ansDeleted++;

                    } catch (NoSuchElementException e) {
                        continue;
                    }
                } else replicsWithoutData++;
                continue;
            }

            final HttpResponse tmpStatus;

            try {
                tmpStatus = Request.Get(URLCreating.urlNodesIdInterior(ports[i], id)).execute().returnResponse();
            } catch (IOException e) {
                continue;
            }

            int code = tmpStatus.getStatusLine().getStatusCode();

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            tmpStatus.getEntity().writeTo(byteArrayOutputStream);

            if (byteArrayOutputStream.toByteArray().length > getValue.length) {
                getValue = byteArrayOutputStream.toByteArray();
            }

            if (code == 200 && getValue.length > 0) goodReplics++;
            else if (code == 202) ansDeleted++;
            else if (code == 404) replicsWithoutData++;
        }

        if (goodReplics >= ack || goodReplics > 0 && ansDeleted == 0 && (goodReplics + replicsWithoutData + ansDeleted) >= ack) {
            http.sendResponseHeaders(200, getValue.length);
            http.getResponseBody().write(getValue);
        } else if (ansDeleted > 0 && replicsWithoutData + goodReplics + ansDeleted >= ack || replicsWithoutData >= ack) {
            http.sendResponseHeaders(404, 0);
        } else if (goodReplics < ack) {
            http.sendResponseHeaders(504, 0);
        }

        http.close();
    }

    public void simpleGet(@NotNull HttpExchange http, String id) throws IOException {
        if (dao.containsDeletedKey(id) && ports.length > 1) http.sendResponseHeaders(202, 0);
        else if (dao.checkId(id) || ports.length == 1) {
            try {
                final byte[] getValue = dao.get(id);

                http.sendResponseHeaders(200, getValue.length);
                http.getResponseBody().write(getValue);

            } catch (NoSuchElementException e) {
                http.sendResponseHeaders(404, 0);
            }
        } else http.sendResponseHeaders(504, 0);
    }

}