package com.tcc.pjb.backend.service.expediente;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.expediente.SemInteresseResponse;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;

@Service
public class SemInteresseService {

    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final OfficialDocumentTemplateService officialDocumentTemplateService;
    private final NotificationService notificationService;

    public SemInteresseService(WorkItemRepository workItemRepository,
                               CurrentUserService currentUserService,
                               OfficialDocumentTemplateService officialDocumentTemplateService,
                               NotificationService notificationService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    @Transactional
    public SemInteresseResponse registrar(Long expedienteId, String justificativa) {
        if (expedienteId == null) {
            throw new IllegalArgumentException("expedienteId é obrigatório");
        }
        Usuario usuario = currentUserService.getRequired();
        WorkItem expediente = workItemRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Expediente", expedienteId));
        Processo processo = expediente.getProcesso();
        if (processo == null || processo.getId() == null) {
            throw new IllegalStateException("Expediente sem processo associado");
        }

        LinkedHashMap<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("expedienteId", String.valueOf(expediente.getId()));
        variaveis.put("dataManifestacao", LocalDate.now().toString());
        variaveis.put("perfilManifestante", usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : usuario.getPerfil());
        variaveis.put("justificativa", sanitize(justificativa));
        variaveis.put("numeroProcesso", processo.getNumeroProcesso());

        OfficialDocumentTemplateRenderResponse render = officialDocumentTemplateService.renderizar(
                new OfficialDocumentTemplateRenderRequest(
                        processo.getId(),
                        TemplateDocumentoOficial.SEM_INTERESSE_MANIFESTACAO,
                        null,
                        Map.copyOf(variaveis),
                        Boolean.TRUE,
                        Boolean.TRUE
                )
        );

        expediente.setSemInteresse(true);
        expediente.setStatus(WorkItemStatus.CONCLUIDO);
        expediente.setDescricao(mergeDescricao(expediente.getDescricao(), sanitize(justificativa), usuario));
        workItemRepository.save(expediente);

        if (processo.getUsuario() != null) {
            notificationService.notifyUser(
                    processo.getUsuario(),
                    processo,
                    "Manifestação registrada",
                    "Foi registrada manifestação de sem interesse no expediente " + expediente.getId(),
                    null
            );
        }

        return new SemInteresseResponse(
                expediente.getId(),
                processo.getId(),
                processo.getNumeroProcesso(),
                expediente.getStatus() != null ? expediente.getStatus().name() : null,
                expediente.isSemInteresse(),
                render.documentoId(),
                render.hashSha256(),
                Instant.now(),
                render.assinaturaQualificada(),
                render.validacaoSoberana()
        );
    }

    private String mergeDescricao(String atual, String justificativa, Usuario usuario) {
        StringBuilder builder = new StringBuilder();
        if (atual != null && !atual.isBlank()) {
            builder.append(atual.trim());
        }
        if (!builder.isEmpty()) {
            builder.append(System.lineSeparator()).append(System.lineSeparator());
        }
        builder.append("Sem interesse registrado por ")
                .append(usuario.getNome() != null ? usuario.getNome() : "usuário institucional")
                .append(" em ")
                .append(LocalDate.now());
        if (justificativa != null && !justificativa.isBlank()) {
            builder.append(System.lineSeparator()).append("Justificativa: ").append(justificativa);
        }
        return builder.toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "SEM_JUSTIFICATIVA_ADICIONAL";
        }
        String sanitized = value.trim().replace('\u0000', ' ');
        if (sanitized.isBlank()) {
            return "SEM_JUSTIFICATIVA_ADICIONAL";
        }
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }
}
