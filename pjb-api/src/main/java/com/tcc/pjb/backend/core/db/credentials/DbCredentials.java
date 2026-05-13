package com.tcc.pjb.backend.core.db.credentials;

import java.util.Arrays;

public record DbCredentials(String username, char[] password, long ttlSeconds) implements AutoCloseable {

    @Override
    public void close() {
        if (password != null) Arrays.fill(password, '\0');
    }
}
