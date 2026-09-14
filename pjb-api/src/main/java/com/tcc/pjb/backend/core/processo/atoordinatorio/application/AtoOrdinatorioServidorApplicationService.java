package com.tcc.pjb.backend.core.processo.atoordinatorio.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.atoordinatorio.AtoOrdinatorioResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.TipoAtoOrdinatorio;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.governance.DocumentTrustChainService;
import com.tcc.pjb.backend.service.institutional.movimentacao.MovimentacaoProcessualRegistrar;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtoOrdinatorioServidorApplicationService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private final MovimentacaoProcessualRegistrar movimentacaoProcessualRegistrar;
    private final DocumentTrustChainService documentTrustChainService;
    private final ObjectMapper objectMapper;

    public AtoOrdinatorioServidorApplicationService(
            ProcessoRepository processoRepository,
            DocumentoProcessualRepository documentoProcessualRepository,
            CurrentUserService currentUserService,
            PjbAuthorizationService authorizationService,
            QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService,
            MovimentacaoProcessualRegistrar movimentacaoProcessualRegistrar,
            DocumentTrustChainService documentTrustChainService,
            ObjectMapper objectMapper) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
        this.movimentacaoProcessualRegistrar = Objects.requireNonNull(movimentacaoProcessualRegistrar);
        this.documentTrustChainService = Objects.requireNonNull(documentTrustChainService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public AtoOrdinatorioResponse proferir(Long processoId, TipoAtoOrdinatorio tipo, String complemento) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.PROFERIR);

        Usuario servidor = currentUserService.getRequired();
        String titulo = tipo.label() + " — " + processo.getNumeroProcesso();
        String conteudo = montarConteudo(tipo, processo, complemento);

        SignedDocumentEnvelope envelope = qualifiedDocumentSignatureEnvelopeService.signGovernedContent(
                processo,
                servidor,
                titulo,
                conteudo,
                "UNIDADE_JUDICIAL",
                "ATO_ORDINATORIO_QUALIFICADA_SOBERANA",
                false,
                List.of("ato_ordinatorio", tipo.name().toLowerCase(Locale.ROOT)));

        String conteudoAssinado = envelope.renderedContent();
        String hash = envelope.contentHash();

        DocumentoCategoria categoria = processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()
                ? DocumentoCategoria.PESSOAL
                : DocumentoCategoria.PUBLICO;

        DocumentoProcessual documento = new DocumentoProcessual();
        documento.setProcesso(processo);
        documento.setNomeOriginal(slug(titulo) + ".txt");
        documento.setTitulo(titulo);
        documento.setContentType("text/plain; charset=UTF-8");
        documento.setTamanhoBytes((long) conteudoAssinado.getBytes(StandardCharsets.UTF_8).length);
        documento.setSha256(hash);
        documento.setStorageBackend("INLINE_DB");
        documento.setOrigemSistema("PJB_ATO_ORDINATORIO_SERVIDOR");
        documento.setCriadoPor(servidor.getId());
        documento.setCriadoEm(LocalDateTime.now());
        documento.setNivelSigilo(processo.getNivelSigilo());
        documento.setCategoria(categoria);
        documento.setPdf(conteudoAssinado.getBytes(StandardCharsets.UTF_8));
        documento = documentoProcessualRepository.save(documento);

        documentTrustChainService.selar(
                processo.getId(),
                documento.getId(),
                "ATO_ORDINATORIO_SERVIDOR",
                "Ato ordinatório de mero expediente assinado pelo servidor: " + tipo.label(),
                false,
                true,
                "ATO_ORDINATORIO_QUALIFICADA_SOBERANA");

        MovimentacaoProcessual movimentacao = movimentacaoProcessualRegistrar.registrar(
                processo,
                servidor,
                processo.getFaseAtual(),
                "Ato ordinatório de mero expediente: " + tipo.label() + " — documento " + documento.getId());

        return new AtoOrdinatorioResponse(
                documento.getId(),
                movimentacao.getId(),
                tipo,
                hash,
                objectMapper.convertValue(envelope.assinaturaQualificada(), MAP_TYPE),
                objectMapper.convertValue(envelope.validacaoSoberana(), MAP_TYPE));
    }

    private String montarConteudo(TipoAtoOrdinatorio tipo, Processo processo, String complemento) {
        String ln = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append(tipo.label()).append(ln).append(ln);
        builder.append("Processo: ").append(processo.getNumeroProcesso()).append(ln);
        builder.append("Fundamento legal: ").append(tipo.fundamentoLegal()).append(ln);
        if (complemento != null && !complemento.isBlank()) {
            builder.append(ln).append(complemento.trim()).append(ln);
        }
        return builder.toString();
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
}
