package com.tcc.pjb.backend.core.digitalizacao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import org.springframework.util.StringUtils;

public class TesseractOcrEngineAdapter implements OcrEnginePort {

    private final DigitalizacaoProperties properties;

    public TesseractOcrEngineAdapter(DigitalizacaoProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public OcrPageResult processar(byte[] imagem, String idioma) {
        if (imagem == null || imagem.length == 0) {
            return new OcrPageResult("", 0.0d);
        }
        String executable = resolveExecutable();
        String resolvedIdioma = StringUtils.hasText(idioma) ? idioma : defaultIdioma();
        Path input = null;
        Path outputBase = null;
        try {
            input = Files.createTempFile("pjb-ocr-", ".img");
            outputBase = Files.createTempFile("pjb-ocr-out-", "");
            Files.write(input, imagem);
            Files.deleteIfExists(outputBase);
            Process process = new ProcessBuilder(executable, input.toString(), outputBase.toString(), "-l", resolvedIdioma)
                    .redirectErrorStream(true)
                    .start();
            String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            Path txt = Path.of(outputBase.toString() + ".txt");
            String texto = Files.exists(txt) ? Files.readString(txt, StandardCharsets.UTF_8) : "";
            double confianca = exit == 0 ? inferConfidence(texto, log) : 0.0d;
            return new OcrPageResult(texto, confianca);
        } catch (Exception e) {
            String textoFallback = new String(imagem, 0, Math.min(imagem.length, 256), StandardCharsets.ISO_8859_1);
            return new OcrPageResult(textoFallback, 10.0d);
        } finally {
            safeDelete(input);
            if (outputBase != null) {
                safeDelete(outputBase);
                safeDelete(Path.of(outputBase.toString() + ".txt"));
            }
        }
    }

    private String resolveExecutable() {
        String configured = properties.tesseractExecutable();
        return StringUtils.hasText(configured) ? configured : "tesseract";
    }

    private String defaultIdioma() {
        return StringUtils.hasText(properties.idiomaDefault()) ? properties.idiomaDefault() : "por";
    }

    private double inferConfidence(String texto, String log) {
        if (!StringUtils.hasText(texto)) {
            return 0.0d;
        }
        String normalizedLog = log == null ? "" : log.toLowerCase(Locale.ROOT);
        if (normalizedLog.contains("error") || normalizedLog.contains("failed")) {
            return 35.0d;
        }
        int length = texto.replaceAll("\\s+", "").length();
        if (length > 400) {
            return 92.0d;
        }
        if (length > 120) {
            return 84.0d;
        }
        if (length > 40) {
            return 72.0d;
        }
        return 55.0d;
    }

    private void safeDelete(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
