package com.tcc.pjb.backend.service;

import com.tcc.pjb.backend.core.security.magistratura.lotacao.MagistradoLotacaoAccessService;
import com.tcc.pjb.backend.model.dto.AcordoHomologadoEvent;
import com.tcc.pjb.backend.model.dto.PdfGenerationResult;
import com.tcc.pjb.backend.model.dto.SavedAudit;
import com.tcc.pjb.backend.model.entity.AcordoHomologado;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AcordoHomologadoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class AcordoHomologacaoJudicialService {

    private final PdfGeneratorService pdfGeneratorService;
    private final AcordoHomologadoRepository acordoHomologadoRepository;
    private final ProcessoRepository processoRepository;
    private final UiHistoryService uiHistoryService;
    private final AuditService auditService;
    private final DomainEventPublisher domainEventPublisher;
    private final MagistradoLotacaoAccessService magistradoLotacaoAccessService;

    @Inject
    AcordoHomologacaoJudicialService(PdfGeneratorService pdfGeneratorService,
                                     AcordoHomologadoRepository acordoHomologadoRepository,
                                     ProcessoRepository processoRepository,
                                     UiHistoryService uiHistoryService,
                                     AuditService auditService,
                                     DomainEventPublisher domainEventPublisher,
                                     MagistradoLotacaoAccessService magistradoLotacaoAccessService) {
        this.pdfGeneratorService = Objects.requireNonNull(pdfGeneratorService);
        this.acordoHomologadoRepository = Objects.requireNonNull(acordoHomologadoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService);
        this.auditService = Objects.requireNonNull(auditService);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
        this.magistradoLotacaoAccessService = Objects.requireNonNull(magistradoLotacaoAccessService);
    }

    void exigirMagistrado() {
        magistradoLotacaoAccessService.requireMagistrado();
    }

    Usuario autorizarMagistrado(Processo processo) {
        return magistradoLotacaoAccessService.requireMagistradoLotadoNaUnidadeDoProcesso(processo);
    }

    Homologacao materializar(PropostaAcordo proposta, Processo processo, Usuario juiz, String hashAssinaturaJuiz) {
        LocalDateTime agora = LocalDateTime.now();
        PdfGenerationResult pdf = pdfGeneratorService.generatePdfWithSealAndQr(
                proposta.getTermosHtml(),
                proposta.getUuid(),
                Map.of(
                        "processo", Objects.toString(processo.getNumeroUnificado(), ""),
                        "juiz", Objects.toString(juiz.getNome(), ""),
                        "data_homologacao", agora.toString()
                )
        );

        AcordoHomologado acordo = AcordoHomologado.builder()
                .proposta(proposta)
                .processo(processo)
                .juiz(juiz)
                .urlPdfHomologado(pdf.getUrl())
                .hashAssinaturaJuiz(hashAssinaturaJuiz)
                .hashAssinaturaParte1(proposta.getHashAssinaturaParte1())
                .hashAssinaturaParte2(proposta.getHashAssinaturaParte2())
                .dataHomologacao(agora)
                .build();

        AcordoHomologado salvo = acordoHomologadoRepository.save(acordo);

        StatusProcesso fromStatus = processo.getStatusProcesso();
        String fromResultado = processo.getResultadoFinal();

        processo.setStatusProcesso(StatusProcesso.JULGADO);
        processo.setResultadoFinal("ACORDO_HOMOLOGADO");
        Processo savedProcesso = processoRepository.save(processo);

        uiHistoryService.recordProcessoStatusChange(
                savedProcesso,
                fromStatus,
                fromResultado,
                StatusProcesso.JULGADO,
                "ACORDO_HOMOLOGADO",
                "Acordo homologado judicialmente"
        );
        return new Homologacao(salvo, pdf);
    }

    void registrar(PropostaAcordo proposta, Processo processo, Usuario juiz, Homologacao homologacao) {
        auditService.recordAgreementHomologation(proposta.getUuid(), new SavedAudit(proposta, homologacao.acordo(), homologacao.pdf()));
        domainEventPublisher.publish(new AcordoHomologadoEvent(processo.getId(), proposta.getUuid(), juiz.getId()));
    }

    record Homologacao(AcordoHomologado acordo, PdfGenerationResult pdf) {
    }
}
