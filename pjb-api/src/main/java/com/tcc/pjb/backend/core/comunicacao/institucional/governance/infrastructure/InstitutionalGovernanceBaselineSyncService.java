package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCompetenceRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaGovernancaInstitucional;

@Component
public class InstitutionalGovernanceBaselineSyncService {

    private final CatalogoInstitucionalUnificadoService catalogo;
    private final InstitutionalCatalogGovernanceStateRepository governanceRepository;
    private final InstitutionalCompetenceRuleStateRepository competenceRuleRepository;
    private final Clock clock;

    public InstitutionalGovernanceBaselineSyncService(CatalogoInstitucionalUnificadoService catalogo,
                                                      InstitutionalCatalogGovernanceStateRepository governanceRepository,
                                                      InstitutionalCompetenceRuleStateRepository competenceRuleRepository,
                                                      Clock clock) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.governanceRepository = Objects.requireNonNull(governanceRepository, "governanceRepository");
        this.competenceRuleRepository = Objects.requireNonNull(competenceRuleRepository, "competenceRuleRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        Instant now = clock.instant();
        for (UnidadeInstitucional unit : catalogo.listarPorTipo(null)) {
            String governanceId = "BASE-" + unit.codigo();
            if (governanceRepository.findByGovernanceId(governanceId).isEmpty()) {
                governanceRepository.save(new InstitutionalCatalogGovernanceEntry(
                        governanceId,
                        unit.codigo(),
                        unit.destinatarioKind(),
                        unit.uf(),
                        unit.comarca(),
                        unit.foro(),
                        unit.ramoDireito(),
                        unit.grauJurisdicao(),
                        unit.uf() == null ? AbrangenciaGovernancaInstitucional.NACIONAL : AbrangenciaGovernancaInstitucional.UF,
                        now,
                        null,
                        unit.ativa(),
                        false,
                        false,
                        unit.canais().stream().map(c -> c.canal()).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                        null,
                        "Carga basal da governança institucional nacional.",
                        "BOOTSTRAP_CATALOGO_BASE",
                        now,
                        now));
            }
            String ruleId = "RULE-BASE-" + unit.codigo();
            if (competenceRuleRepository.findByRuleId(ruleId).isEmpty()) {
                competenceRuleRepository.save(new InstitutionalCompetenceRule(
                        ruleId,
                        unit.destinatarioKind(),
                        unit.papelPrincipal(),
                        unit.uf(),
                        unit.comarca(),
                        unit.foro(),
                        unit.ramoDireito(),
                        unit.grauJurisdicao(),
                        unit.codigo(),
                        10,
                        now,
                        null,
                        true,
                        "BOOTSTRAP_CATALOGO_BASE",
                        "Regra basal derivada do catálogo unificado.",
                        now,
                        now));
            }
        }
    }
}
