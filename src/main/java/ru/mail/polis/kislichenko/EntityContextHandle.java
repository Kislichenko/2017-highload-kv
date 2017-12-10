package ru.mail.polis.kislichenko;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class EntityContextHandle {

    private static final String PREFIX = "id=";
    private static final String REPLICA_PREFIX = "&replicas=";
    private static final String CHECK_NODE_PUT = "&checkPut";

    @NotNull
    private RequestPut myPut;
    @NotNull
    private RequestGet myGet;
    @NotNull
    private RequestDelete myDelete;

    private final int sizeTopology;

    public EntityContextHandle(int port, @NotNull final MyFileDAO dao, int[] ports, String[] hosts) {
        this.myPut = new RequestPut(port, dao, ports, hosts);
        this.myGet = new RequestGet(port, dao, ports, hosts);
        this.myDelete = new RequestDelete(port, dao, ports, hosts);
        this.sizeTopology = ports.length;
    }

    public void entityContextHandle(@NotNull HttpExchange http) throws IOException {
        final String query = http.getRequestURI().getQuery();
        final String id;

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

        int ack = 0;
        int from = 0;

        if (checkAckFrom(query)) {
            try {
                ack = Integer.parseInt(extractAckFrom(query)[0]);
                from = Integer.parseInt(extractAckFrom(query)[1]);
            } catch (NumberFormatException e) {
                e.getStackTrace();
            }

            if (ack > from || ack == 0 || from == 0) {
                http.sendResponseHeaders(400, 0);
                http.close();
                return;
            }
        }

        switch (http.getRequestMethod()) {
            case "GET":
                if (checkNode(query, CHECK_NODE_PUT)) myPut.checkPut(http, id);
                else if (!checkAckFrom(query)) myGet.simpleGet(http, id);
                else myGet.topologyGet(http, id, ack, from);

                break;

            case "DELETE":
                if (!checkAckFrom(query)) myDelete.simpleDelete(http, id);
                else myDelete.topologyDelete(http, id, ack, from);
                break;

            case "PUT":
                if (!checkAckFrom(query)) myPut.simplePut(http, id);
                else myPut.topologyPut(http, id, ack, from);
                break;

            default:
                requestDefault(http, id);
                break;
        }

        http.close();
    }

    @NotNull
    private String[] extractAckFrom(@NotNull final String query) {

        String[] ackFrom = new String[2];
        try {
            ackFrom = query.substring(query.lastIndexOf(REPLICA_PREFIX)
                    + REPLICA_PREFIX.length()).split("/");
        } catch (Exception e) {

            ackFrom[0] = "" + (sizeTopology / 2 + 1);
            ackFrom[1] = "" + sizeTopology;
        }

        return ackFrom;
    }

    private boolean checkAckFrom(@NotNull final String query) {
        if (query.contains(REPLICA_PREFIX)) return true;
        return false;
    }

    private boolean checkNode(@NotNull final String query, @NotNull final String URLNode) {
        if (query.contains(URLNode)) return true;
        return false;
    }

    @NotNull
    private static String extractId(@NotNull final String query) {
        if (!query.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Query without correct PREFIX!");
        }
        String[] fromID = query.substring(PREFIX.length()).split("&");
        if (fromID[0].contains("/")) fromID[0] = fromID[0].substring(0, fromID[0].length() - 1);
        return fromID[0];
    }

    private void requestWithEmptyId(@NotNull HttpExchange http) throws IOException {
        http.sendResponseHeaders(400, 0);
        http.close();
    }

    private void requestDefault(@NotNull HttpExchange http, String id) throws IOException {
        http.sendResponseHeaders(405, 0);
    }

}