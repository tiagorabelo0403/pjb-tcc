package com.tcc.pjb.backend.service.profile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoDocumentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;

@Service
public class DiligenceCertificateEvidenceService {

    private final DiligenciaOperadorCertidaoRepository certidaoRepository;
    private final DiligenciaOperadorCertidaoDocumentoRepository vinculoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;

    public DiligenceCertificateEvidenceService(DiligenciaOperadorCertidaoRepository certidaoRepository,
                                               DiligenciaOperadorCertidaoDocumentoRepository vinculoRepository,
                                               DocumentoProcessualRepository documentoRepository,
                                               ProcessoRepository processoRepository,
                                               PjbAuthorizationService authorizationService) {
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
        this.vinculoRepository = Objects.requireNonNull(vinculoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional
    public List<DiligenceCertificateDocumentLinkResponse> bind(Long certidaoId,
                                                               TelemetriaOperacionalCanal canal,
                                                               String diligenceReference,
                                                               DiligenceCertificateDocumentLinkRequest request) {
        validateReference(certidaoId, canal, diligenceReference);
        return bind(certidaoId, request);
    }

    @Transactional
    public List<DiligenceCertificateDocumentLinkResponse> bind(Long certidaoId,
                                                               DiligenceCertificateDocumentLinkRequest request) {
        if (request == null || request.documentoIds() == null || request.documentoIds().isEmpty()) {
            throw new IllegalArgumentException("documentos_obrigatorios");
        }
        DiligenciaOperadorCertidao certidao = resolveCertidao(certidaoId);
        Long processoId = certidao.getProcessoId();
        if (processoId == null) {
            throw new IllegalArgumentException("certidao_sem_processo_vinculado");
        }
        Processo processo = resolveProcesso(processoId);
        request.documentoIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(documentoRepository::findById)
                .map(optional -> optional.orElseThrow(() -> new IllegalArgumentException("documento_nao_encontrado")))
                .peek(documento -> validarDocumento(certidao, processoId, processo, documento))
                .filter(documento -> !vinculoRepository.existsByCertidaoIdAndDocumentoId(certidao.getId(), documento.getId()))
                .map(documento -> DiligenciaOperadorCertidaoDocumento.builder()
                        .certidaoId(certidao.getId())
                        .processoId(processoId)
                        .documentoId(documento.getId())
                        .documentoTitulo(resolveTitulo(documento))
                        .documentoSha256(documento.getSha256())
                        .origem("REQUEST")
                        .build())
                .forEach(vinculoRepository::save);
        return list(certidaoId);
    }

    @Transactional(readOnly = true)
    public List<DiligenceCertificateDocumentLinkResponse> list(Long certidaoId,
                                                               TelemetriaOperacionalCanal canal,
                                                               String diligenceReference) {
        validateReference(certidaoId, canal, diligenceReference);
        return list(certidaoId);
    }

    @Transactional(readOnly = true)
    public List<DiligenceCertificateDocumentLinkResponse> list(Long certidaoId) {
        resolveCertidao(certidaoId);
        return vinculoRepository.findByCertidaoIdOrderByCreatedAtDesc(certidaoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DiligenceCertificateDocumentLinkResponse> suggestions(Long certidaoId,
                                                                      TelemetriaOperacionalCanal canal,
                                                                      String diligenceReference,
                                                                      int limit) {
        validateReference(certidaoId, canal, diligenceReference);
        return suggestions(certidaoId, limit);
    }

    @Transactional(readOnly = true)
    public List<DiligenceCertificateDocumentLinkResponse> suggestions(Long certidaoId,
                                                                      int limit) {
        DiligenciaOperadorCertidao certidao = resolveCertidao(certidaoId);
        if (certidao.getProcessoId() == null) {
            return List.of();
        }
        Processo processo = resolveProcesso(certidao.getProcessoId());
        return documentoRepository.findByProcessoId(certidao.getProcessoId()).stream()
                .peek(documento -> authorizationService.requireReadDocumento(processo, documento))
                .filter(documento -> !vinculoRepository.existsByCertidaoIdAndDocumentoId(certidaoId, documento.getId()))
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(documento -> new DiligenceCertificateDocumentLinkResponse(
                        null,
                        certidaoId,
                        certidao.getProcessoId(),
                        documento.getId(),
                        resolveTitulo(documento),
                        documento.getSha256(),
                        "SUGESTAO",
                        documento.getCriadoEm() != null ? documento.getCriadoEm().toInstant(java.time.ZoneOffset.UTC) : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public int count(Long certidaoId) {
        return Math.toIntExact(vinculoRepository.countByCertidaoId(certidaoId));
    }

    private void validateReference(Long certidaoId,
                                   TelemetriaOperacionalCanal canal,
                                   String diligenceReference) {
        DiligenciaOperadorCertidao certidao = resolveCertidao(certidaoId);
        if (canal != null && certidao.getCanal() != canal) {
            throw new IllegalArgumentException("certidao_incompativel_com_canal");
        }
        if (diligenceReference != null && !diligenceReference.isBlank() && !Objects.equals(certidao.getDiligenceReference(), diligenceReference.trim())) {
            throw new IllegalArgumentException("certidao_incompativel_com_diligencia");
        }
    }

    private void validarDocumento(DiligenciaOperadorCertidao certidao,
                                  Long processoId,
                                  Processo processo,
                                  DocumentoProcessual documento) {
        if (documento.getId() == null) {
            throw new IllegalArgumentException("documento_invalido");
        }
        Long documentoProcessoId = documento.getProcesso() != null ? documento.getProcesso().getId() : null;
        if (!Objects.equals(documentoProcessoId, processoId)) {
            throw new IllegalArgumentException("documento_fora_do_processo_da_certidao");
        }
        authorizationService.requireReadDocumento(processo, documento);
        if (!Objects.equals(certidao.getProcessoId(), processoId)) {
            throw new IllegalArgumentException("certidao_incompativel_com_documento");
        }
    }

    private Processo resolveProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("processo_da_certidao_nao_encontrado"));
    }

    private DiligenciaOperadorCertidao resolveCertidao(Long certidaoId) {
        if (certidaoId == null) {
            throw new IllegalArgumentException("certidao_obrigatoria");
        }
        return certidaoRepository.findById(certidaoId)
                .orElseThrow(() -> new IllegalArgumentException("certidao_nao_encontrada"));
    }

    private DiligenceCertificateDocumentLinkResponse toResponse(DiligenciaOperadorCertidaoDocumento vinculo) {
        return new DiligenceCertificateDocumentLinkResponse(
                vinculo.getId(),
                vinculo.getCertidaoId(),
                vinculo.getProcessoId(),
                vinculo.getDocumentoId(),
                vinculo.getDocumentoTitulo(),
                vinculo.getDocumentoSha256(),
                vinculo.getOrigem(),
                vinculo.getCreatedAt()
        );
    }

    private String resolveTitulo(DocumentoProcessual documento) {
        String value = documento.getTitulo();
        if (value == null || value.isBlank()) {
            value = documento.getNomeOriginal();
        }
        if (value == null || value.isBlank()) {
            return documento.getId() != null ? documento.getId().toString() : UUID.randomUUID().toString();
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
