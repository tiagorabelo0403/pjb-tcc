package com.tcc.pjb.backend.modules.laiane.service;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.*;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeCaseBundle;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeDeadlineDelegation;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeTese;
import com.tcc.pjb.backend.modules.laiane.model.LaianeCaseBundleStatus;
import com.tcc.pjb.backend.modules.laiane.model.LaianeDeadlineDelegationStatus;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.*;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRitoAttachmentPolicy;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LaianeLawyerService {

    private final LaianeRoleGuard guard;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final LaianeTeseRepository teseRepository;

    
    private final WorkItemRepository workItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.tcc.pjb.backend.model.repository.ProcessoRepository processRepository;
    private final LaianeDeadlineDelegationRepository delegationRepository;
    private final LaianeCaseBundleRepository caseBundleRepository;
    private final AuditoriaInteligenteService auditoria;
    private final ObjectMapper objectMapper;
    private final ProceduralCatalogService proceduralCatalogService;
    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    private final PjbTimeService timeService;

    
    
    

    @Transactional
    public LaianeProcuracao createProcuracao(Long clienteId,
                                             String poderesJson,
                                             LocalDateTime validadeAte,
                                             String tipoInstrumento,
                                             Long audienciaId,
                                             String tipoAudiencia,
                                             boolean contextoConsensual,
                                             boolean poderesEspeciaisTransigir,
                                             String termoAudienciaReferencia,
                                             String ataAudienciaReferencia) {
        var adv = guard.requireAdvogado();

        RepresentacaoProcessualPolicyResponse policy = representacaoProcessualPolicyService.resolve(
                (String) null,
                (String) null,
                null,
                adv.getTipoUsuario(),
                tipoInstrumento,
                audienciaId,
                tipoAudiencia,
                contextoConsensual,
                poderesEspeciaisTransigir,
                termoAudienciaReferencia,
                ataAudienciaReferencia
        );
        ensureLawyerCompatibleRepresentation(policy);

        LocalDate fim = (validadeAte != null) ? validadeAte.toLocalDate() : null;
        var proc = LaianeProcuracao.builder()
                .advogado(adv)
                .clienteId(clienteId)
                .status(LaianeProcuracaoStatus.ATIVA)
                .inicioVigencia(LocalDate.now())
                .fimVigencia(fim)
                .poderes(mergePoderesJson(poderesJson, policy))
                .build();

        proc = procuracaoRepository.save(proc);
        auditoria.registrarEventoImutavel("ADV_PROCURACAO_CRIADA", "LAIANE_PROCURACAO", proc.getId(), "clienteId=" + clienteId);
        return proc;
    }

    
    @Transactional
    public LaianeProcuracao solicitarHabilitacao(Long processoId,
                                                 Long clienteId,
                                                 String poderesJson,
                                                 String anexosJson,
                                                 String mensagem,
                                                 String tipoInstrumento,
                                                 Long audienciaId,
                                                 String tipoAudiencia,
                                                 boolean contextoConsensual,
                                                 boolean poderesEspeciaisTransigir,
                                                 String termoAudienciaReferencia,
                                                 String ataAudienciaReferencia) {
        var adv = guard.requireAdvogado();

        if (processoId == null) {
            throw new com.tcc.pjb.backend.service.exception.ErroDeValidacaoException(com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao.CAMPO_OBRIGATORIO, "processoId");
        }
        if (clienteId == null) {
            throw new com.tcc.pjb.backend.service.exception.ErroDeValidacaoException(com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao.CAMPO_OBRIGATORIO, "clienteId");
        }

        boolean jaExiste = procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatusIn(
                adv.getId(),
                processoId,
                java.util.List.of(LaianeProcuracaoStatus.ATIVA, LaianeProcuracaoStatus.PENDENTE_HABILITACAO)
        );
        if (jaExiste) {
            throw new com.tcc.pjb.backend.service.exception.ErroDeValidacaoException(com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao.REGRA_NEGOCIO, "habilitacao")
                    .addMetadado("motivo", "Já existe habilitação ativa ou pendente para este processo")
                    .addMetadado("processoId", processoId)
                    .addMetadado("advogadoId", adv.getId());
        }

        var policy = consultarPoliticaRepresentacao(
                processoId,
                tipoInstrumento,
                audienciaId,
                tipoAudiencia,
                contextoConsensual,
                poderesEspeciaisTransigir,
                termoAudienciaReferencia,
                ataAudienciaReferencia
        );
        ensureLawyerCompatibleRepresentation(policy);

        LaianeProcuracao p = LaianeProcuracao.builder()
                .advogado(adv)
                .processoId(processoId)
                .clienteId(clienteId)
                .status(LaianeProcuracaoStatus.PENDENTE_HABILITACAO)
                .inicioVigencia(null)
                .fimVigencia(null)
                .poderes(mergePoderesJson(poderesJson, policy))
                .anexosJson(anexosJson)
                .build();

        p = procuracaoRepository.save(p);

        auditoria.registrarEventoImutavelJustificado(
                "ADV_HABILITACAO_SOLICITADA",
                "LAIANE_PROCURACAO",
                p.getId(),
                "processoId=" + processoId + ";clienteId=" + clienteId,
                mensagem
        );

        return p;
    }



    @Transactional(readOnly = true)
    public RepresentacaoProcessualPolicyResponse consultarPoliticaRepresentacao(Long processoId,
                                                                                String tipoInstrumento,
                                                                                Long audienciaId,
                                                                                String tipoAudiencia,
                                                                                boolean contextoConsensual,
                                                                                boolean poderesEspeciaisTransigir,
                                                                                String termoAudienciaReferencia,
                                                                                String ataAudienciaReferencia) {
        var adv = guard.requireAdvogado();
        var processo = processoId == null ? null : processRepositorySafe(processoId);
        return representacaoProcessualPolicyService.resolve(
                processo,
                adv,
                tipoInstrumento,
                audienciaId,
                tipoAudiencia,
                contextoConsensual,
                poderesEspeciaisTransigir,
                termoAudienciaReferencia,
                ataAudienciaReferencia
        );
    }


    @Transactional(readOnly = true)
    public List<LaianeProcuracao> listMyProcuracoes() {
        var adv = guard.requireAdvogado();
        return procuracaoRepository.findByAdvogado_Id(adv.getId(), PageRequest.of(0, 200)).getContent();
    }

    @Transactional
    public void revokeProcuracao(Long id) {
        var adv = guard.requireAdvogado();
        var p = procuracaoRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Procuração não encontrada"));
        if (!p.getAdvogado().getId().equals(adv.getId())) {
            throw new SecurityException("Não autorizado");
        }
        p.setStatus(LaianeProcuracaoStatus.REVOGADA);
        if (p.getFimVigencia() == null) {
            p.setFimVigencia(LocalDate.now());
        }
        procuracaoRepository.save(p);
        auditoria.registrarEventoImutavel("ADV_PROCURACAO_REVOGADA", "LAIANE_PROCURACAO", p.getId(), "clienteId=" + p.getClienteId());
    }

    @Transactional
    public LaianeTese createTese(String area, String tese, String fundamentacao, String tagsCsv) {
        var adv = guard.requireAdvogado();
        String tagsJson = null;
        if (tagsCsv != null && !tagsCsv.isBlank()) {
            List<String> tags = Arrays.stream(tagsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
            try {
                tagsJson = objectMapper.writeValueAsString(tags);
            } catch (Exception ignored) {
            }
        }
        var t = LaianeTese.builder()
                .advogado(adv)
                .area(area)
                
                .titulo(tese)
                .corpo(fundamentacao)
                .tagsJson(tagsJson)
                .build();
        t = teseRepository.save(t);
        auditoria.registrarEventoImutavel("ADV_TESE_CRIADA", "LAIANE_TESE", t.getId(), "area=" + area);
        return t;
    }

    @Transactional(readOnly = true)
    public List<LaianeTese> listMyTeses() {
        var adv = guard.requireAdvogado();
        return teseRepository.findByAdvogado_Id(adv.getId(), PageRequest.of(0, 200)).getContent();
    }

    
    
    

    @Transactional(readOnly = true)
    public LaianeLawyerAttachmentValidationResponse validateAttachments(LaianeLawyerAttachmentValidationRequest req) {
        guard.requireAdvogado();

        String rito = req != null ? req.getRito() : null;
        List<String> informed = normalizeAttachments(req);
        var resolvedRito = proceduralCatalogService.resolveRito(rito, null, null);
        List<String> required = proceduralCatalogService.requiredDocuments(resolvedRito);

        Set<String> inf = new HashSet<>(informed);
        List<String> missing = required.stream().filter(r -> !inf.contains(normalizeKey(r))).toList();

        return LaianeLawyerAttachmentValidationResponse.builder()
                .rito(rito)
                .ok(missing.isEmpty())
                .required(required)
                .informed(informed)
                .missing(missing)
                .build();
    }

    @Transactional
    public LaianeDeadlineDelegationResponse createDelegation(LaianeDeadlineDelegationCreateRequest req) {
        var adv = guard.requireAdvogado();

        WorkItem wi = workItemRepository.findById(req.getWorkItemId())
                .orElseThrow(() -> new NoSuchElementException("WorkItem não encontrado"));

        Usuario delegatee = usuarioRepository.findById(req.getDelegateeId())
                .orElseThrow(() -> new NoSuchElementException("Delegado não encontrado"));

        LaianeDeadlineDelegation d = LaianeDeadlineDelegation.builder()
                .delegator(adv)
                .delegatee(delegatee)
                .workItem(wi)
                .status(LaianeDeadlineDelegationStatus.PENDENTE)
                .descricao(req.getDescricao())
                .build();

        d = delegationRepository.save(d);

        auditoria.registrarEventoImutavelJustificado(
                "ADV_DELEGACAO_CRIADA",
                "LAIANE_DELEGACAO",
                d.getId(),
                "workItemId=" + wi.getId() + ";delegateeId=" + delegatee.getId(),
                req.getJustificativa()
        );

        return toDelegationResponse(d);
    }

    @Transactional(readOnly = true)
    public Page<LaianeDeadlineDelegationResponse> listDelegationsSent(int page, int size) {
        var adv = guard.requireAdvogado();
        Page<LaianeDeadlineDelegation> result = delegationRepository.findByDelegator_IdOrderByCreatedAtDesc(adv.getId(), PageRequest.of(page, size));
        return result.map(this::toDelegationResponse);
    }

    @Transactional(readOnly = true)
    public Page<LaianeDeadlineDelegationResponse> listDelegationsReceived(int page, int size) {
        var adv = guard.requireAdvogado();
        Page<LaianeDeadlineDelegation> result = delegationRepository.findByDelegatee_IdOrderByCreatedAtDesc(adv.getId(), PageRequest.of(page, size));
        return result.map(this::toDelegationResponse);
    }

    @Transactional
    public LaianeDeadlineDelegationResponse acceptDelegation(Long id, String justificativa) {
        var adv = guard.requireAdvogado();
        LaianeDeadlineDelegation d = delegationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Delegação não encontrada"));

        if (d.getDelegatee() == null || !Objects.equals(d.getDelegatee().getId(), adv.getId())) {
            throw new SecurityException("Somente o delegado pode aceitar");
        }

        if (d.getStatus() != LaianeDeadlineDelegationStatus.PENDENTE) {
            throw new IllegalStateException("Delegação não está pendente");
        }

        d.setStatus(LaianeDeadlineDelegationStatus.ACEITA);
        d.setAcceptedAt(LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone()));
        d = delegationRepository.save(d);

        auditoria.registrarEventoImutavelJustificado(
                "ADV_DELEGACAO_ACEITA",
                "LAIANE_DELEGACAO",
                d.getId(),
                "delegateeId=" + adv.getId(),
                justificativa
        );

        return toDelegationResponse(d);
    }

    @Transactional
    public LaianeDeadlineDelegationResponse completeDelegation(Long id, String justificativa) {
        var adv = guard.requireAdvogado();
        LaianeDeadlineDelegation d = delegationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Delegação não encontrada"));

        if (d.getDelegatee() == null || !Objects.equals(d.getDelegatee().getId(), adv.getId())) {
            throw new SecurityException("Somente o delegado pode concluir");
        }

        if (d.getStatus() != LaianeDeadlineDelegationStatus.ACEITA) {
            throw new IllegalStateException("Delegação não está aceita");
        }

        d.setStatus(LaianeDeadlineDelegationStatus.CONCLUIDA);
        d.setCompletedAt(LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone()));
        d = delegationRepository.save(d);

        auditoria.registrarEventoImutavelJustificado(
                "ADV_DELEGACAO_CONCLUIDA",
                "LAIANE_DELEGACAO",
                d.getId(),
                "delegateeId=" + adv.getId(),
                justificativa
        );

        return toDelegationResponse(d);
    }

    @Transactional
    public LaianeDeadlineDelegationResponse cancelDelegation(Long id, String justificativa) {
        var adv = guard.requireAdvogado();
        LaianeDeadlineDelegation d = delegationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Delegação não encontrada"));

        boolean isDelegator = d.getDelegator() != null && Objects.equals(d.getDelegator().getId(), adv.getId());
        if (!isDelegator) {
            throw new SecurityException("Somente o delegador pode cancelar");
        }
        if (d.getStatus() == LaianeDeadlineDelegationStatus.CONCLUIDA) {
            throw new IllegalStateException("Delegação já concluída");
        }

        d.setStatus(LaianeDeadlineDelegationStatus.CANCELADA);
        d = delegationRepository.save(d);

        auditoria.registrarEventoImutavelJustificado(
                "ADV_DELEGACAO_CANCELADA",
                "LAIANE_DELEGACAO",
                d.getId(),
                "delegatorId=" + adv.getId(),
                justificativa
        );

        return toDelegationResponse(d);
    }

    @Transactional
    public LaianeCaseBundleResponse createBundle(LaianeCaseBundleCreateRequest req) {
        var adv = guard.requireAdvogado();

        String processosJson = writeJson(req.getProcessosIds());

        LaianeCaseBundle bundle = LaianeCaseBundle.builder()
                .advogado(adv)
                .status(LaianeCaseBundleStatus.ABERTO)
                .processosJson(processosJson)
                .teseId(req.getTeseId())
                .descricao(req.getDescricao())
                .build();

        bundle = caseBundleRepository.save(bundle);

        auditoria.registrarEventoImutavelJustificado(
                "ADV_BUNDLE_CRIADO",
                "LAIANE_BUNDLE",
                bundle.getId(),
                "processos=" + req.getProcessosIds().size() + ";teseId=" + req.getTeseId(),
                req.getJustificativa()
        );

        return toBundleResponse(bundle);
    }

    @Transactional(readOnly = true)
    public Page<LaianeCaseBundleResponse> listBundles(String status, int page, int size) {
        var adv = guard.requireAdvogado();

        int safePage = Math.max(0, page);
        int safeSize = Math.max(5, Math.min(size, 200));

        Page<LaianeCaseBundle> result;
        if (status == null || status.isBlank()) {
            result = caseBundleRepository.findByAdvogado_IdOrderByCreatedAtDesc(adv.getId(), PageRequest.of(safePage, safeSize));
        } else {
            result = caseBundleRepository.findByAdvogado_IdAndStatusOrderByCreatedAtDesc(adv.getId(), LaianeCaseBundleStatus.from(status), PageRequest.of(safePage, safeSize));
        }

        return result.map(this::toBundleResponse);
    }

    @Transactional
    public LaianeCaseBundleResponse updateBundle(Long id, LaianeCaseBundleUpdateRequest req) {
        var adv = guard.requireAdvogado();

        LaianeCaseBundle bundle = caseBundleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bundle não encontrado"));

        if (bundle.getAdvogado() == null || !Objects.equals(bundle.getAdvogado().getId(), adv.getId())) {
            throw new SecurityException("Não autorizado");
        }

        if (req.getProcessosIds() != null) {
            bundle.setProcessosJson(writeJson(req.getProcessosIds()));
        }
        if (req.getTeseId() != null) {
            bundle.setTeseId(req.getTeseId());
        }
        if (req.getDescricao() != null) {
            bundle.setDescricao(req.getDescricao());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            bundle.setStatus(LaianeCaseBundleStatus.from(req.getStatus()));
        }

        bundle = caseBundleRepository.save(bundle);

        auditoria.registrarEventoImutavelJustificado(
                "ADV_BUNDLE_ATUALIZADO",
                "LAIANE_BUNDLE",
                bundle.getId(),
                "status=" + bundle.getStatus().name(),
                req.getJustificativa()
        );

        return toBundleResponse(bundle);
    }

    @Transactional
    public LaianeCaseBundleConsolidationResponse consolidateBundle(Long id, String justificativa) {
        var adv = guard.requireAdvogado();

        LaianeCaseBundle bundle = caseBundleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bundle não encontrado"));

        if (bundle.getAdvogado() == null || !Objects.equals(bundle.getAdvogado().getId(), adv.getId())) {
            throw new SecurityException("Não autorizado");
        }

        List<Long> processos = readProcessos(bundle.getProcessosJson());

        int openWorkItems = 0;
        Instant earliest = null;
        for (Long pid : processos) {
            if (pid == null) continue;
            List<WorkItem> items = workItemRepository.findAllByProcesso(pid);
            for (WorkItem wi : items) {
                if (wi.getStatus() == WorkItemStatus.CONCLUIDO) {
                    continue;
                }
                openWorkItems++;
            }
            Instant min = workItemRepository.minOpenDueAtForProcesso(pid);
            if (min != null && (earliest == null || min.isBefore(earliest))) {
                earliest = min;
            }
        }

        bundle.setStatus(LaianeCaseBundleStatus.CONSOLIDADO);
        bundle = caseBundleRepository.save(bundle);

        String suggestion = (earliest != null)
                ? "Priorize o primeiro prazo em " + earliest + " e consolide a tese para os processos do lote."
                : "Lote consolidado. Não há prazos abertos com data definida (dueAt) nos work items do lote.";

        auditoria.registrarEventoImutavelJustificado(
                "ADV_BUNDLE_CONSOLIDADO",
                "LAIANE_BUNDLE",
                bundle.getId(),
                "processos=" + processos.size() + ";openWorkItems=" + openWorkItems,
                justificativa
        );

        return LaianeCaseBundleConsolidationResponse.builder()
                .bundleId(bundle.getId())
                .totalProcessos(processos.size())
                .processosIds(processos)
                .openWorkItems(openWorkItems)
                .earliestDueAt(earliest)
                .suggestion(suggestion)
                .build();
    }

    
    
    

    private LaianeDeadlineDelegationResponse toDelegationResponse(LaianeDeadlineDelegation d) {
        return LaianeDeadlineDelegationResponse.builder()
                .id(d.getId())
                .delegatorId(d.getDelegator() != null ? d.getDelegator().getId() : null)
                .delegateeId(d.getDelegatee() != null ? d.getDelegatee().getId() : null)
                .workItemId(d.getWorkItem() != null ? d.getWorkItem().getId() : null)
                .status(d.getStatus() != null ? d.getStatus().name() : null)
                .descricao(d.getDescricao())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .acceptedAt(d.getAcceptedAt())
                .completedAt(d.getCompletedAt())
                .build();
    }

    private LaianeCaseBundleResponse toBundleResponse(LaianeCaseBundle b) {
        List<Long> processos = readProcessos(b.getProcessosJson());
        return LaianeCaseBundleResponse.builder()
                .id(b.getId())
                .advogadoId(b.getAdvogado() != null ? b.getAdvogado().getId() : null)
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .processosIds(processos)
                .teseId(b.getTeseId())
                .descricao(b.getDescricao())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private List<Long> readProcessos(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids != null ? ids : List.of());
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao serializar processosIds");
        }
    }

    private void ensureLawyerCompatibleRepresentation(RepresentacaoProcessualPolicyResponse policy) {
        if (policy == null) {
            return;
        }
        if (!policy.regularidadeSuficiente()) {
            throw new com.tcc.pjb.backend.service.exception.ErroDeValidacaoException(com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao.REGRA_NEGOCIO, "representacao")
                    .addMetadado("motivo", firstAlert(policy.alertas()))
                    .addMetadado("instrumento", policy.resolvedInstrument());
        }
        if ("JUS_POSTULANDI_TRABALHISTA".equalsIgnoreCase(policy.resolvedInstrument())) {
            throw new com.tcc.pjb.backend.service.exception.ErroDeValidacaoException(com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao.REGRA_NEGOCIO, "tipoInstrumento")
                    .addMetadado("motivo", "Jus postulandi e regime de autorrepresentacao da parte e nao deve ser cadastrado pela rota exclusiva da advocacia.");
        }
    }

    private String mergePoderesJson(String poderesJson, RepresentacaoProcessualPolicyResponse policy) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        put(out, "raw", safeLongText(poderesJson));
        List<String> normalized = parseDeclaredPowers(poderesJson);
        if (!normalized.isEmpty()) {
            out.put("declaredPowers", normalized);
        }
        if (policy != null) {
            out.put("representationPolicy", policy.envelope());
        }
        if (out.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(out);
        } catch (Exception ignored) {
            return safeLongText(poderesJson);
        }
    }

    private List<String> parseDeclaredPowers(String poderesJson) {
        if (poderesJson == null || poderesJson.isBlank()) {
            return List.of();
        }
        try {
            Object parsed = objectMapper.readValue(poderesJson, Object.class);
            if (parsed instanceof Collection<?> collection) {
                return collection.stream().map(this::asText).filter(s -> s != null && !s.isBlank()).distinct().toList();
            }
            if (parsed instanceof Map<?, ?> map) {
                Object list = map.get("declaredPowers");
                if (list instanceof Collection<?> collection) {
                    return collection.stream().map(this::asText).filter(s -> s != null && !s.isBlank()).distinct().toList();
                }
            }
        } catch (Exception ignored) {
        }
        return Arrays.stream(poderesJson.split(",|;|\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String safeLongText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 12000 ? trimmed.substring(0, 12000) : trimmed;
    }

    private void put(LinkedHashMap<String, Object> map, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        map.put(key, value);
    }

    private String firstAlert(List<String> alertas) {
        if (alertas == null || alertas.isEmpty()) {
            return "Representacao processual inconsistente para a rota escolhida";
        }
        return alertas.getFirst();
    }

    private com.tcc.pjb.backend.model.entity.Processo processRepositorySafe(Long processoId) {
        return processRepository.findById(processoId)
                .orElseThrow(() -> new NoSuchElementException("Processo nao encontrado"));
    }


    private List<String> normalizeAttachments(LaianeLawyerAttachmentValidationRequest req) {
        List<String> out = new ArrayList<>();
        if (req == null) return out;

        if (req.getAnexos() != null) {
            req.getAnexos().forEach(a -> {
                if (a != null && !a.isBlank()) out.add(normalizeKey(a));
            });
        }

        if (req.getAnexosJson() != null && !req.getAnexosJson().isBlank()) {
            String raw = req.getAnexosJson().trim();
            try {
                if (raw.startsWith("[")) {
                    List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
                    list.forEach(a -> {
                        if (a != null && !a.isBlank()) out.add(normalizeKey(a));
                    });
                }
            } catch (Exception ignored) {
            }
        }

        return out.stream().distinct().toList();
    }

    private String normalizeKey(String s) {
        return s.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
