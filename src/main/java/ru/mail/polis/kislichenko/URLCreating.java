package ru.mail.polis.kislichenko;

import org.jetbrains.annotations.NotNull;

public class URLCreating {

    @NotNull
    public static String urlNodesIdPut(final int port, @NotNull final String id) {
        return endpoint(port) + "/v0/entity?id=" + id + "/&checkPut";
    }

    @NotNull
    public static String urlNodesIdInterior(final int port, @NotNull final String id) {
        return endpoint(port) + "/v0/entity?id=" + id + "/&interior";
    }
    @NotNull
    public static String endpoint(final int port) {
        return "http://localhost:" + port;
    }
}