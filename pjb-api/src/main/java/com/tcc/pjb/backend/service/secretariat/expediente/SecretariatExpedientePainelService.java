package com.tcc.pjb.backend.service.secretariat.expediente;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SecretariatExpedientePainelService {

    public record PainelExpedienteSnapshot(
            int totalPendentes,
            int totalSemCiencia,
            int totalPrazoEncerrado,
            int totalUrgentes,
            Map<String, Long> contagemPorSituacao,
            List<String> alertas
    ) {}

    private final SecretariatExpedienteDestaqueService destaqueService;

    public SecretariatExpedientePainelService(SecretariatExpedienteDestaqueService destaqueService) {
        this.destaqueService = destaqueService;
    }

    public List<SecretariatQueuePanelItemDto> filtrarExpedientesPendentes(
            List<SecretariatQueuePanelItemDto> fila) {
        return fila.stream()
                .filter(item -> item.tags() != null
                        && item.tags().contains("EXPEDIENTE_PENDENTE"))
                .toList();
    }

    public PainelExpedienteSnapshot construirPainel(
            List<SecretariatExpedienteDestaqueService.ExpedienteItem> expedientes) {
        List<SecretariatExpedienteDestaqueService.ExpedienteDestaque> destaques =
                destaqueService.identificarDestaques(expedientes);

        long pendentes = destaques.stream()
                .filter(d -> d.situacao() == SecretariatExpedienteSituacao.PENDENTE).count();
        long semCiencia = destaques.stream()
                .filter(d -> d.situacao() == SecretariatExpedienteSituacao.SEM_REGISTRO_INTIMACAO).count();
        long prazoEncerrado = destaques.stream()
                .filter(d -> d.situacao() == SecretariatExpedienteSituacao.PRAZO_ENCERRADO).count();
        long urgentes = destaques.stream().filter(SecretariatExpedienteDestaqueService.ExpedienteDestaque::urgente).count();

        Map<String, Long> contagem = destaques.stream()
                .collect(Collectors.groupingBy(d -> d.situacao().name(), Collectors.counting()));

        List<String> alertas = destaques.stream()
                .filter(d -> d.urgente() || d.situacao() == SecretariatExpedienteSituacao.PRAZO_ENCERRADO)
                .map(d -> d.processoNumero() + ": " + d.mensagem())
                .toList();

        return new PainelExpedienteSnapshot(
                (int) pendentes, (int) semCiencia, (int) prazoEncerrado, (int) urgentes, contagem, alertas);
    }
}
