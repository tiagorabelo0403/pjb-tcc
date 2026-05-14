package com.tcc.pjb.backend.service.assinatura;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class AssinaturaHashComputationService {

    public record HashResult(
            String algoritmo,
            String hashHex,
            int tamanhoBytes
    ) {}

    public HashResult computarSha256(byte[] conteudo) {
        return computar(conteudo, "SHA-256");
    }

    public HashResult computarSha512(byte[] conteudo) {
        return computar(conteudo, "SHA-512");
    }

    public HashResult computarDeTexto(String texto, String algoritmo) {
        return computar(texto.getBytes(StandardCharsets.UTF_8), algoritmo);
    }

    public boolean verificarIntegridade(byte[] conteudo, String hashEsperadoHex, String algoritmo) {
        HashResult resultado = computar(conteudo, algoritmo);
        return resultado.hashHex().equalsIgnoreCase(hashEsperadoHex);
    }

    private HashResult computar(byte[] conteudo, String algoritmo) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algoritmo);
            byte[] hashBytes = digest.digest(conteudo);
            return new HashResult(algoritmo, HexFormat.of().formatHex(hashBytes), conteudo.length);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algoritmo de hash não suportado: " + algoritmo, e);
        }
    }
}
