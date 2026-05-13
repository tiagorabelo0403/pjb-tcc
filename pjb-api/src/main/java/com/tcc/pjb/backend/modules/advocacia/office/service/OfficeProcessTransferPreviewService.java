package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferImpactItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferPreviewView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeTrustLevel;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeProcessTransferPreviewService {

    private static final int MIN_SENSITIVE_TRUST = 7;
    private static final Set<RamoDireito> SENSITIVE_RAMOS = EnumSet.of(
            RamoDireito.PENAL,
            RamoDireito.PROCESSUAL_PENAL,
            RamoDireito.EXECUCAO_PENAL,
            RamoDireito.MILITAR,
            RamoDireito.INFANCIA_JUVENTUDE
    );

    private final CurrentUserService currentUserService;
    private final EquipeRepository equipeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final EquipeOfficePolicyRepository policyRepository;
    private final EquipeOfficeDelegacaoRegraRepository regraRepository;
    private final OfficeTrustScoreService trustScoreService;

    public OfficeProcessTransferPreviewService(CurrentUserService currentUserService,
                                               EquipeRepository equipeRepository,
                                               UsuarioRepository usuarioRepository,
                                               ProcessoRepository processoRepository,
                                               MembroEquipeRepository membroEquipeRepository,
                                               EquipeOfficePolicyRepository policyRepository,
                                               EquipeOfficeDelegacaoRegraRepository regraRepository,
                                               OfficeTrustScoreService trustScoreService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.equipeRepository = Objects.requireNonNull(equipeRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.regraRepository = Objects.requireNonNull(regraRepository);
        this.trustScoreService = Objects.requireNonNull(trustScoreService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeProcessTransferPreviewView preview(FrontendOfficeProcessTransferRequest request) {
        currentUserService.getRequired();
        Equipe sourceEquipe = equipeRepository.findById(request.sourceEquipeId()).orElseThrow(() -> new IllegalArgumentException("Escritorio de origem nao encontrado."));
        Equipe targetEquipe = equipeRepository.findById(request.targetEquipeId()).orElseThrow(() -> new IllegalArgumentException("Escritorio de destino nao encontrado."));
        Usuario targetResponsible = usuarioRepository.findById(request.targetResponsibleUserId()).orElseThrow(() -> new IllegalArgumentException("Responsavel de destino nao encontrado."));
        List<Long> orderedIds = request.processoIds().stream().filter(Objects::nonNull).distinct().sorted().toList();
        List<Processo> processos = processoRepository.findAllById(orderedIds);
        if (processos.size() != orderedIds.size()) {
            throw new IllegalArgumentException("Um ou mais processos informados nao existem.");
        }
        return evaluate(sourceEquipe, targetEquipe, targetResponsible, processos);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeProcessTransferPreviewView previewExisting(Long sourceEquipeId,
                                                                       Long targetEquipeId,
                                                                       Long targetResponsibleUserId,
                                                                       List<Processo> processos) {
        currentUserService.getRequired();
        Equipe sourceEquipe = equipeRepository.findById(sourceEquipeId).orElseThrow(() -> new IllegalArgumentException("Escritorio de origem nao encontrado."));
        Equipe targetEquipe = equipeRepository.findById(targetEquipeId).orElseThrow(() -> new IllegalArgumentException("Escritorio de destino nao encontrado."));
        Usuario targetResponsible = usuarioRepository.findById(targetResponsibleUserId).orElseThrow(() -> new IllegalArgumentException("Responsavel de destino nao encontrado."));
        return evaluate(sourceEquipe, targetEquipe, targetResponsible, processos);
    }

    private PjbFrontendOfficeProcessTransferPreviewView evaluate(Equipe sourceEquipe,
                                                                 Equipe targetEquipe,
                                                                 Usuario targetResponsible,
                                                                 List<Processo> processos) {
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();

        if (Objects.equals(sourceEquipe.getId(), targetEquipe.getId())) {
            blockers.add("SOURCE_TARGET_EQUAL");
        }

        MembroEquipe targetMembership = membroEquipeRepository.findByUsuario_IdAndEquipe_Id(targetResponsible.getId(), targetEquipe.getId()).orElse(null);
        if (targetMembership == null || !targetMembership.isAtivo()) {
            blockers.add("TARGET_MEMBERSHIP_INACTIVE");
        }

        EquipeOfficePolicy policy = policyRepository.findByEquipeId(targetEquipe.getId()).orElse(null);
        EquipeOfficeDelegacaoRegra regra = regraRepository.findByEquipeAndUser(targetEquipe.getId(), targetResponsible.getId()).orElse(null);
        OfficeTrustScoreService.TrustScore trust = trustScoreService.avaliar(targetResponsible.getId(), targetEquipe.getId());
        if (trust.frozen()) {
            blockers.add("TARGET_SECURITY_FROZEN");
        }

        Set<RamoDireito> allowedRamos = effectiveAllowedRamos(policy, regra);
        boolean canViewAllRamos = allowedRamos.isEmpty();
        int minTrustRequired = effectiveMinTrust(policy, regra);
        Long signerUserId = policy == null ? null : policy.getSignerUserId();
        if (policy != null && policy.isEnabled() && policy.isForcePatronoCertificate() && policy.getSignerUserId() == null) {
            blockers.add("TARGET_OFFICE_WITHOUT_SIGNER");
        }
        if (trust.newcomer()) {
            warnings.add("TARGET_RESPONSIBLE_NEWCOMER");
        }

        List<PjbFrontendOfficeProcessTransferImpactItemView> items = processos.stream()
                .sorted(Comparator.comparing(Processo::getId))
                .map(processo -> evaluateItem(sourceEquipe, targetResponsible, targetMembership, processo, policy, regra, allowedRamos, canViewAllRamos, trust, minTrustRequired, signerUserId))
                .toList();

        int sensitiveProcessCount = (int) items.stream().filter(PjbFrontendOfficeProcessTransferImpactItemView::sensitive).count();
        boolean requiresManualReview = items.stream().anyMatch(PjbFrontendOfficeProcessTransferImpactItemView::requiresManualReview);
        items.stream().flatMap(item -> item.blockers().stream()).distinct().forEach(blockers::add);
        items.stream().flatMap(item -> item.warnings().stream()).distinct().forEach(warnings::add);

        String previewSummary = buildPreviewSummary(sourceEquipe, targetEquipe, targetResponsible, items, canViewAllRamos, allowedRamos, trust.score(), minTrustRequired);
        String previewHash = buildPreviewHash(sourceEquipe, targetEquipe, targetResponsible, policy, regra, trust.score(), minTrustRequired, items);
        return new PjbFrontendOfficeProcessTransferPreviewView(
                sourceEquipe.getId(),
                sourceEquipe.getNome(),
                targetEquipe.getId(),
                targetEquipe.getNome(),
                targetResponsible.getId(),
                targetResponsible.getNome(),
                items.size(),
                sensitiveProcessCount,
                blockers.isEmpty(),
                requiresManualReview,
                previewSummary,
                previewHash,
                trust.score(),
                OfficeTrustLevel.fromScore(trust.score()).name(),
                minTrustRequired,
                canViewAllRamos,
                canViewAllRamos ? enumNames(RamoDireito.values()) : sortedRamos(allowedRamos),
                List.copyOf(new LinkedHashSet<>(blockers)),
                List.copyOf(new LinkedHashSet<>(warnings)),
                items);
    }

    private PjbFrontendOfficeProcessTransferImpactItemView evaluateItem(Equipe sourceEquipe,
                                                                        Usuario targetResponsible,
                                                                        MembroEquipe targetMembership,
                                                                        Processo processo,
                                                                        EquipeOfficePolicy policy,
                                                                        EquipeOfficeDelegacaoRegra regra,
                                                                        Set<RamoDireito> allowedRamos,
                                                                        boolean canViewAllRamos,
                                                                        OfficeTrustScoreService.TrustScore trust,
                                                                        int minTrustRequired,
                                                                        Long signerUserId) {
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        boolean sensitive = isSensitiveProcess(processo);

        if (processo.getEquipe() == null || !Objects.equals(processo.getEquipe().getId(), sourceEquipe.getId())) {
            blockers.add("PROCESS_SOURCE_MISMATCH");
        }
        if (!canViewAllRamos && processo.getRamoDireito() != null && !allowedRamos.contains(processo.getRamoDireito())) {
            blockers.add("RAMO_NAO_AUTORIZADO");
        }
        if (targetMembership == null || !targetMembership.isAtivo()) {
            blockers.add("TARGET_MEMBERSHIP_INACTIVE");
        }
        if (targetMembership != null && blocksPersonalCases(policy, regra, targetMembership.getPapel()) && isPersonalCase(processo, targetResponsible)) {
            blockers.add("PERSONAL_SCOPE_BLOCKED");
        }
        if (sensitive && trust.score() < Math.max(minTrustRequired, MIN_SENSITIVE_TRUST)) {
            blockers.add(processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO
                    ? "TRUST_INSUFICIENTE_PARA_SIGILO"
                    : "TRUST_INSUFICIENTE_PARA_RAMO_SENSIVEL");
        }
        if (policy != null && policy.isEnabled() && policy.isForcePatronoCertificate()) {
            if (policy.getSignerUserId() == null) {
                blockers.add("TARGET_OFFICE_WITHOUT_SIGNER");
            } else if (!Objects.equals(policy.getSignerUserId(), targetResponsible.getId())) {
                warnings.add("ASSINATURA_PATRONAL_OBRIGATORIA");
            }
        }
        if (sensitive && trust.newcomer()) {
            warnings.add("REVIEW_OBRIGATORIA_POR_NOVO_VINCULO");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            warnings.add("PROCESSO_SIGILOSO");
        }

        return new PjbFrontendOfficeProcessTransferImpactItemView(
                processo.getId(),
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()),
                processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name(),
                sensitive,
                !blockers.isEmpty(),
                !warnings.isEmpty(),
                List.copyOf(new LinkedHashSet<>(blockers)),
                List.copyOf(new LinkedHashSet<>(warnings)));
    }

    private boolean blocksPersonalCases(EquipeOfficePolicy policy, EquipeOfficeDelegacaoRegra regra, PapelEquipe papelEquipe) {
        if (papelEquipe == PapelEquipe.ADMINISTRADOR || papelEquipe == PapelEquipe.COORDENADOR) {
            return false;
        }
        if (regra != null && regra.isBloqueiaPessoal()) {
            return true;
        }
        return policy != null && policy.isEnabled() && policy.isBloqueiaCausasProprias();
    }

    private boolean isPersonalCase(Processo processo, Usuario usuario) {
        if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuario.getId())) {
            return true;
        }
        String cpf = normalizeDigits(usuario.getCpf());
        if (cpf == null) {
            return false;
        }
        return cpf.equals(normalizeDigits(processo.getParteAutoraCpf())) || cpf.equals(normalizeDigits(processo.getParteReuCpf()));
    }

    private Set<RamoDireito> effectiveAllowedRamos(EquipeOfficePolicy policy, EquipeOfficeDelegacaoRegra regra) {
        if (regra != null && regra.getAllowedRamosOverride() != null && !regra.getAllowedRamosOverride().isEmpty()) {
            return regra.getAllowedRamosOverride();
        }
        if (policy != null && policy.getAllowedRamos() != null && !policy.getAllowedRamos().isEmpty()) {
            return policy.getAllowedRamos();
        }
        return EnumSet.noneOf(RamoDireito.class);
    }

    private int effectiveMinTrust(EquipeOfficePolicy policy, EquipeOfficeDelegacaoRegra regra) {
        if (regra != null && regra.getMinTrustAutoOverride() != null) {
            return regra.getMinTrustAutoOverride();
        }
        return policy == null ? 0 : policy.getMinTrustAuto();
    }

    private boolean isSensitiveProcess(Processo processo) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return true;
        }
        return processo.getRamoDireito() != null && (processo.getRamoDireito().isPenalLike() || SENSITIVE_RAMOS.contains(processo.getRamoDireito()));
    }

    private String buildPreviewSummary(Equipe sourceEquipe,
                                       Equipe targetEquipe,
                                       Usuario targetResponsible,
                                       List<PjbFrontendOfficeProcessTransferImpactItemView> items,
                                       boolean canViewAllRamos,
                                       Set<RamoDireito> allowedRamos,
                                       int trustScore,
                                       int minTrustRequired) {
        long blocked = items.stream().filter(PjbFrontendOfficeProcessTransferImpactItemView::blocked).count();
        long sensitive = items.stream().filter(PjbFrontendOfficeProcessTransferImpactItemView::sensitive).count();
        String ramos = canViewAllRamos ? "todos os ramos" : sortedRamos(allowedRamos).stream().collect(Collectors.joining(", "));
        return "Preview de transferencia de " + items.size() + " processo(s) do escritorio " + sourceEquipe.getNome()
                + " para " + targetEquipe.getNome()
                + " sob responsabilidade de " + targetResponsible.getNome()
                + ". Ramos autorizados no destino: " + ramos
                + ". Trust atual/minimo: " + trustScore + "/" + minTrustRequired
                + ". Itens sensiveis: " + sensitive
                + ". Itens bloqueados: " + blocked + ".";
    }

    private String buildPreviewHash(Equipe sourceEquipe,
                                    Equipe targetEquipe,
                                    Usuario targetResponsible,
                                    EquipeOfficePolicy policy,
                                    EquipeOfficeDelegacaoRegra regra,
                                    int trustScore,
                                    int minTrustRequired,
                                    List<PjbFrontendOfficeProcessTransferImpactItemView> items) {
        String itemMaterial = items.stream()
                .map(item -> String.join(":",
                        String.valueOf(item.processoId()),
                        nullSafe(item.numeroProcesso()),
                        nullSafe(item.ramoDireito()),
                        nullSafe(item.nivelSigilo()),
                        Boolean.toString(item.blocked()),
                        String.join(",", item.blockers()),
                        String.join(",", item.warnings())))
                .collect(Collectors.joining("|"));
        String policyMaterial = String.join("|",
                String.valueOf(sourceEquipe.getId()),
                String.valueOf(targetEquipe.getId()),
                String.valueOf(targetResponsible.getId()),
                policy == null ? "" : String.valueOf(policy.getSignerUserId()),
                policy == null ? "" : Boolean.toString(policy.isForcePatronoCertificate()),
                regra == null ? "" : Boolean.toString(regra.isBloqueiaPessoal()),
                String.valueOf(trustScore),
                String.valueOf(minTrustRequired));
        return Hashes.sha256Hex(policyMaterial + "|" + itemMaterial);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private List<String> sortedRamos(Set<RamoDireito> ramos) {
        return ramos == null ? List.of() : ramos.stream().map(Enum::name).sorted().toList();
    }

    private List<String> enumNames(Enum<?>[] values) {
        return java.util.Arrays.stream(values).map(Enum::name).sorted().toList();
    }
}
