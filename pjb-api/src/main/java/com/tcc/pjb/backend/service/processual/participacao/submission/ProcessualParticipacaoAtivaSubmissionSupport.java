package com.tcc.pjb.backend.service.processual.participacao.submission;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.participacao.ActionProfile;
import com.tcc.pjb.backend.service.processual.participacao.Persona;
import com.tcc.pjb.backend.service.processual.participacao.ProcessualParticipacaoAtivaSupportUtils;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessualParticipacaoAtivaSubmissionSupport {

    public static final int MAX_ATTACHMENTS = 20;
    public static final int MAX_TOTAL_BYTES = 80 * 1024 * 1024;
    public static final int MAX_INLINE_TEXT_BYTES = 2 * 1024 * 1024;
    public static final int MAX_BINARY_ATTACHMENT_BYTES = 12 * 1024 * 1024;
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "image/png",
            "image/jpeg",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final DocumentoProcessualRepository documentoRepository;
    private final EntityManager entityManager;

    public ProcessualParticipacaoAtivaSubmissionSupport(DocumentoProcessualRepository documentoRepository,
                                                 EntityManager entityManager) {
        this.documentoRepository = Objects.requireNonNull(documentoRepository, "documentoRepository");
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    public PreparedPrimaryDocument preparePrimaryDocument(Processo processo,
                                                   Usuario usuario,
                                                   Persona persona,
                                                   ActionProfile action,
                                                   SubmissionRequest request) {
        String titulo = ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(
                ProcessualParticipacaoAtivaSupportUtils.trimToNull(request.titulo()),
                action.label() + " — " + persona.label());
        String conteudo = ProcessualParticipacaoAtivaSupportUtils.requireText(request.conteudoPrincipal(), "conteudoPrincipal");
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_INLINE_TEXT_BYTES) {
            throw ProcessualParticipacaoAtivaSupportUtils.sizeValidation("Conteúdo principal excede o limite inline para juntada direta.");
        }
        String sha256 = ProcessualParticipacaoAtivaSupportUtils.shaHex("SHA-256", bytes);
        if (documentoRepository.existsByProcessoIdAndSha256(processo.getId(), sha256)) {
            throw ProcessualParticipacaoAtivaSupportUtils.duplicateValidation("Já existe documento principal com o mesmo conteúdo neste processo.");
        }
        NivelSigilo sigilo = ProcessualParticipacaoAtivaSupportUtils.resolveRequestedSigilo(request, processo, action);
        DocumentoCategoria categoria = ProcessualParticipacaoAtivaSupportUtils.resolveRequestedCategoria(request, sigilo);
        DocumentoProcessual doc = DocumentoProcessual.builder()
                .id(UUID.randomUUID())
                .processo(entityManager.getReference(Processo.class, processo.getId()))
                .titulo(titulo)
                .nomeOriginal(ProcessualParticipacaoAtivaSupportUtils.slugFilename(titulo, ".md"))
                .contentType("text/markdown")
                .tamanhoBytes((long) bytes.length)
                .sha256(sha256)
                .sha384(ProcessualParticipacaoAtivaSupportUtils.shaHex("SHA-384", bytes))
                .storageBackend("INLINE_DB")
                .storageUri(ProcessualParticipacaoAtivaSupportUtils.inlineStorageUri(processo.getId(), sha256))
                .pdf(bytes)
                .origemSistema(ProcessualParticipacaoAtivaSupportUtils.ORIGEM_SISTEMA)
                .categoria(categoria)
                .nivelSigilo(sigilo)
                .criadoPor(usuario.getId())
                .criadoEm(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        return new PreparedPrimaryDocument(doc, bytes.length);
    }

    public List<PreparedAttachment> prepareAttachments(Processo processo,
                                                Usuario usuario,
                                                Persona persona,
                                                ActionProfile action,
                                                List<AttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        if (attachments.size() > MAX_ATTACHMENTS) {
            throw ProcessualParticipacaoAtivaSupportUtils.sizeValidation("Quantidade de anexos excede o limite operacional do workspace.");
        }
        int totalBytes = 0;
        List<PreparedAttachment> out = new ArrayList<>(attachments.size());
        Set<String> seenHashes = new LinkedHashSet<>();
        for (AttachmentRequest attachment : attachments) {
            String nome = ProcessualParticipacaoAtivaSupportUtils.requireText(attachment.nomeArquivo(), "anexos.nomeArquivo");
            String contentType = ProcessualParticipacaoAtivaSupportUtils.normalizeContentType(attachment.contentType());
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw ProcessualParticipacaoAtivaSupportUtils.formatValidation("Content-Type não suportado para juntada direta: " + contentType);
            }
            byte[] bytes = ProcessualParticipacaoAtivaSupportUtils.decodeContent(attachment.base64Content(), nome);
            if (bytes.length == 0) {
                throw ProcessualParticipacaoAtivaSupportUtils.validation("Anexo vazio não pode ser protocolado.");
            }
            if (bytes.length > MAX_BINARY_ATTACHMENT_BYTES) {
                throw ProcessualParticipacaoAtivaSupportUtils.sizeValidation("Anexo excede o limite individual permitido no workspace.");
            }
            totalBytes += bytes.length;
            if (totalBytes > MAX_TOTAL_BYTES) {
                throw ProcessualParticipacaoAtivaSupportUtils.sizeValidation("Lote anexado excede o limite total permitido para a submissão.");
            }
            String sha256 = ProcessualParticipacaoAtivaSupportUtils.shaHex("SHA-256", bytes);
            if (!seenHashes.add(sha256)) {
                throw ProcessualParticipacaoAtivaSupportUtils.duplicateValidation("O lote contém anexos duplicados pelo mesmo hash.");
            }
            if (documentoRepository.existsByProcessoIdAndSha256(processo.getId(), sha256)) {
                throw ProcessualParticipacaoAtivaSupportUtils.duplicateValidation("Já existe no processo um anexo com o mesmo conteúdo digital.");
            }
            NivelSigilo sigilo = action.highSensitivity()
                    ? ProcessualParticipacaoAtivaSupportUtils.maxSigilo(ProcessualParticipacaoAtivaSupportUtils.resolveSigilo(processo), NivelSigilo.SIGILO_N2)
                    : ProcessualParticipacaoAtivaSupportUtils.resolveSigilo(processo);
            if (attachment.nivelSigilo() != null && !attachment.nivelSigilo().isBlank()) {
                sigilo = ProcessualParticipacaoAtivaSupportUtils.maxSigilo(sigilo, NivelSigilo.fromString(attachment.nivelSigilo()));
            }
            DocumentoCategoria categoria = attachment.categoria() == null || attachment.categoria().isBlank()
                    ? (sigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() ? DocumentoCategoria.PESSOAL : DocumentoCategoria.PUBLICO)
                    : DocumentoCategoria.fromString(attachment.categoria());
            String titulo = ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(ProcessualParticipacaoAtivaSupportUtils.trimToNull(attachment.titulo()), nome);
            DocumentoProcessual documento = DocumentoProcessual.builder()
                    .id(UUID.randomUUID())
                    .processo(entityManager.getReference(Processo.class, processo.getId()))
                    .titulo(titulo)
                    .nomeOriginal(nome)
                    .contentType(contentType)
                    .tamanhoBytes((long) bytes.length)
                    .sha256(sha256)
                    .sha384(ProcessualParticipacaoAtivaSupportUtils.shaHex("SHA-384", bytes))
                    .storageBackend("INLINE_DB")
                    .storageUri(ProcessualParticipacaoAtivaSupportUtils.inlineStorageUri(processo.getId(), sha256))
                    .pdf(bytes)
                    .origemSistema(ProcessualParticipacaoAtivaSupportUtils.ORIGEM_SISTEMA + ':' + persona.name())
                    .categoria(categoria)
                    .nivelSigilo(sigilo)
                    .criadoPor(usuario.getId())
                    .criadoEm(LocalDateTime.now(ZoneOffset.UTC))
                    .build();
            out.add(new PreparedAttachment(documento, bytes.length));
        }
        return List.copyOf(out);
    }

    public void ensureNoDuplicate(PreparedPrimaryDocument primary, List<PreparedAttachment> attachments) {
        int total = primary.sizeBytes();
        for (PreparedAttachment attachment : attachments) {
            total += attachment.sizeBytes();
        }
        if (total > MAX_TOTAL_BYTES) {
            throw ProcessualParticipacaoAtivaSupportUtils.sizeValidation("O lote completo excede o envelope máximo do workspace de participação ativa.");
        }
    }

    public String buildReceptionDescription(Processo processo,
                                     Usuario usuario,
                                     Persona persona,
                                     ActionProfile action,
                                     SubmissionRequest request,
                                     Long eventSeq,
                                     DocumentoProcessual primary) {
        List<String> lines = new ArrayList<>();
        lines.add("Recepção automática da participação ativa protocolada no próprio processo.");
        lines.add("Perfil: " + persona.label() + " / usuário=" + ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(usuario.getNome(), usuario.getEmail(), usuario.getCpf()));
        lines.add("Ação: " + action.label() + " (" + action.code() + ")");
        lines.add("Processo: " + ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())));
        lines.add("Documento principal: " + ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(primary.getTitulo(), primary.getNomeOriginal()) + " / sha256=" + primary.getSha256());
        lines.add("Evento seq: " + eventSeq);
        if (Boolean.TRUE.equals(request.urgente())) {
            lines.add("Marcado como urgente pela origem, exigindo leitura qualificada da secretaria/unidade.");
        }
        if (ProcessualParticipacaoAtivaSupportUtils.trimToNull(request.referenciaPrazo()) != null) {
            lines.add("Referência de prazo informada: " + request.referenciaPrazo());
        }
        return String.join("\n", lines);
    }

    public List<DocumentoProcessual> saveAll(List<DocumentoProcessual> documentos) {
        return documentoRepository.saveAll(documentos);
    }

    public String summarizeAck(ActionProfile action, WorkItem recepcao, int documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("Submissão recebida em trilha unificada do PJB com ").append(documents).append(" documento(s).");
        sb.append(" Ação protocolada: ").append(action.label()).append('.');
        if (recepcao != null) {
            sb.append(" Recepção topológica aberta em ").append(recepcao.getInboxKey()).append('.');
        }
        return sb.toString();
    }
}
