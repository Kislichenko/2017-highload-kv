package ru.mail.polis.kislichenko;

import com.google.common.collect.Iterators;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.http.HttpResponse;
import  org.apache.*;
//import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Set;

public class ClusterService implements KVService{
    private static final String PREFIX = "id=";
    private static final String REPLICA_PREFIX="&replicas=";

    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyFileDAO dao;

    private int[] ports;
    private String[] hosts;

    @NotNull
    private final Set<String> topology;

    private final int myPort;

    public ClusterService(int port, @NotNull final MyFileDAO dao, @NotNull final Set<String> topology) throws IOException {

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;

        this.server.createContext("/v0/status", this::statusContextHandle);
        this.server.createContext("/v0/entity", this::entityContextHandle);

        this.topology=topology;

        this.ports=getPorts(topology);
        this.hosts=getHosts(topology);

        this.myPort=port;
    }

    @NotNull
    private static String extractId(@NotNull final String query) {
        if (!query.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Query without correct PREFIX!");
        }
        String [] fromID = query.substring(PREFIX.length()).split("&");
        return fromID[0];
    }

    @NotNull
    private static String[] extractAckFrom(@NotNull final String query){
        //проверить есть ли слэш!!!!
        String[] ackFrom=query.substring(query.lastIndexOf(REPLICA_PREFIX)
                +REPLICA_PREFIX.length()).split("/");

        return ackFrom;
    }

    private boolean checkAckFrom(@NotNull final String query){
        if (query.contains(REPLICA_PREFIX)) return true;
        return false;
    }

    private int[] getPorts(@NotNull final Set<String> topology){
        String[] endpoints = topology.toArray(new String[topology.size()]);
        int[] ports=new int[topology.size()];

        for(int i=0;i<endpoints.length;i++){
            ports[i]=Integer.parseInt(endpoints[i].substring(endpoints[i].lastIndexOf(':')+1));
        }

        return ports;
    }

    private String[] getHosts(@NotNull final Set<String> topology){
        String[] endpoints = topology.toArray(new String[topology.size()]);
        String[] hosts=new String[topology.size()];

        for(int i=0;i<endpoints.length;i++){
            hosts[i]=endpoints[i].substring("http://".length(),endpoints[i].lastIndexOf(':'));
        }

        return hosts;
    }

    private void entityContextHandle(@NotNull HttpExchange http) throws IOException {
        final String query=http.getRequestURI().getQuery();
        String id;

        try {
            id = extractId(query);
        } catch (IllegalArgumentException e) {
            http.sendResponseHeaders(404, 0);
            http.close();
            return;
        }

        if (id.isEmpty()) {
            requestWithEmptyId(http);
            return;
        }

         int ack=0;
         int from=0;
        //проверить можно ли преобразовать в int!!!
        if(checkAckFrom(query)) {
            ack = Integer.parseInt(extractAckFrom(query)[0]);
            from = Integer.parseInt(extractAckFrom(query)[1]);
            if(ack>from||ack==0||from==0) {
                http.sendResponseHeaders(400, 0);
                http.close();
                return;
            }
        }


        switch (http.getRequestMethod()) {
            case "GET":
                if(!checkAckFrom(query)) {
                    requestGet(http, id);
                }else requestGetTopology(http, id, ack, from);
                break;

            case "DELETE":
                if(!checkAckFrom(query)) {
                    requestDelete(http, id);
                }else requestDeleteTopology(http, id, ack, from);
                break;

            case "PUT":
                if(!checkAckFrom(query)) {
                    requestPut(http, id);
                }else requestPutTopology(http, id, ack, from);

                break;

            default:
                requestDefault(http, id);
                break;
        }

        http.close();
    }

    private void statusContextHandle(HttpExchange http) throws IOException {
        final String response = "ONLINE";

        http.sendResponseHeaders(200, response.length());
        http.getResponseBody().write(response.getBytes());

        http.close();
    }

