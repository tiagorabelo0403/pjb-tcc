package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferView;
import com.tcc.pjb.backend.core.governance.idempotency.IdempotencyInProgressException;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyBeginResult;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessTransfer;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessTransferItem;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspaceProfile;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeProcessTransferStatus;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeProcessTransferItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeProcessTransferRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspaceProfileRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeProcessTransferService {

    private final CurrentUserService currentUserService;
    private final EquipeRepository equipeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final AdvOfficeWorkspaceProfileRepository profileRepository;
    private final EquipeOfficePolicyRepository policyRepository;
    private final AdvOfficeProcessTransferRepository transferRepository;
    private final AdvOfficeProcessTransferItemRepository itemRepository;
    private final OfficeProcessTransferPreviewService previewService;
    private final RequestIdempotencyService requestIdempotencyService;
    private final AuditLedgerService auditLedgerService;

    public OfficeProcessTransferService(CurrentUserService currentUserService,
                                        EquipeRepository equipeRepository,
                                        UsuarioRepository usuarioRepository,
                                        ProcessoRepository processoRepository,
                                        MembroEquipeRepository membroEquipeRepository,
                                        AdvOfficeWorkspaceProfileRepository profileRepository,
                                        EquipeOfficePolicyRepository policyRepository,
                                        AdvOfficeProcessTransferRepository transferRepository,
                                        AdvOfficeProcessTransferItemRepository itemRepository,
                                        OfficeProcessTransferPreviewService previewService,
                                        RequestIdempotencyService requestIdempotencyService,
                                        AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.equipeRepository = Objects.requireNonNull(equipeRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.transferRepository = Objects.requireNonNull(transferRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.previewService = Objects.requireNonNull(previewService);
        this.requestIdempotencyService = Objects.requireNonNull(requestIdempotencyService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOfficeProcessTransferView> myIncomingTransfers() {
        Usuario usuario = currentUserService.getRequired();
        return transferRepository.findIncomingForUser(usuario.getId()).stream()
                .map(transfer -> view(transfer, usuario, canManage(transfer.getSourceEquipe().getId(), usuario.getId()) || canManage(transfer.getTargetEquipe().getId(), usuario.getId()), isActionable(transfer, usuario)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOfficeProcessTransferView> officeTransfers(Long equipeId) {
        Usuario usuario = currentUserService.getRequired();
        assertManageable(equipeId, usuario.getId());
        return transferRepository.findByEquipe(equipeId).stream()
                .map(transfer -> view(transfer, usuario, true, isActionable(transfer, usuario)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeProcessTransferPreviewView previewTransfer(FrontendOfficeProcessTransferRequest request) {
        Usuario usuario = currentUserService.getRequired();
        assertManageable(request.sourceEquipeId(), usuario.getId());
        return previewService.preview(request);
    }

    @Transactional
    public PjbFrontendOfficeProcessTransferView createTransfer(FrontendOfficeProcessTransferRequest request) {
        Usuario usuario = currentUserService.getRequired();
        assertManageable(request.sourceEquipeId(), usuario.getId());

        if (Objects.equals(request.sourceEquipeId(), request.targetEquipeId())) {
            throw new IllegalArgumentException("Equipe de origem e destino nao podem ser iguais.");
        }

        Equipe sourceEquipe = equipeRepository.findById(request.sourceEquipeId()).orElseThrow(() -> new IllegalArgumentException("Escritorio de origem nao encontrado."));
        Equipe targetEquipe = equipeRepository.findById(request.targetEquipeId()).orElseThrow(() -> new IllegalArgumentException("Escritorio de destino nao encontrado."));
        Usuario targetResponsible = usuarioRepository.findById(request.targetResponsibleUserId()).orElseThrow(() -> new IllegalArgumentException("Responsavel de destino nao encontrado."));
        MembroEquipe targetMembership = membroEquipeRepository.findByUsuario_IdAndEquipe_Id(targetResponsible.getId(), targetEquipe.getId()).orElse(null);
        if (targetMembership == null || !targetMembership.isAtivo()) {
            throw new IllegalStateException("Responsavel informado nao possui vinculo ativo com o escritorio de destino.");
        }

        String requestHash = Hashes.sha256Hex("OFFICE_TRANSFER_CREATE|" + request.sourceEquipeId() + "|" + request.targetEquipeId() + "|" + request.targetResponsibleUserId() + "|" + request.processoIds() + "|" + defaultIdempotencyKey(request.idempotencyKey(), usuario.getId()));
        RequestIdempotencyBeginResult begin = beginIdempotent("ADV_OFFICE_PROCESS_TRANSFER_CREATE", requestHash);
        if (!begin.created()) {
            return transferRepository.fetchById(Long.parseLong(begin.resourceId())).map(transfer -> view(transfer, usuario, true, false)).orElseThrow(() -> new IllegalStateException("Transferencia anterior nao localizada."));
        }

        try {
            List<Long> orderedIds = request.processoIds().stream().distinct().sorted().toList();
            List<Processo> processos = processoRepository.findAllById(orderedIds);
            if (processos.size() != orderedIds.size()) {
                throw new IllegalArgumentException("Um ou mais processos informados nao existem.");
            }
            for (Processo processo : processos) {
                if (processo.getEquipe() == null || !Objects.equals(processo.getEquipe().getId(), sourceEquipe.getId())) {
                    throw new IllegalStateException("Todos os processos devem pertencer ao escritorio de origem no momento da transferencia.");
                }
            }

            PjbFrontendOfficeProcessTransferPreviewView preview = previewService.previewExisting(sourceEquipe.getId(), targetEquipe.getId(), targetResponsible.getId(), processos);
            if (!preview.valid()) {
                throw new IllegalStateException("Transferencia bloqueada pela governanca do escritorio de destino: " + String.join(", ", preview.blockers()));
            }
            if (request.previewHash() != null && !request.previewHash().isBlank() && !Objects.equals(request.previewHash(), preview.previewHash())) {
                throw new IllegalStateException("Preview desatualizado. Gere novo preview antes de criar a transferencia.");
            }

            AdvOfficeProcessTransfer transfer = new AdvOfficeProcessTransfer();
            transfer.setSourceEquipe(sourceEquipe);
            transfer.setTargetEquipe(targetEquipe);
            transfer.setInitiatedByUserId(usuario.getId());
            transfer.setSourceResponsibleUserId(usuario.getId());
            transfer.setTargetResponsibleUserId(targetResponsible.getId());
            transfer.setMotivo(blankToNull(request.motivo()));
            transfer.setEscopo(blankToNull(request.escopo()));
            transfer.setProcessCount(processos.size());
            transfer.setSensitiveProcessCount(preview.sensitiveProcessCount());
            transfer.setPreviewSummary(preview.previewSummary());
            transfer.setImpactHash(preview.previewHash());
            transfer.setStatus(OfficeProcessTransferStatus.PENDING_DESTINATION_ACCEPTANCE);
            transfer.setCreatedAt(Instant.now());
            transferRepository.save(transfer);

            for (Processo processo : processos) {
                AdvOfficeProcessTransferItem item = new AdvOfficeProcessTransferItem();
                item.setTransfer(transfer);
                item.setProcesso(processo);
                item.setSourceEquipeId(processo.getEquipe() == null ? null : processo.getEquipe().getId());
                item.setSourceUsuarioId(processo.getUsuario() == null ? null : processo.getUsuario().getId());
                item.setTargetEquipeId(targetEquipe.getId());
                item.setTargetUsuarioId(targetResponsible.getId());
                item.setRamoDireito(processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
                item.setNivelSigilo(processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name());
                item.setNumeroProcessoSnapshot(firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()));
                itemRepository.save(item);
            }

            auditLedgerService.appendSafely("ADV_OFFICE_PROCESS_TRANSFER_CREATED", "EQUIPE", String.valueOf(sourceEquipe.getId()), "transfer=" + transfer.getId() + " targetEquipe=" + targetEquipe.getId() + " count=" + transfer.getProcessCount());
            PjbFrontendOfficeProcessTransferView view = view(transfer, usuario, true, false);
            requestIdempotencyService.complete(requestHash, "ADV_OFFICE_PROCESS_TRANSFER", String.valueOf(transfer.getId()), Hashes.sha256Hex(view.toString()), view.toString());
            return view;
        } catch (RuntimeException ex) {
            requestIdempotencyService.fail(requestHash);
            throw ex;
        }
    }

    @Transactional
    public PjbFrontendOfficeProcessTransferView acceptTransfer(Long transferId, FrontendOfficeProcessTransferDecisionRequest request) {
        Usuario usuario = currentUserService.getRequired();
        if (request == null || !Boolean.TRUE.equals(request.acceptTerms())) {
            throw new IllegalArgumentException("Aceite expresso da transferencia e obrigatorio.");
        }
        AdvOfficeProcessTransfer transfer = transferRepository.fetchById(transferId).orElseThrow(() -> new IllegalArgumentException("Transferencia nao encontrada."));
        if (!isActionable(transfer, usuario)) {
            throw new IllegalStateException("Usuario nao pode executar esta transferencia.");
        }

        String requestHash = Hashes.sha256Hex("OFFICE_TRANSFER_ACCEPT|" + transferId + "|" + usuario.getId() + "|" + defaultIdempotencyKey(request.idempotencyKey(), usuario.getId()));
        RequestIdempotencyBeginResult begin = beginIdempotent("ADV_OFFICE_PROCESS_TRANSFER_ACCEPT", requestHash);
        if (!begin.created()) {
            return transferRepository.fetchById(transferId).map(reloaded -> view(reloaded, usuario, true, false)).orElseThrow(() -> new IllegalStateException("Transferencia nao encontrada."));
        }

        try {
            List<AdvOfficeProcessTransferItem> items = itemRepository.findByTransferId(transferId);
            Usuario targetResponsible = usuarioRepository.findById(transfer.getTargetResponsibleUserId()).orElseThrow(() -> new IllegalStateException("Responsavel de destino nao encontrado."));
            List<Processo> processos = items.stream().map(AdvOfficeProcessTransferItem::getProcesso).toList();
            PjbFrontendOfficeProcessTransferPreviewView preview = previewService.previewExisting(transfer.getSourceEquipe().getId(), transfer.getTargetEquipe().getId(), targetResponsible.getId(), processos);
            if (!preview.valid()) {
                throw new IllegalStateException("Transferencia bloqueada no aceite pela governanca do escritorio de destino: " + String.join(", ", preview.blockers()));
            }
            if (transfer.getImpactHash() != null && !Objects.equals(transfer.getImpactHash(), preview.previewHash())) {
                throw new IllegalStateException("Cenario da transferencia foi alterado desde a criacao. Gere nova transferencia com preview atualizado.");
            }
            for (AdvOfficeProcessTransferItem item : items) {
                Processo processo = item.getProcesso();
                processo.setEquipe(transfer.getTargetEquipe());
                processo.setUsuario(targetResponsible);
                processo.setDataAtualizacao(LocalDateTime.now());
                processoRepository.save(processo);
            }
            transfer.setStatus(OfficeProcessTransferStatus.EXECUTED);
            transfer.setRespondedAt(Instant.now());
            transfer.setResponseByUserId(usuario.getId());
            transfer.setExecutedAt(Instant.now());
            transferRepository.save(transfer);
            auditLedgerService.appendSafely("ADV_OFFICE_PROCESS_TRANSFER_EXECUTED", "EQUIPE", String.valueOf(transfer.getTargetEquipe().getId()), "transfer=" + transfer.getId() + " count=" + transfer.getProcessCount());
            PjbFrontendOfficeProcessTransferView view = view(transfer, usuario, true, false);
            requestIdempotencyService.complete(requestHash, "ADV_OFFICE_PROCESS_TRANSFER", String.valueOf(transfer.getId()), Hashes.sha256Hex(view.toString()), view.toString());
            return view;
        } catch (RuntimeException ex) {
            requestIdempotencyService.fail(requestHash);
            throw ex;
        }
    }

    @Transactional
    public PjbFrontendOfficeProcessTransferView rejectTransfer(Long transferId) {
        Usuario usuario = currentUserService.getRequired();
        AdvOfficeProcessTransfer transfer = transferRepository.fetchById(transferId).orElseThrow(() -> new IllegalArgumentException("Transferencia nao encontrada."));
        if (!isActionable(transfer, usuario)) {
            throw new IllegalStateException("Usuario nao pode rejeitar esta transferencia.");
        }
        transfer.setStatus(OfficeProcessTransferStatus.REJECTED);
        transfer.setRespondedAt(Instant.now());
        transfer.setResponseByUserId(usuario.getId());
        transferRepository.save(transfer);
        auditLedgerService.appendSafely("ADV_OFFICE_PROCESS_TRANSFER_REJECTED", "EQUIPE", String.valueOf(transfer.getTargetEquipe().getId()), "transfer=" + transfer.getId());
        return view(transfer, usuario, true, false);
    }

    private RequestIdempotencyBeginResult beginIdempotent(String action, String requestHash) {
        try {
            return requestIdempotencyService.begin(action, requestHash, Duration.ofMinutes(5));
        } catch (IdempotencyInProgressException ex) {
            throw new IllegalStateException("Operacao ja esta em processamento para esta transferencia.");
        }
    }

    private boolean isSensitiveProcess(Processo processo) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return true;
        }
        return processo.getRamoDireito() != null && processo.getRamoDireito().isPenalLike();
    }

    private String buildPreviewSummary(List<Processo> processos, Equipe sourceEquipe, Equipe targetEquipe, Usuario targetResponsible) {
        String ramos = processos.stream()
                .map(Processo::getRamoDireito)
                .filter(Objects::nonNull)
                .map(Enum::name)
                .sorted()
                .distinct()
                .collect(Collectors.joining(", "));
        long sigilosos = processos.stream().filter(this::isSensitiveProcess).count();
        return "Transferencia formal de " + processos.size() + " processo(s) do escritorio " + sourceEquipe.getNome()
                + " para " + targetEquipe.getNome()
                + " sob responsabilidade de " + targetResponsible.getNome()
                + ". Ramos: " + (ramos.isBlank() ? "nao classificados" : ramos)
                + ". Itens sensiveis: " + sigilosos + ".";
    }

    private String buildImpactMaterial(List<Processo> processos, AdvOfficeProcessTransfer transfer) {
        String processIds = processos.stream()
                .sorted(Comparator.comparing(Processo::getId))
                .map(item -> String.valueOf(item.getId()))
                .collect(Collectors.joining(","));
        return String.join("|",
                String.valueOf(transfer.getSourceEquipe().getId()),
                String.valueOf(transfer.getTargetEquipe().getId()),
                String.valueOf(transfer.getTargetResponsibleUserId()),
                processIds,
                blankToNull(transfer.getMotivo()) == null ? "" : transfer.getMotivo());
    }

    private boolean isActionable(AdvOfficeProcessTransfer transfer, Usuario usuario) {
        if (transfer.getStatus().isTerminal()) {
            return false;
        }
        if (Objects.equals(transfer.getTargetResponsibleUserId(), usuario.getId())) {
            return true;
        }
        return canManage(transfer.getTargetEquipe().getId(), usuario.getId());
    }

    private PjbFrontendOfficeProcessTransferView view(AdvOfficeProcessTransfer transfer, Usuario usuario, boolean manageable, boolean actionable) {
        List<Long> processoIds = itemRepository.findByTransferId(transfer.getId()).stream()
                .map(item -> item.getProcesso().getId())
                .filter(Objects::nonNull)
                .toList();
        String targetResponsibleNome = usuarioRepository.findById(transfer.getTargetResponsibleUserId()).map(Usuario::getNome).orElse(null);
        return new PjbFrontendOfficeProcessTransferView(
                transfer.getId(),
                transfer.getSourceEquipe().getId(),
                transfer.getSourceEquipe().getNome(),
                transfer.getTargetEquipe().getId(),
                transfer.getTargetEquipe().getNome(),
                transfer.getTargetResponsibleUserId(),
                targetResponsibleNome,
                transfer.getStatus().name(),
                transfer.getProcessCount(),
                transfer.getSensitiveProcessCount(),
                transfer.getMotivo(),
                transfer.getEscopo(),
                processoIds,
                transfer.getPreviewSummary(),
                manageable,
                actionable,
                transfer.getCreatedAt(),
                transfer.getRespondedAt(),
                transfer.getExecutedAt());
    }

    private void assertManageable(Long equipeId, Long userId) {
        if (!canManage(equipeId, userId)) {
            throw new IllegalStateException("Usuario nao pode gerenciar transferencias deste escritorio.");
        }
    }

    private boolean canManage(Long equipeId, Long userId) {
        AdvOfficeWorkspaceProfile profile = profileRepository.findByEquipe_Id(equipeId).orElse(null);
        if (profile != null && Objects.equals(profile.getOwnerUserId(), userId)) {
            return true;
        }
        EquipeOfficePolicy policy = policyRepository.findByEquipeId(equipeId).orElse(null);
        if (policy != null && Objects.equals(policy.getSignerUserId(), userId)) {
            return true;
        }
        MembroEquipe membro = membroEquipeRepository.findByUsuario_IdAndEquipe_Id(userId, equipeId).orElse(null);
        return membro != null && membro.isAtivo() && (membro.getPapel() == PapelEquipe.ADMINISTRADOR || membro.getPapel() == PapelEquipe.COORDENADOR || membro.getPapel() == PapelEquipe.ADVOGADO_SENIOR);
    }

    private String defaultIdempotencyKey(String raw, Long userId) {
        if (raw != null && !raw.isBlank()) {
            return raw.trim();
        }
        return "U" + userId;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
