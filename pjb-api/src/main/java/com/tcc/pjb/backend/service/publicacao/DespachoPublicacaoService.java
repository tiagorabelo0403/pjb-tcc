package com.tcc.pjb.backend.service.publicacao;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DespachoPublicacaoService {

    public record DespachoInput(
            UUID processoId,
            UUID documentoId,
            RitoProcessual rito,
            String tipoDespacho,
            String textoResumo,
            MeioPublicacao meioPublicacao,
            boolean geraIntimacao,
            boolean geraContagem,
            int diasPrazoIntimado
    ) {}

    public record DespachoPublicado(
            UUID processoId,
            UUID documentoId,
            UUID publicacaoId,
            MeioPublicacao meio,
            Instant publicadoEm,
            Instant intimacaoDisponibilizadaEm,
            boolean geraContagem,
            String status
    ) {}

    private final PublicacaoReadinessService readinessService;

    public DespachoPublicacaoService(PublicacaoReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    public DespachoPublicado publicar(DespachoInput input,
            PublicacaoReadinessService.PublicacaoInput readiness) {
        PublicacaoReadinessService.PublicacaoReadiness check =
                readinessService.avaliar(readiness);
        if (!check.apta()) {
            return new DespachoPublicado(
                    input.processoId(), input.documentoId(), null,
                    input.meioPublicacao(), null, null,
                    false, "BLOQUEADO: " + String.join("; ", check.bloqueios()));
        }
        Instant agora = Instant.now();
        Instant disponivel = input.meioPublicacao().eEletronico()
                ? agora : agora.plusSeconds(86400);
        return new DespachoPublicado(
                input.processoId(), input.documentoId(),
                UUID.randomUUID(), input.meioPublicacao(),
                agora, disponivel, input.geraContagem(), "PUBLICADO"
        );
    }
}