    private void requestGetTopology(@NotNull HttpExchange http, String id, int ack, int from) throws IOException {


        int code=0;
        int goodReplics=0;
        int replicsWithoutData=0;
        byte[] getValue={};

        System.out.println("Начинаем получать данные в кластеры!" +
                "Количество портов:  "+ports.length+" ; количество нужных кластеров "+ack+
                " ; Количество хостов: "+hosts.length+" ; Мой порт: "+ports[0]);

            for(int i=0;i<hosts.length;i++){

               System.out.println("Хост получения №"+i);

                if(ports[i]==myPort) {
                    System.out.println("Получили свой порт"+i);

                    try{

                        if( dao.get(id).length>getValue.length) {
                            getValue = dao.get(id);
                        }

                    }catch (NoSuchElementException e) {
                        continue;
                    }

                    goodReplics++;
                    if(replicsWithoutData+goodReplics==ack)break;
                    continue;
                }

                HttpResponse tmpStatus=null;

                try {
                    System.out.println("Пытемся получить данные с сервера: "+i+" ; Ссылка на получение: "+url(ports[i], id));
                    tmpStatus = Request.Get(url(ports[i], id)).execute().returnResponse();


                }catch (IOException e){
                    System.out.println("Не получилось получить статус от сервера или передать данные: "+e.getMessage());

                    continue;
                }

                System.out.println("Получили ответ от Get! ");

                code = tmpStatus.getStatusLine().getStatusCode();

                System.out.println("Получили следующий ответ от Get:  "+code);

                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                tmpStatus.getEntity().writeTo( byteArrayOutputStream );

                if(byteArrayOutputStream.toByteArray().length>getValue.length) {
                    getValue = byteArrayOutputStream.toByteArray();
                }

                System.out.println("getValue: " +getValue.length);

                if(code == 200&&getValue.length>0){

                    goodReplics++;
                }
                else if(code == 404){
                    System.out.println("Ответ получен, но данных нет! ");
                    replicsWithoutData++;
                }
                if(replicsWithoutData+goodReplics==ack)break;
            }

            System.out.println("Количество итоговых реплик: "+goodReplics);
            System.out.println("Количество 404: "+replicsWithoutData);

            if(goodReplics>=ack){
                http.sendResponseHeaders(200, getValue.length);
                http.getResponseBody().write(getValue);
            }
            else if(replicsWithoutData>0&&replicsWithoutData+goodReplics==ack||replicsWithoutData==ack){
                http.sendResponseHeaders(404, 0);
            }
            else if(replicsWithoutData+goodReplics<ack){
                http.sendResponseHeaders(504, 0);
            }


          http.close();

    }

    private void requestPutTopology(@NotNull HttpExchange http, String id, int ack, int from) throws IOException {
        int code=0;
        int goodReplics=0;
        byte[] myValue={};
        final int contentLength = http.getRequestBody().available();


        System.out.println("Начинаем записывать данные в кластеры!" +
                "Количество портов:  "+ports.length+" ; количество нужных кластеров "+ack+
                " ; Количество хостов: "+hosts.length+" ; Мой порт: "+ports[0]);

        for(int i=0;i<hosts.length;i++){

            //проходим по топологии
            System.out.println("Хост № "+i);

            if(ports[i]==myPort) {
                System.out.println("Нашли свой сервер под номером "+i);
                //записываем в свой сервер
                dao.upsert(id, new StreamReading(http.getRequestBody(), contentLength).getByteArray());


                goodReplics++;
                if(goodReplics==ack)break;
                continue;
            }

            HttpResponse tmpStatus=null;

            //пытаемся записать данные на другие сервера и получить ответ
            try {
                System.out.println("Номер порта: "+i+" ; Ссылка к серверу: "+url(ports[i], id));

                myValue=new StreamReading(http.getRequestBody(), contentLength).getByteArray();

                System.out.println(""+myValue.toString());
                //отправляем запрос с даннымити и хоти получить ответ
                tmpStatus = Request.Put(url(ports[i], id)).bodyByteArray(myValue).execute().returnResponse();


            }catch (IOException e){
                System.out.println("Не получилось получить статус от сервера или передать данные: "+e.getMessage());

                continue;
            }

                code = tmpStatus.getStatusLine().getStatusCode();
                System.out.println("Получаем код: "+code);

                if(code == 201){
                    goodReplics++;
                    System.out.println("Количество итоговых реплик "+goodReplics);
                }

               if(goodReplics==ack) break;
        }
        System.out.println("Количество итоговых реплик "+goodReplics);

        if(goodReplics>=ack){
            System.out.println("Все хорошо!");
            http.sendResponseHeaders(201, 0);
        }
        else {
            http.sendResponseHeaders(504, 0);
        }


      http.close();
    }

