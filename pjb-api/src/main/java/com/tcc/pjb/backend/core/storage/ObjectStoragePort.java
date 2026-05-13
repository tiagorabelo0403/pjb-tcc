package com.tcc.pjb.backend.core.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

public interface ObjectStoragePort {

    ObjectWriteResult put(String key,
                          InputStream data,
                          long contentLength,
                          String contentType,
                          Map<String, String> metadata) throws IOException;

    ObjectReadResult get(String key) throws IOException;

    URI presignPut(String key, Duration expires);

    URI presignGet(String key, Duration expires);

    boolean exists(String key);

    void delete(String key) throws IOException;
}
