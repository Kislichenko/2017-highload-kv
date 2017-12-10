package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;


import javax.security.auth.callback.Callback;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;


public class RequestDelete {

    private final int[] ports;
    private final String[] hosts;
    private final int myPort;


    @NotNull
    private final MyFileDAO dao;

    public RequestDelete(int port, @NotNull final MyFileDAO dao, int[] ports, String[] hosts) {

        this.hosts=hosts;
        this.ports=ports;
        this.dao=dao;
        this.myPort=port;
    }

    public void simpleDelete(@NotNull HttpExchange http, String id) throws IOException {
        if(dao.checkId(id)||ports.length==1) {
            dao.delete(id);
            http.sendResponseHeaders(202, 0);
        }else http.sendResponseHeaders(404, 0);
    }

    public void topologyDelete(@NotNull HttpExchange http, String id, int ack, int from) throws IOException {
        int goodReplics=0;

        for(int i=0;goodReplics<from&&i<hosts.length;i++){
            if(ports[i]==myPort) {
                if (dao.checkId(id)) {
                    dao.delete(id);
                    goodReplics++;
                }
                continue;
            }
            int code;

            HttpResponse tmpStatus;
            try {
                tmpStatus = Request.Delete(URLCreating.urlEntity(ports[i], id)).execute().returnResponse();
            } catch (IOException e) {
                continue;
            }
            code = tmpStatus.getStatusLine().getStatusCode();

            if (code == 202) goodReplics++;

        }
        if(goodReplics>=ack) http.sendResponseHeaders(202, 0);
        else http.sendResponseHeaders(504, 0);

        http.close();
        }
    }