package com.tcc.pjb.backend.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class Gzip {

    private Gzip() {
    }

    public static byte[] gzip(byte[] plain) {
        if (plain == null || plain.length == 0) return new byte[0];
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(plain.length, 64 * 1024));
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(plain);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gzip", e);
        }
    }

    public static byte[] ungzip(byte[] gz, int maxInflatedBytes) {
        if (gz == null || gz.length == 0) return new byte[0];
        int cap = maxInflatedBytes <= 0 ? (16 * 1024 * 1024) : maxInflatedBytes;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(gz.length * 2, 256 * 1024));
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
                byte[] buf = new byte[8192];
                int n;
                int total = 0;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > cap) {
                        throw new IllegalStateException("Payload inflado excede limite: " + cap + " bytes");
                    }
                    out.write(buf, 0, n);
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ungzip", e);
        }
    }
}
