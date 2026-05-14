package com.tcc.pjb.backend.service.prazo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrazoDecursoCertidaoService {

    public record CertidaoDecursoInput(
            UUID processoId,
            UUID prazoId,
            String descricaoPrazo,
            PrazoProcessualEngine.PrazoSnapshot snapshot,
            String servidorId
    ) {}

    public record CertidaoDecurso(
            UUID processoId,
            UUID prazoId,
            String descricaoPrazo,
            boolean aptaParaCertidao,
            List<String> pendencias,
            String textoCertidao,
            Instant geradaEm
    ) {}

    public CertidaoDecurso avaliar(CertidaoDecursoInput input) {
        List<String> pendencias = verificarPendencias(input.snapshot());
        boolean apta = pendencias.isEmpty() && input.snapshot().vencido();
        String texto = apta
                ? montarTextoCertidao(input)
                : "Prazo ainda em curso ou com pendências impeditivas.";
        return new CertidaoDecurso(
                input.processoId(),
                input.prazoId(),
                input.descricaoPrazo(),
                apta,
                pendencias,
                texto,
                Instant.now()
        );
    }

    private List<String> verificarPendencias(PrazoProcessualEngine.PrazoSnapshot s) {
        if (!s.vencido()) {
            return List.of("Prazo ainda não venceu — vencimento em " + s.dataVencimento() + ".");
        }
        if (s.situacao() == PrazoSituacao.SUSPENSO) {
            return List.of("Prazo está suspenso — aguardar retomada.");
        }
        if (s.situacao() == PrazoSituacao.SOBRESTADO) {
            return List.of("Prazo sobrestado — aguardar desobrestamento.");
        }
        return List.of();
    }

    private String montarTextoCertidao(CertidaoDecursoInput input) {
        return "CERTIDÃO DE DECURSO DE PRAZO — " + input.descricaoPrazo()
                + ". Prazo: " + input.snapshot().diasUteisTotal() + " dias úteis."
                + " Vencimento: " + input.snapshot().dataVencimento()
                + ". Certifico o transcurso in albis.";
    }
}
