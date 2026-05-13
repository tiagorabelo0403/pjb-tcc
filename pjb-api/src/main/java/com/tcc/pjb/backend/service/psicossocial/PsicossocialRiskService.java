package com.tcc.pjb.backend.service.psicossocial;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class PsicossocialRiskService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;

    public PsicossocialRiskService(ProcessoRepository processoRepository,
                                   WorkItemRepository workItemRepository,
                                   InstitutionalActorRoutingService institutionalActorRoutingService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
    }

    @Transactional
    public RiscoPsicossocialReport analisar(AnaliseLaudoRequest request) {
        Objects.requireNonNull(request);
        String texto = normalize(request.textoLaudo());
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        List<String> indicadores = new ArrayList<>();

        detectar(texto, flags, indicadores, "FEMINICIDIO", List.of("AMEACA DE MORTE", "ESCALADA DE VIOLENCIA", "TENTATIVA DE ESTRANGULAMENTO", "ARMA DE FOGO"));
        detectar(texto, flags, indicadores, "ABUSO_INFANTIL", List.of("ABUSO SEXUAL", "TOQUES INTIMOS", "CRIANCA RELATA MEDO", "LESAO EM MENOR"));
        detectar(texto, flags, indicadores, "ALIENACAO_PARENTAL", List.of("DESQUALIFICACAO CONSTANTE", "IMPEDIMENTO DE CONVIVENCIA", "FALSA MEMORIA", "REJEICAO INDUZIDA"));
        detectar(texto, flags, indicadores, "SAP", List.of("SINDROME DA ALIENACAO", "SAP"));
        detectar(texto, flags, indicadores, "AUTOEXTERMINIO", List.of("IDEACAO SUICIDA", "RISCO DE SUICIDIO", "AUTOEXTERMINIO"));

        int score = 0;
        if (flags.contains("FEMINICIDIO")) {
            score += 45;
        }
        if (flags.contains("ABUSO_INFANTIL")) {
            score += 40;
        }
        if (flags.contains("ALIENACAO_PARENTAL")) {
            score += 18;
        }
        if (flags.contains("SAP")) {
            score += 12;
        }
        if (flags.contains("AUTOEXTERMINIO")) {
            score += 30;
        }
        boolean riscoImediato = score >= 70;
        String urgencia = riscoImediato ? "IMEDIATA" : score >= 40 ? "ALTA" : score >= 20 ? "MEDIA" : "BAIXA";
        int prioridade = riscoImediato ? 0 : 1;
        long prazoHoras = riscoImediato ? 4 : 24;
        Instant analisadoEm = Instant.now();

        Long workItemId = null;
        if (request.processoId() != null && !flags.isEmpty()) {
            Processo processo = processoRepository.findById(request.processoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
            String templateCode = "PSICOSSOCIAL_RISCO:" + request.processoId() + ":" + flags.hashCode();
            InstitutionalActorRoutingService.InstitutionalRoute riskRoute = institutionalActorRoutingService.gabineteReview(processo.getId(), "RISCO_PSICOSSOCIAL");
            workItemId = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO)
                    .map(WorkItem::getId)
                    .orElseGet(() -> {
                        WorkItem item = WorkItem.builder()
                                .processo(processo)
                                .faseOrigem(processo.getFaseAtual())
                                .templateCode(templateCode)
                                .type(WorkItemType.LAUDO)
                                .titulo("Risco psicossocial detectado — " + urgencia + " — " + processo.getNumeroProcesso())
                                .descricao(String.join(" | ", flags) + " | " + String.join(" | ", indicadores))
                                .queueCode(riskRoute.queueCode())
                                .inboxKey(riskRoute.inboxKey())
                                .assignedRole(riskRoute.assignedRole())
                                .status(WorkItemStatus.PENDENTE)
                                .prioridade(prioridade)
                                .uf(processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : null)
                                .comarca(processo.getJurisdicao() != null ? processo.getJurisdicao().getComarca() : null)
                                .baseLegal("Analise psicossocial assistida por IA do PJB")
                                .dueAt(analisadoEm.plus(prazoHoras, ChronoUnit.HOURS))
                                .build();
                        return workItemRepository.save(item).getId();
                    });
        }

        return new RiscoPsicossocialReport(
                request.processoId(),
                score,
                urgencia,
                List.copyOf(flags),
                List.copyOf(indicadores),
                workItemId,
                analisadoEm
        );
    }

    private void detectar(String texto, Set<String> flags, List<String> indicadores, String flag, List<String> gatilhos) {
        for (String gatilho : gatilhos) {
            if (texto.contains(gatilho)) {
                flags.add(flag);
                indicadores.add(gatilho);
            }
        }
    }

    private String normalize(String texto) {
        return texto == null ? "" : texto.toUpperCase(Locale.ROOT).replace('\n', ' ');
    }

    public record AnaliseLaudoRequest(Long processoId, String textoLaudo) {
    }

    public record RiscoPsicossocialReport(
            Long processoId,
            int scoreRisco,
            String urgencia,
            List<String> flags,
            List<String> indicadores,
            Long workItemId,
            Instant analisadoEm
    ) {
    }
}
