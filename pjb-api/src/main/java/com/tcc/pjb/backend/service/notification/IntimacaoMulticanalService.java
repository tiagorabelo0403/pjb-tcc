package com.tcc.pjb.backend.service.notification;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;

@Service
public class IntimacaoMulticanalService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificationService notificationService;
    private final PjbAuthorizationService authorizationService;
    private final OfficialDocumentTemplateService officialDocumentTemplateService;

    public IntimacaoMulticanalService(ProcessoRepository processoRepository,
                                      UsuarioRepository usuarioRepository,
                                      NotificationService notificationService,
                                      PjbAuthorizationService authorizationService,
                                      OfficialDocumentTemplateService officialDocumentTemplateService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "notification.multichannel.dispatch.persist", maxMillis = 1800, critical = true)
    public DispatchPlanResponse dispatch(Long processoId,
                                         Long usuarioId,
                                         String titulo,
                                         String mensagem,
                                         String urlAcesso,
                                         boolean prioridadeAlta) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));

        NotificationService.DispatchReport report = notificationService.notifyUserAdvanced(usuario, processo, titulo, mensagem, urlAcesso, prioridadeAlta);
        OfficialDocumentTemplateRenderResponse intimacaoFormal = renderFormalIntimation(processo, usuario, titulo, mensagem, urlAcesso, prioridadeAlta);
        return new DispatchPlanResponse(
                processo.getId(),
                usuario.getId(),
                report.deliveredChannels(),
                report.suppressedChannels(),
                report.failedChannels(),
                report.trackingTokens(),
                report.status(),
                Instant.now(),
                report.rationale(),
                summarizeRenderedDocument(intimacaoFormal),
                intimacaoFormal.assinaturaQualificada(),
                intimacaoFormal.validacaoSoberana()
        );
    }

    private OfficialDocumentTemplateRenderResponse renderFormalIntimation(Processo processo,
                                                                          Usuario usuario,
                                                                          String titulo,
                                                                          String mensagem,
                                                                          String urlAcesso,
                                                                          boolean prioridadeAlta) {
        String destinatario = usuario.getNome() == null || usuario.getNome().isBlank()
                ? "DESTINATARIO_NAO_INFORMADO"
                : usuario.getNome().trim();
        StringBuilder conteudo = new StringBuilder();
        if (titulo != null && !titulo.isBlank()) {
            conteudo.append(titulo.trim());
        }
        if (mensagem != null && !mensagem.isBlank()) {
            if (conteudo.length() > 0) {
                conteudo.append(System.lineSeparator()).append(System.lineSeparator());
            }
            conteudo.append(mensagem.trim());
        }
        if (urlAcesso != null && !urlAcesso.isBlank()) {
            conteudo.append(System.lineSeparator()).append(System.lineSeparator())
                    .append("Acesso eletrônico: ").append(urlAcesso.trim());
        }
        String prazo = prioridadeAlta ? "PRIORIDADE_IMEDIATA" : "CONFORME_TRILHA_MULTICANAL";
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                TemplateDocumentoOficial.INTIMACAO_FORMAL,
                "Intimação formal multicanal — " + processo.getNumeroProcesso(),
                Map.of(
                        "destinatario", destinatario,
                        "conteudoIntimacao", conteudo.length() == 0 ? "CONTEUDO_NAO_INFORMADO" : conteudo.toString(),
                        "prazoResposta", prazo
                ),
                Boolean.TRUE,
                Boolean.TRUE
        ));
    }

    private Map<String, Object> summarizeRenderedDocument(OfficialDocumentTemplateRenderResponse render) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("documentoId", render.documentoId());
        out.put("template", render.template().name());
        out.put("tituloDocumento", render.tituloDocumento());
        out.put("hashSha256", render.hashSha256());
        out.put("assinaturaQualificada", render.assinaturaQualificada());
        out.put("validacaoSoberana", render.validacaoSoberana());
        out.put("selado", render.selado());
        return Collections.unmodifiableMap(out);
    }

    public record DispatchPlanResponse(
            Long processoId,
            Long usuarioId,
            List<String> deliveredChannels,
            List<String> suppressedChannels,
            List<String> failedChannels,
            List<String> trackingTokens,
            String status,
            Instant generatedAt,
            List<String> rationale,
            Map<String, Object> documentoFormalAssinado,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
    }
}