    private void requestDeleteTopology(@NotNull HttpExchange http, String id, int ack, int from) throws IOException {

        int code=0;
        int goodReplics=0;


        System.out.println("Начинаем записывать данные в кластеры!" +
                "Количество портов:  "+ports.length+" ; количество нужных кластеров "+ack+
                " ; Количество хостов: "+hosts.length+" ; Мой порт: "+ports[0]);

        for(int i=0;i<hosts.length;i++){

            //проходим по топологии
            System.out.println("Хост № "+i);

            if(ports[i]==myPort) {
                System.out.println("Нашли свой сервер под номером "+i);
                //удаляпм из своего сервера
                dao.delete(id);

                goodReplics++;
                if(goodReplics==ack)break;
                 continue;
            }

            HttpResponse tmpStatus=null;

            //пытаемся записать данные на другие сервера и получить ответ
            try {
                System.out.println("Номер порта: "+i+" ; Ссылка к серверу: "+url(ports[i], id));

                //отправляем запрос с даннымити и хоти получить ответ
                tmpStatus = Request.Delete(url(ports[i], id)).execute().returnResponse();

            }catch (IOException e){
                System.out.println("Не получилось получить статус от сервера или удалить данные: "+e.getMessage());

                continue;
            }

            code = tmpStatus.getStatusLine().getStatusCode();
            System.out.println("Получаем код: "+code);


            if(code == 202){
                goodReplics++;
                System.out.println("Количество итоговых реплик "+goodReplics);
            }

            if(goodReplics==ack)break;
        }
        System.out.println("Количество итоговых реплик "+goodReplics);

        if(goodReplics>=ack){
            System.out.println("Все хорошо!");
            http.sendResponseHeaders(202, 0);
        }
        else {
            http.sendResponseHeaders(504, 0);
        }

        http.close();
    }

    private void requestGet(@NotNull HttpExchange http, String id) throws IOException {
        try {
            final byte[] getValue = dao.get(id);

            http.sendResponseHeaders(200, getValue.length);
            http.getResponseBody().write(getValue);

        } catch (NoSuchElementException e) {
            http.sendResponseHeaders(404, 0);
        }

    }

    private void requestDefault(@NotNull HttpExchange http, String id) throws IOException {
        http.sendResponseHeaders(405, 0);
    }

    private void requestDelete(@NotNull HttpExchange http, String id) throws IOException {
        dao.delete(id);
        http.sendResponseHeaders(202, 0);
    }

    private void requestPut(@NotNull HttpExchange http, String id) throws IOException {

        final int contentLength = http.getRequestBody().available();

        dao.upsert(id, new StreamReading(http.getRequestBody(), contentLength).getByteArray());
        http.sendResponseHeaders(201, 0);
    }

    private void requestWithEmptyId(@NotNull HttpExchange http) throws IOException {
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

    @NotNull
    private String url(final int port, @NotNull final String id) {
        return endpoint(port) + "/v0/entity?id=" + id;
    }

    @NotNull
    static String endpoint(final int port) {
        return "http://localhost:" + port;
    }
}
