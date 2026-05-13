package com.tcc.pjb.backend.service.recursal.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecursalAttachmentStorageService {

    private final RecursalAttachmentProperties props;

    public RecursalAttachmentStorageService(RecursalAttachmentProperties props) {
        this.props = Objects.requireNonNull(props, "props");
    }

    public RecursalStoredFileRef store(Long processoId,
                                       String originalFilename,
                                       String contentType,
                                       InputStream inputStream) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(inputStream, "inputStream");

        Path base = Paths.get(props.getLocalPath()).toAbsolutePath().normalize();
        Path dir = base.resolve("processo").resolve(String.valueOf(processoId));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao criar diretório de anexos: " + dir, e);
        }

        String safeName = sanitizeFilename(originalFilename);
        String ext = extensionOf(safeName);

        Path tmp = dir.resolve("upload-" + Instant.now().toEpochMilli() + "-" + Math.abs(Objects.hashCode(safeName)) + ".tmp");

        long max = props.getMaxUploadBytes();
        long size = 0L;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }

        byte[] buf = new byte[32 * 1024];
        try (OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            while (true) {
                int r = inputStream.read(buf);
                if (r < 0) break;
                if (r == 0) continue;

                size += r;
                if (max > 0 && size > max) {
                    throw new PayloadTooLargeException("Arquivo excede maxUploadBytes=" + max);
                }

                md.update(buf, 0, r);
                out.write(buf, 0, r);
            }
        } catch (PayloadTooLargeException e) {
            safeDelete(tmp);
            throw e;
        } catch (IOException e) {
            safeDelete(tmp);
            throw new IllegalStateException("Falha ao persistir upload (stream)", e);
        }

        String sha256 = HexFormat.of().formatHex(md.digest());
        String filename = sha256 + (ext.isBlank() ? "" : ("." + ext));
        Path finalPath = dir.resolve(filename);

        try {
            Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            
            try {
                Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                safeDelete(tmp);
                throw new IllegalStateException("Falha ao finalizar upload", ex);
            }
        }

        String storageKey = "processo/" + processoId + "/" + filename;

        return new RecursalStoredFileRef(
                processoId,
                storageKey,
                sha256,
                size,
                contentType == null ? null : contentType.trim(),
                safeName,
                Instant.now()
        );
    }

    public Path resolveLocalPath(RecursalStoredFileRef ref) {
        Objects.requireNonNull(ref, "ref");
        Path base = Paths.get(props.getLocalPath()).toAbsolutePath().normalize();
        return base.resolve(ref.storageKey()).normalize();
    }

    public Path resolveLocalPath(Long processoId, String filename) {
        Path base = Paths.get(props.getLocalPath()).toAbsolutePath().normalize();
        return base.resolve("processo").resolve(String.valueOf(processoId)).resolve(filename).normalize();
    }

    private static void safeDelete(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
        }
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null) return "";
        String v = filename.trim();
        v = v.replaceAll("[\\r\\n\\t]", "_");
        v = v.replaceAll("[/\\\\]", "_");
        if (v.length() > 180) v = v.substring(v.length() - 180);
        return v;
    }

    private static String extensionOf(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        String ext = filename.substring(idx + 1).trim().toLowerCase();
        if (ext.length() > 8) return "";
        if (!ext.matches("[a-z0-9]+")) return "";
        return ext;
    }

    public static class PayloadTooLargeException extends RuntimeException {
        public PayloadTooLargeException(String message) {
            super(message);
        }
    }
}
