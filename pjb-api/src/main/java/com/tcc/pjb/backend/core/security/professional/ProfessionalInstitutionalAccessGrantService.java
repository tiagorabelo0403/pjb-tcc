package com.tcc.pjb.backend.core.security.professional;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProfessionalInstitutionalAccessGrantService {

    private final ProfessionalInstitutionalAccessGrantRepository repository;

    public ProfessionalInstitutionalAccessGrantService(ProfessionalInstitutionalAccessGrantRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public GrantResolution resolveApplicable(Usuario usuario,
                                             Processo processo,
                                             ProfessionalActorClass actorClass) {
        if (usuario == null || usuario.getId() == null || actorClass == null) {
            return GrantResolution.empty();
        }
        LocalDateTime reference = LocalDateTime.now();
        List<ProfessionalInstitutionalAccessGrant> grants = repository.findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(usuario.getId()).stream()
                .filter(item -> item != null && item.isAtivoNaJanela(reference))
                .filter(item -> item.getActorClass() == actorClass)
                .filter(item -> matches(item, processo))
                .toList();
        if (grants.isEmpty()) {
            return GrantResolution.empty();
        }
        boolean relatoria = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.RELATORIA_PROCESSO);
        boolean colegiado = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO);
        boolean substituicao = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.SUBSTITUICAO);
        boolean plantao = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.PLANTAO);
        boolean delegacao = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.DELEGACAO_GABINETE);
        boolean processBound = grants.stream().anyMatch(item -> item.getProcesso() != null && item.getProcesso().getId() != null);
        boolean territorial = grants.stream().anyMatch(item -> item.getProcesso() == null && (notBlank(item.getUf()) || notBlank(item.getComarca()) || notBlank(item.getTribunal()) || notBlank(item.getUnidadeJudiciariaCodigo())));
        boolean designacao = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.DESIGNACAO_PROCESSO || item.getGrantType() == ProfessionalAccessGrantType.DESIGNACAO_TERRITORIAL);
        boolean representacao = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.REPRESENTACAO_PROCESSO || item.getGrantType() == ProfessionalAccessGrantType.REPRESENTACAO_ENTE);
        boolean lotacao = grants.stream().anyMatch(item -> item.getGrantType() == ProfessionalAccessGrantType.LOTACAO_UNIDADE || item.getGrantType() == ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL);
        boolean requiresStepUp = grants.stream().anyMatch(ProfessionalInstitutionalAccessGrant::requiresStepUp);
        List<String> lineage = grants.stream().map(this::lineageLabel).toList();
        String anchor = grants.stream().map(this::anchor).filter(this::notBlank).distinct().collect(Collectors.joining(" | "));
        return new GrantResolution(List.copyOf(grants), List.copyOf(lineage), processBound, territorial, designacao, representacao, lotacao, relatoria, colegiado, substituicao, plantao, delegacao, requiresStepUp, anchor);
    }

    private boolean matches(ProfessionalInstitutionalAccessGrant grant, Processo processo) {
        if (grant == null) {
            return false;
        }
        if (processo == null) {
            return grant.getProcesso() == null;
        }
        if (grant.getProcesso() != null && grant.getProcesso().getId() != null) {
            return processo.getId() != null && grant.getProcesso().getId().equals(processo.getId());
        }
        if (notBlank(grant.getUf()) && !equalsNormalized(grant.getUf(), processo.getUf())) {
            return false;
        }
        if (notBlank(grant.getComarca()) && !equalsNormalized(grant.getComarca(), processo.getComarca())) {
            return false;
        }
        if (notBlank(grant.getTribunal()) && !equalsNormalized(grant.getTribunal(), processo.getTribunal())) {
            return false;
        }
        if (notBlank(grant.getUnidadeJudiciariaCodigo()) && !equalsNormalized(grant.getUnidadeJudiciariaCodigo(), processo.getUnidadeJudiciariaCodigo())) {
            return false;
        }
        return true;
    }

    private String lineageLabel(ProfessionalInstitutionalAccessGrant grant) {
        List<String> parts = new ArrayList<>();
        parts.add(grant.getGrantType().displayName());
        if (grant.getAccessBasis() != null) {
            parts.add(grant.getAccessBasis().displayName());
        }
        if (notBlank(grant.getSourceLabel())) {
            parts.add(grant.getSourceLabel().trim());
        }
        String anchor = anchor(grant);
        if (notBlank(anchor)) {
            parts.add(anchor);
        }
        return String.join(" • ", parts);
    }

    private String anchor(ProfessionalInstitutionalAccessGrant grant) {
        List<String> parts = new ArrayList<>();
        if (notBlank(grant.getUf())) {
            parts.add(grant.getUf().trim().toUpperCase(Locale.ROOT));
        }
        if (notBlank(grant.getComarca())) {
            parts.add(grant.getComarca().trim());
        }
        if (notBlank(grant.getTribunal())) {
            parts.add(grant.getTribunal().trim().toUpperCase(Locale.ROOT));
        }
        if (notBlank(grant.getUnidadeJudiciariaCodigo())) {
            parts.add(grant.getUnidadeJudiciariaCodigo().trim().toUpperCase(Locale.ROOT));
        }
        if (notBlank(grant.getOrgaoColegiadoCodigo())) {
            parts.add(grant.getOrgaoColegiadoCodigo().trim().toUpperCase(Locale.ROOT));
        }
        return String.join(" / ", parts);
    }

    private boolean equalsNormalized(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public record GrantResolution(
            List<ProfessionalInstitutionalAccessGrant> grants,
            List<String> lineage,
            boolean processBound,
            boolean territorial,
            boolean formalDesignation,
            boolean formalRepresentation,
            boolean lotacao,
            boolean relatoria,
            boolean colegiado,
            boolean substituicao,
            boolean plantao,
            boolean delegatedCabinet,
            boolean requiresStepUp,
            String organizationalAnchor
    ) {
        public static GrantResolution empty() {
            return new GrantResolution(List.of(), List.of(), false, false, false, false, false, false, false, false, false, false, false, "");
        }

        public boolean hasFormalInstitutionalCoverage() {
            return formalDesignation || formalRepresentation || lotacao;
        }

        public boolean hasFormalJudicialCoverage() {
            return relatoria || colegiado || substituicao || plantao;
        }

        public boolean hasAnyGrant() {
            return !grants.isEmpty();
        }
    }
}
