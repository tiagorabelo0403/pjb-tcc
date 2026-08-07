package com.tcc.pjb.backend.service.api;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceDocumentoComplementarService {

    private static final String STATUS_PENDENTE_DOCUMENTACAO = "PENDENTE_DOCUMENTACAO";
    private static final String STATUS_RECEBIDO = "RECEBIDO_MARKETPLACE";

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final MarketplaceDocumentoPersistenceService documentoPersistenceService;
    private final CompletudeDocumentalPolicyService completudeDocumentalPolicyService;
    private final MarketplaceGovernanceService governanceService;
    private final AuditLedgerService auditLedger;

    public MarketplaceDocumentoComplementarService(ProcessoRepository processoRepository,
                                                   DocumentoProcessualRepository documentoRepository,
                                                   MarketplaceDocumentoPersistenceService documentoPersistenceService,
                                                   CompletudeDocumentalPolicyService completudeDocumentalPolicyService,
                                                   MarketplaceGovernanceService governanceService,
                                                   AuditLedgerService auditLedger) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.documentoPersistenceService = Objects.requireNonNull(documentoPersistenceService);
        this.completudeDocumentalPolicyService = Objects.requireNonNull(completudeDocumentalPolicyService);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @Transactional
    public MarketplaceComplementoDocumentalResponse complementar(Long processoId, List<Attachment> documentos, String clientId) {
        Processo processo = processoRepository.findById(processoId)
                .filter(p -> clientId != null && clientId.equals(p.getConnectorClientId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado para este cliente."));

        if (!STATUS_PENDENTE_DOCUMENTACAO.equals(processo.getConnectorSubmissionStatus())) {
            throw new RecursoJaExistenteException("Este processo já está com a documentação completa — nenhuma ação necessária.");
        }

        governanceService.assertClientHasActiveSubscription(clientId);

        List<String> documentosRecebidos = new ArrayList<>();
        for (Attachment attachment : documentos) {
            documentoPersistenceService.persistirSeNovo(processo, attachment, false)
                    .ifPresent(documentosRecebidos::add);
        }

        Set<TipoDocumento> tiposPresentes = EnumSet.noneOf(TipoDocumento.class);
        for (DocumentoProcessual d : documentoRepository.findByProcessoId(processoId)) {
            if (d.getTipoDocumento() != null) {
                tiposPresentes.add(d.getTipoDocumento());
            }
        }

        InstrumentoRepresentacaoProcessual instrumento =
                InstrumentoRepresentacaoProcessual.fromString(processo.getInstrumentoRepresentacaoResolvido());
        var diagnostico = completudeDocumentalPolicyService.diagnosticar(processo.getRito(), tiposPresentes, instrumento);
        List<String> documentosFaltantes = diagnostico.faltantes().stream().map(Enum::name).toList();
        boolean documentacaoCompleta = !diagnostico.bloqueante();

        LocalDateTime agora = LocalDateTime.now();
        if (documentacaoCompleta) {
            processo.setConnectorSubmissionStatus(STATUS_RECEBIDO);
            processo.setConnectorSubmissionMessage("Documentação completada via marketplace em " + agora + ".");
            governanceService.publicarEventoDocumentacaoCompletada(clientId, processo.getId(), processo.getNumeroProcesso(),
                    processo.getConnectorProtocolReference());
        } else {
            processo.setConnectorSubmissionMessage(
                    "Protocolo recebido via marketplace, pendente de documentacao obrigatoria: " + documentosFaltantes);
            governanceService.publicarEventoPendenciaDocumental(clientId, processo.getId(), processo.getNumeroProcesso(),
                    processo.getConnectorProtocolReference(), documentosFaltantes);
        }
        processoRepository.save(processo);

        auditLedger.appendSafely("MARKETPLACE_DOCUMENTOS_COMPLEMENTADOS", "PROCESSO", String.valueOf(processoId), null,
                "cliente=" + clientId + " recebidos=" + documentosRecebidos.size() + " completo=" + documentacaoCompleta);

        return new MarketplaceComplementoDocumentalResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getConnectorSubmissionStatus(),
                documentacaoCompleta,
                documentosFaltantes,
                documentosRecebidos,
                agora);
    }
}
