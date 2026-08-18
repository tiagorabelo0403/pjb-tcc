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
import java.util.Locale;
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
        String ct = detectContentType(p, key);
        return new ObjectReadResult(new FileSystemResource(p), Files.size(p), ct);
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

    static String detectContentType(Path path, String key) {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] header = in.readNBytes(16);
            if (header.length >= 3) {
                int b0 = header[0] & 0xFF;
                int b1 = header[1] & 0xFF;
                int b2 = header[2] & 0xFF;

                // JPEG: FF D8 FF
                if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return "image/jpeg";

                // PNG: 89 50 4E 47
                if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && header.length >= 4 && (header[3] & 0xFF) == 0x47)
                    return "image/png";

                // PDF: %PDF
                if (b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && header.length >= 4 && (header[3] & 0xFF) == 0x46)
                    return "application/pdf";

                // GIF: GIF8
                if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) return "image/gif";
            }
            // WebP: RIFF????WEBP (needs 12 bytes)
            if (header.length >= 12
                    && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50)
                return "image/webp";

        } catch (IOException ignored) {
            // fall through to extension-based detection
        }
        return detectByExtension(key);
    }

    private static String detectByExtension(String key) {
        if (key == null) return "application/octet-stream";
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".txt"))  return "text/plain; charset=UTF-8";
        return "application/octet-stream";
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
