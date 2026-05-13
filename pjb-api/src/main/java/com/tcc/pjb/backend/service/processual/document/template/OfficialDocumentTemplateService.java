package com.tcc.pjb.backend.service.processual.document.template;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.governance.DocumentTrustChainService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;

@Service
public class OfficialDocumentTemplateService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final DocumentTrustChainService documentTrustChainService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public OfficialDocumentTemplateService(ProcessoRepository processoRepository,
                                           DocumentoProcessualRepository documentoProcessualRepository,
                                           CurrentUserService currentUserService,
                                           PjbAuthorizationService authorizationService,
                                           DocumentTrustChainService documentTrustChainService,
                                           QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.documentTrustChainService = Objects.requireNonNull(documentTrustChainService);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    @Transactional
    public OfficialDocumentTemplateRenderResponse renderizar(OfficialDocumentTemplateRenderRequest request) {
        Objects.requireNonNull(request);
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        Usuario usuario = currentUserService.getRequired();
        Map<String, String> variaveis = request.variaveis() == null ? Map.of() : request.variaveis();
        List<String> ausentes = request.template().variaveisObrigatorias().stream()
                .filter(chave -> !variaveis.containsKey(chave) || variaveis.get(chave) == null || variaveis.get(chave).isBlank())
                .toList();
        List<String> alertas = new ArrayList<>();
        if (!ausentes.isEmpty()) {
            alertas.add("Variáveis obrigatórias ausentes impedem formalização integral do documento.");
        }
        String titulo = request.tituloCustomizado() == null || request.tituloCustomizado().isBlank()
                ? request.template().tituloPadrao() + " — " + processo.getNumeroProcesso()
                : request.tituloCustomizado().trim();
        String conteudo = montarConteudo(request.template(), processo, usuario, titulo, variaveis);
        QualifiedDocumentSignatureEnvelopeService.SignedContent signedContent = qualifiedDocumentSignatureEnvelopeService.signOfficialTemplate(
                processo,
                usuario,
                request.template(),
                titulo,
                conteudo,
                true
        );
        conteudo = signedContent.renderedContent();
        String hash = signedContent.contentHash();
        DocumentoCategoria categoria = processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()
                ? DocumentoCategoria.PESSOAL
                : DocumentoCategoria.PUBLICO;
        DocumentoProcessual documento = null;
        boolean persistido = Boolean.TRUE.equals(request.persistir()) && ausentes.isEmpty();
        boolean selado = false;
        if (persistido) {
            documento = new DocumentoProcessual();
            documento.setProcesso(processo);
            documento.setNomeOriginal(slug(titulo) + ".txt");
            documento.setTitulo(titulo);
            documento.setContentType("text/plain; charset=UTF-8");
            documento.setTamanhoBytes((long) conteudo.getBytes(StandardCharsets.UTF_8).length);
            documento.setSha256(hash);
            documento.setStorageBackend("INLINE_DB");
            documento.setOrigemSistema("PJB_TEMPLATE_ENGINE_ASSINATURA_TRANSVERSAL");
            documento.setCriadoPor(usuario.getId());
            documento.setCriadoEm(LocalDateTime.now());
            documento.setNivelSigilo(processo.getNivelSigilo());
            documento.setCategoria(categoria);
            documento.setPdf(conteudo.getBytes(StandardCharsets.UTF_8));
            documento = documentoProcessualRepository.save(documento);
            if (Boolean.TRUE.equals(request.selarCadeiaConfianca())) {
                documentTrustChainService.selar(processo.getId(), documento.getId(), "TEMPLATE_DOC_OFICIAL", "Documento oficial parametrizado com assinatura qualificada completa", false, true, "QUALIFICADA_SOBERANA");
                selado = true;
            }
        }
        return new OfficialDocumentTemplateRenderResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                request.template(),
                titulo,
                request.template().variaveisObrigatorias(),
                ausentes,
                conteudo,
                hash,
                documento == null ? null : documento.getId(),
                categoria,
                processo.getNivelSigilo(),
                persistido,
                selado,
                List.copyOf(alertas),
                signedContent.assinaturaQualificada(),
                signedContent.validacaoSoberana()
        );
    }

    private String montarConteudo(TemplateDocumentoOficial template,
                                  Processo processo,
                                  Usuario usuario,
                                  String titulo,
                                  Map<String, String> variaveis) {
        String ln = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append(titulo).append(ln).append(ln);
        builder.append("Processo: ").append(processo.getNumeroProcesso()).append(ln);
        builder.append("Classe: ").append(safe(processo.getClasseProcessual())).append(ln);
        builder.append("Assunto: ").append(safe(processo.getAssunto())).append(ln);
        builder.append("Rito: ").append(processo.getRito() == null ? "NAO_INFORMADO" : processo.getRito().name()).append(ln);
        builder.append("Autoridade emissora: ").append(usuario.getTipoUsuario() == null ? "NAO_IDENTIFICADA" : usuario.getTipoUsuario().name()).append(ln).append(ln);
        builder.append("Modelo: ").append(template.name()).append(ln).append(ln);
        for (String chave : template.variaveisObrigatorias()) {
            builder.append(capitalize(chave)).append(": ").append(safe(variaveis.get(chave))).append(ln);
        }
        variaveis.entrySet().stream()
                .filter(entry -> !template.variaveisObrigatorias().contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(capitalize(entry.getKey())).append(": ").append(safe(entry.getValue())).append(ln));
        builder.append(ln).append("Emitido em: ").append(LocalDateTime.now()).append(ln);
        return builder.toString();
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Campo";
        }
        String normalized = value.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "PENDENTE_DE_PREENCHIMENTO" : value.trim();
    }
}
