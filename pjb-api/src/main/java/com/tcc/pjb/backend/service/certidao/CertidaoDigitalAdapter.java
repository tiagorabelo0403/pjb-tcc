package com.tcc.pjb.backend.service.certidao;

import com.tcc.pjb.backend.core.certidao.CertidaoDigital;
import com.tcc.pjb.backend.core.certidao.CertidaoDigitalPort;
import com.tcc.pjb.backend.core.certidao.CertidaoRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CertidaoDigitalAdapter implements CertidaoDigitalPort {

    private final Map<String, CertidaoDigital> emitidas = new ConcurrentHashMap<>();

    @Override
    public CertidaoDigital emitir(CertidaoRequest request) {
        Objects.requireNonNull(request, "request");
        String codigoVerificacao = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String conteudo = buildConteudo(request);
        String hashSha256 = sha256Hex(conteudo.getBytes(StandardCharsets.UTF_8));
        CertidaoDigital certidao = new CertidaoDigital(
                codigoVerificacao,
                request.tipo(),
                request.processoId(),
                conteudo,
                new byte[0],
                hashSha256,
                Instant.now(),
                Instant.now().plus(365, ChronoUnit.DAYS)
        );
        emitidas.put(codigoVerificacao, certidao);
        return certidao;
    }

    @Override
    public boolean verificar(String codigoVerificacao) {
        if (codigoVerificacao == null || codigoVerificacao.isBlank()) {
            return false;
        }
        CertidaoDigital certidao = emitidas.get(codigoVerificacao.toUpperCase());
        if (certidao == null) {
            return false;
        }
        return certidao.validaAte() == null || Instant.now().isBefore(certidao.validaAte());
    }

    private String buildConteudo(CertidaoRequest request) {
        return String.format(
                "CERTIDÃO JUDICIAL — TIPO: %s%nPROCESSO: %s%nSOLICITANTE: %s%nFINALIDADE: %s%nEMISSÃO: %s",
                request.tipo().name(),
                request.processoId(),
                request.solicitanteNome() != null ? request.solicitanteNome() : request.solicitanteCpfCnpj(),
                request.finalidade() != null ? request.finalidade() : "NÃO INFORMADA",
                Instant.now()
        );
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            return "HASH_INDISPONIVEL";
        }
    }
}
