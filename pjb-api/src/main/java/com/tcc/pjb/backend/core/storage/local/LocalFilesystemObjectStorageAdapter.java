package com.tcc.pjb.backend.core.storage.local;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.FileSystemResource;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;

public final class LocalFilesystemObjectStorageAdapter implements ObjectStoragePort {

    private final Path baseDir;
    private final URI publicBaseUri;

    public LocalFilesystemObjectStorageAdapter(Path baseDir, URI publicBaseUri) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir");
        this.publicBaseUri = Objects.requireNonNull(publicBaseUri, "publicBaseUri");
    }

    @Override
    public ObjectWriteResult put(String key, InputStream data, long contentLength, String contentType, Map<String, String> metadata) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(data, "data");

        Path target = resolveKey(key);
        Files.createDirectories(target.getParent());

        Path tmp = target.getParent().resolve("upload-" + System.nanoTime() + ".tmp");

        MessageDigest sha256 = digester("SHA-256");
        MessageDigest sha384 = digester("SHA-384");

        long written = 0L;
        try (var in = data; var out = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[1024 * 256];
            int r;
            while ((r = in.read(buf)) != -1) {
                if (r == 0) continue;
                out.write(buf, 0, r);
                sha256.update(buf, 0, r);
                sha384.update(buf, 0, r);
                written += r;
            }
        }

        if (contentLength >= 0 && written != contentLength) {
            Files.deleteIfExists(tmp);
            throw new IOException("tamanho divergente: esperado=" + contentLength + " gravado=" + written);
        }

        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        URI uri = publicBaseUri.resolve("/objects/" + sanitizeKey(key));
        return new ObjectWriteResult(
                key,
                uri,
                written,
                HexFormat.of().formatHex(sha256.digest()),
                HexFormat.of().formatHex(sha384.digest())
        );
    }

    @Override
    public ObjectReadResult get(String key) throws IOException {
        Path p = resolveKey(key);
        if (!Files.exists(p)) {
            throw new IOException("objeto não encontrado: " + key);
        }
        return new ObjectReadResult(new FileSystemResource(p), Files.size(p), "application/pdf");
    }

    @Override
    public URI presignPut(String key, Duration expires) {
        return publicBaseUri.resolve("/api/v1/uploads/direct/" + sanitizeKey(key));
    }

    @Override
    public URI presignGet(String key, Duration expires) {
        return publicBaseUri.resolve("/objects/" + sanitizeKey(key));
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolveKey(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolveKey(key));
    }

    private Path resolveKey(String key) {
        String safe = sanitizeKey(key);
        return baseDir.resolve(safe);
    }

    private static String sanitizeKey(String key) {
        String k = key.trim().replace('\\', '/');
        while (k.startsWith("/")) k = k.substring(1);
        if (k.contains("..")) {
            throw new IllegalArgumentException("key inválida");
        }
        return k;
    }

    private static MessageDigest digester(String alg) {
        try {
            return MessageDigest.getInstance(alg);
        } catch (Exception e) {
            throw new IllegalStateException("algoritmo indisponível: " + alg, e);
        }
    }
}
