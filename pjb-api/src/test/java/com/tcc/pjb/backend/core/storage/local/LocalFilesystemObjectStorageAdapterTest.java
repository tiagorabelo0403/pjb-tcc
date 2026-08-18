package com.tcc.pjb.backend.core.storage.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilesystemObjectStorageAdapterTest {

    @TempDir
    Path tmpDir;

    @Test
    void get_jpeg_retornaContentTypeCorreto() throws IOException {
        LocalFilesystemObjectStorageAdapter adapter = new LocalFilesystemObjectStorageAdapter(
                tmpDir, URI.create("http://localhost:8080"));

        byte[] jpegMagic = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        adapter.put("images/foto.jpg", new ByteArrayInputStream(jpegMagic), jpegMagic.length, "image/jpeg", java.util.Map.of());

        ObjectReadResult result = adapter.get("images/foto.jpg");

        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void get_png_retornaContentTypeCorreto() throws IOException {
        LocalFilesystemObjectStorageAdapter adapter = new LocalFilesystemObjectStorageAdapter(
                tmpDir, URI.create("http://localhost:8080"));

        byte[] pngMagic = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 0, 0, 0, 0, 0};
        adapter.put("images/logo.png", new ByteArrayInputStream(pngMagic), pngMagic.length, "image/png", java.util.Map.of());

        ObjectReadResult result = adapter.get("images/logo.png");

        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void get_pdf_retornaContentTypeCorreto() throws IOException {
        LocalFilesystemObjectStorageAdapter adapter = new LocalFilesystemObjectStorageAdapter(
                tmpDir, URI.create("http://localhost:8080"));

        byte[] pdfMagic = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34,
                0, 0, 0, 0, 0, 0, 0, 0};
        adapter.put("docs/sentenca.pdf", new ByteArrayInputStream(pdfMagic), pdfMagic.length, "application/pdf", java.util.Map.of());

        ObjectReadResult result = adapter.get("docs/sentenca.pdf");

        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void get_webp_retornaContentTypeCorreto() throws IOException {
        LocalFilesystemObjectStorageAdapter adapter = new LocalFilesystemObjectStorageAdapter(
                tmpDir, URI.create("http://localhost:8080"));

        byte[] webpMagic = new byte[]{
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50,
                0, 0, 0, 0};
        adapter.put("images/banner.webp", new ByteArrayInputStream(webpMagic), webpMagic.length, "image/webp", java.util.Map.of());

        ObjectReadResult result = adapter.get("images/banner.webp");

        assertThat(result.contentType()).isEqualTo("image/webp");
    }

    @Test
    void get_extensaoFallback_quandoMagicBytesInconclusive() throws IOException {
        LocalFilesystemObjectStorageAdapter adapter = new LocalFilesystemObjectStorageAdapter(
                tmpDir, URI.create("http://localhost:8080"));

        byte[] randomBytes = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        adapter.put("avatar/user42.jpg", new ByteArrayInputStream(randomBytes), randomBytes.length, "image/jpeg", java.util.Map.of());

        ObjectReadResult result = adapter.get("avatar/user42.jpg");

        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void get_naoRetornaMaisApplicationPdfParaImagens() throws IOException {
        LocalFilesystemObjectStorageAdapter adapter = new LocalFilesystemObjectStorageAdapter(
                tmpDir, URI.create("http://localhost:8080"));

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        adapter.put("avatars/user/88/avatar.jpg", new ByteArrayInputStream(jpegBytes), jpegBytes.length, "image/jpeg", java.util.Map.of());

        ObjectReadResult result = adapter.get("avatars/user/88/avatar.jpg");

        assertThat(result.contentType()).isNotEqualTo("application/pdf");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void detectContentType_staticMethod_jpeg() throws Exception {
        Path f = tmpDir.resolve("test.jpg");
        Files.write(f, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        assertThat(LocalFilesystemObjectStorageAdapter.detectContentType(f, "test.jpg")).isEqualTo("image/jpeg");
    }

    @Test
    void detectContentType_staticMethod_png() throws Exception {
        Path f = tmpDir.resolve("test.png");
        Files.write(f, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0, 0, 0, 0});

        assertThat(LocalFilesystemObjectStorageAdapter.detectContentType(f, "test.png")).isEqualTo("image/png");
    }
}
