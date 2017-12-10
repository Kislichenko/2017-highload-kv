package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class StatusContextHandle {

    private  final String response = "ONLINE";

    public StatusContextHandle(){}

    public void contextStatus(HttpExchange http) throws IOException {

        http.sendResponseHeaders(200, response.length());
        http.getResponseBody().write(response.getBytes());
        http.close();
    }
}
