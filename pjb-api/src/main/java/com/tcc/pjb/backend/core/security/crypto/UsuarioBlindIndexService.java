package com.tcc.pjb.backend.core.security.crypto;

import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Índice cego de {@code cpf}/{@code email} de {@link com.tcc.pjb.backend.model.entity.Usuario} —
 * normaliza (mesma forma sempre, independente de como o chamador formatou o valor) e calcula o hash
 * determinístico usado para busca, já que os campos passam a ser criptografados
 * ({@code SensitiveDataConverter}, IV aleatório, não comparável em {@code WHERE}).
 *
 * <p>Único ponto de verdade da normalização: usado pelo {@code @PrePersist}/{@code @PreUpdate} de
 * {@code Usuario} (grava o hash), por {@code UsuarioRepositoryImpl}/{@code ProcessoRepositoryImpl}
 * (busca pelo hash) e pelo backfill — todos precisam concordar exatamente, ou o hash da escrita nunca
 * bate com o hash da busca.</p>
 */
@Service
public class UsuarioBlindIndexService {

    private final CryptoVaultService cryptoVaultService;

    public UsuarioBlindIndexService(CryptoVaultService cryptoVaultService) {
        this.cryptoVaultService = Objects.requireNonNull(cryptoVaultService, "cryptoVaultService");
    }

    public static String normalizarCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    public static String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    public String hashCpf(String cpfBruto) {
        String normalizado = normalizarCpf(cpfBruto);
        return normalizado == null ? null : cryptoVaultService.hmacHex(normalizado);
    }

    public String hashEmail(String emailBruto) {
        String normalizado = normalizarEmail(emailBruto);
        return normalizado == null ? null : cryptoVaultService.hmacHex(normalizado);
    }
}
