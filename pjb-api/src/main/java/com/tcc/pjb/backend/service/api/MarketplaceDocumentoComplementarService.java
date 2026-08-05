package com.tcc.pjb.backend.service.api;

import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.document.DocumentContentValidator;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceDocumentoComplementarService {

    private static final String STATUS_PENDENTE_DOCUMENTACAO = "PENDENTE_DOCUMENTACAO";
    private static final String STATUS_RECEBIDO = "RECEBIDO_MARKETPLACE";

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ObjectStoragePort objectStorage;
    private final DocumentContentValidator contentValidator;
    private final DocumentoSigiloClassifier sigiloClassifier;
    private final CompletudeDocumentalPolicyService completudeDocumentalPolicyService;
    private final MarketplaceGovernanceService governanceService;
    private final AuditLedgerService auditLedger;

    public MarketplaceDocumentoComplementarService(ProcessoRepository processoRepository,
                                                   DocumentoProcessualRepository documentoRepository,
                                                   ObjectStoragePort objectStorage,
                                                   DocumentContentValidator contentValidator,
                                                   DocumentoSigiloClassifier sigiloClassifier,
                                                   CompletudeDocumentalPolicyService completudeDocumentalPolicyService,
                                                   MarketplaceGovernanceService governanceService,
                                                   AuditLedgerService auditLedger) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.objectStorage = Objects.requireNonNull(objectStorage);
        this.contentValidator = Objects.requireNonNull(contentValidator);
        this.sigiloClassifier = Objects.requireNonNull(sigiloClassifier);
        this.completudeDocumentalPolicyService = Objects.requireNonNull(completudeDocumentalPolicyService);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @Transactional
    public MarketplaceComplementoDocumentalResponse complementar(Long processoId, List<Attachment> documentos, String clientId) {
        Processo processo = processoRepository.findById(processoId)
                .filter(p -> p.getConnectorProtocolReference() != null
                        && p.getConnectorProtocolReference().startsWith(clientId + ":"))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado para este cliente."));

        if (!STATUS_PENDENTE_DOCUMENTACAO.equals(processo.getConnectorSubmissionStatus())) {
            throw new RecursoJaExistenteException("Este processo já está com a documentação completa — nenhuma ação necessária.");
        }

        List<String> documentosRecebidos = new ArrayList<>();
        for (Attachment attachment : documentos) {
            if (attachment.getTipoDocumento() == null) {
                throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "tipoDocumento")
                        .addMetadado("motivo", "tipoDocumento obrigatório para cada documento enviado");
            }
            byte[] bytes = attachment.getContent();
            String nome = attachment.getName();
            contentValidator.validarTamanho(bytes == null ? 0 : bytes.length, nome);
            contentValidator.validarExtensaoOuContentType(nome, attachment.getContentType());

            String sha256 = Hashes.sha256HexBytes(bytes);
            if (documentoRepository.existsByProcessoIdAndSha256(processoId, sha256)) {
                continue;
            }

            try (var validado = contentValidator.validarEstruturaPdf(bytes, nome)) {
            } catch (IOException e) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nome, e)
                        .addMetadado("erro_tecnico", e.getClass().getSimpleName());
            }

            var cls = sigiloClassifier.classify(nome, null);
            NivelSigilo procSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
            NivelSigilo sigiloDoc = maxSigilo(procSigilo, cls.minSigilo());

            String key = "marketplace/" + processoId + "/" + UUID.randomUUID();
            try {
                objectStorage.put(key, new ByteArrayInputStream(bytes), bytes.length,
                        attachment.getContentType(), Map.of());
            } catch (IOException e) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nome, e)
                        .addMetadado("erro_tecnico", "falha ao gravar no armazenamento de objetos");
            }

            DocumentoProcessual doc = DocumentoProcessual.builder()
                    .processo(processo)
                    .nomeOriginal(nome)
                    .titulo(nome)
                    .contentType(attachment.getContentType())
                    .tamanhoBytes((long) bytes.length)
                    .sha256(sha256)
                    .storageBackend("LOCALFS")
                    .storageUri(key)
                    .tipoDocumento(attachment.getTipoDocumento())
                    .categoria(cls.suggestedCategoria())
                    .nivelSigilo(sigiloDoc)
                    .origemSistema("MARKETPLACE_API")
                    .criadoEm(LocalDateTime.now())
                    .build();
            documentoRepository.save(doc);
            documentosRecebidos.add(attachment.getTipoDocumento().name());
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

        if (documentacaoCompleta) {
            processo.setConnectorSubmissionStatus(STATUS_RECEBIDO);
            processo.setConnectorSubmissionMessage("Documentação completada via marketplace em " + LocalDateTime.now() + ".");
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
                LocalDateTime.now());
    }

    private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
        NivelSigilo x = a == null ? NivelSigilo.PUBLICO : a;
        NivelSigilo y = b == null ? NivelSigilo.PUBLICO : b;
        return x.getNivel() >= y.getNivel() ? x : y;
    }
}
