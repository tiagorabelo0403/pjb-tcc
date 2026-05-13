package com.tcc.pjb.backend.service.juiz;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.crypto.quantum.PjbQuantumProperties;
import com.tcc.pjb.backend.core.security.crypto.quantum.PostQuantumSigner;
import com.tcc.pjb.backend.core.security.crypto.quantum.PqcEvidence;
import com.tcc.pjb.backend.model.dto.juiz.CertidaoTJResponse;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;

@Service
public class CertidaoTransitoJulgadoService {

    private final ProcessoRepository processoRepository;
    private final OfficialDocumentTemplateService officialDocumentTemplateService;
    private final CurrentUserService currentUserService;
    private final PjbQuantumProperties quantumProperties;

    public CertidaoTransitoJulgadoService(ProcessoRepository processoRepository,
                                          OfficialDocumentTemplateService officialDocumentTemplateService,
                                          CurrentUserService currentUserService,
                                          PjbQuantumProperties quantumProperties) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.quantumProperties = Objects.requireNonNull(quantumProperties);
    }

    @Transactional
    public CertidaoTJResponse gerarAutomatica(Long processoId) {
        if (processoId == null) {
            throw new IllegalArgumentException("processoId é obrigatório");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario usuario = currentUserService.getRequired();

        LinkedHashMap<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("dataTransito", LocalDate.now().toString());
        variaveis.put("responsavelCertificacao", usuario.getNome() != null ? usuario.getNome() : "AUTORIDADE_JUDICIAL");
        variaveis.put("fatoCertificado", "Certifico o trânsito em julgado do processo "+ processo.getNumeroProcesso());
        variaveis.put("numeroProcesso", processo.getNumeroProcesso());
        variaveis.put("classeProcessual", processo.getClasseProcessual());
        variaveis.put("assunto", processo.getAssunto());

        OfficialDocumentTemplateRenderResponse render = officialDocumentTemplateService.renderizar(
                new OfficialDocumentTemplateRenderRequest(
                        processo.getId(),
                        TemplateDocumentoOficial.CERTIDAO_TRANSITO_JULGADO,
                        null,
                        Map.copyOf(variaveis),
                        Boolean.TRUE,
                        Boolean.TRUE
                )
        );

        PqcEvidence evidence = null;
        if (quantumProperties.enabled()) {
            PostQuantumSigner signer = new PostQuantumSigner(quantumProperties.signatureAlgorithm());
            evidence = signer.sign(render.conteudoRenderizado().getBytes(StandardCharsets.UTF_8));
        }

        return new CertidaoTJResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                render.documentoId(),
                render.hashSha256(),
                evidence != null ? evidence.algorithm() : null,
                evidence != null ? evidence.signatureB64() : null,
                evidence != null ? evidence.publicKeyB64() : null,
                Instant.now(),
                render.assinaturaQualificada(),
                render.validacaoSoberana()
        );
    }
}
