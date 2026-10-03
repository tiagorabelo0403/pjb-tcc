package com.tcc.pjb.backend.service.profile;

import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DiligenceFormalizationRecordResolver {

    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final DiligenciaOperadorCertidaoRepository certidaoRepository;

    public DiligenceFormalizationRecordResolver(DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                                DiligenciaOperadorCertidaoRepository certidaoRepository) {
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
    }

    public DiligenciaOperadorEncerramento resolveEncerramento(Usuario actor,
                                                              TelemetriaOperacionalCanal canal,
                                                              String diligenceReference,
                                                              DiligenceProcessFormalizationRequest request) {
        if (request != null && request.encerramentoId() != null) {
            return encerramentoRepository.findById(request.encerramentoId())
                    .orElseThrow(() -> new IllegalArgumentException("encerramento_operacional_nao_encontrado"));
        }
        return encerramentoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("encerramento_operacional_obrigatorio"));
    }

    public DiligenciaOperadorCertidao resolveCertidao(Usuario actor,
                                                      TelemetriaOperacionalCanal canal,
                                                      String diligenceReference,
                                                      DiligenceProcessFormalizationRequest request,
                                                      DiligenciaOperadorEncerramento encerramento) {
        Long certidaoId = request != null && request.certidaoId() != null ? request.certidaoId() : encerramento.getCertidaoId();
        if (certidaoId != null) {
            return certidaoRepository.findById(certidaoId)
                    .orElseThrow(() -> new IllegalArgumentException("certidao_nao_encontrada"));
        }
        return certidaoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("certidao_operacional_obrigatoria"));
    }
}
