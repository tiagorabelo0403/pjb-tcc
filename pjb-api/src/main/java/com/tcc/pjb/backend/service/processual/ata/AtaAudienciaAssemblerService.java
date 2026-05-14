package com.tcc.pjb.backend.service.processual.ata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AtaAudienciaAssemblerService {

    public record ParticipanteAudiencia(
            String nome,
            String qualidade,
            String oab,
            boolean presente
    ) {}

    public record AtaInput(
            UUID processoId,
            UUID audienciaId,
            LocalDate dataAudiencia,
            String tipoAudiencia,
            String magistrado,
            String servidorPresente,
            List<ParticipanteAudiencia> participantes,
            String resultado,
            boolean geraAcordao,
            String textoResumo,
            List<String> determinacoes
    ) {}

    public record AtaAudiencia(
            UUID processoId,
            UUID audienciaId,
            UUID ataId,
            LocalDate dataAudiencia,
            String textoAta,
            List<String> determinacoes,
            boolean requerAssinaturaMagistrado,
            Instant geradaEm
    ) {}

    public AtaAudiencia montar(AtaInput input) {
        StringBuilder texto = new StringBuilder();
        texto.append("ATA DE ").append(input.tipoAudiencia().toUpperCase());
        texto.append("\nProcesso: ").append(input.processoId());
        texto.append("\nData: ").append(input.dataAudiencia());
        texto.append("\nMagistrado(a): ").append(input.magistrado());
        texto.append("\nServidor(a): ").append(input.servidorPresente());
        texto.append("\n\nPresentes:");
        for (ParticipanteAudiencia p : input.participantes()) {
            texto.append("\n - ").append(p.nome())
                    .append(" (").append(p.qualidade()).append(")")
                    .append(p.presente() ? "" : " — AUSENTE");
        }
        texto.append("\n\nResultado: ").append(input.resultado());
        if (input.textoResumo() != null && !input.textoResumo().isBlank()) {
            texto.append("\n\n").append(input.textoResumo());
        }
        return new AtaAudiencia(
                input.processoId(), input.audienciaId(),
                UUID.randomUUID(), input.dataAudiencia(),
                texto.toString(), List.copyOf(input.determinacoes()),
                true, Instant.now()
        );
    }
}
