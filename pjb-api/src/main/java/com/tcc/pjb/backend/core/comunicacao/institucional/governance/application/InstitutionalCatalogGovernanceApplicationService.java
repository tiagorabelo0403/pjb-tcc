package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogCoverageItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogCoverageSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalNationalExpansionResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRegionalExpansionResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCompetenceRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaGovernancaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import jakarta.inject.Inject;

@Service
public class InstitutionalCatalogGovernanceApplicationService {

    private final CatalogoInstitucionalUnificadoService catalogo;
    private final InstitutionalCatalogGovernanceOverlayService overlayService;
    private final Clock clock;

    @Inject
    public InstitutionalCatalogGovernanceApplicationService(CatalogoInstitucionalUnificadoService catalogo,
                                                            InstitutionalCatalogGovernanceOverlayService overlayService) {
        this(catalogo, overlayService, Clock.systemUTC());
    }

    InstitutionalCatalogGovernanceApplicationService(CatalogoInstitucionalUnificadoService catalogo,
                                                     InstitutionalCatalogGovernanceOverlayService overlayService,
                                                     Clock clock) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.overlayService = Objects.requireNonNull(overlayService, "overlayService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<InstitutionalCatalogGovernanceEntry> listGovernances(DestinatarioInstitucionalKind kind, String uf) {
        return overlayService.listGovernances(kind, uf);
    }

    public List<InstitutionalCompetenceRule> listCompetenceRules(DestinatarioInstitucionalKind kind, String uf) {
        return overlayService.listCompetenceRules(kind, uf);
    }

    public InstitutionalCatalogGovernanceSummary summarize() {
        return overlayService.summarize(catalogo.listarPorTipo(null).size(), catalogo.version());
    }

    public InstitutionalCatalogCoverageSummary coverageSummary() {
        List<String> ufsNacionais = List.of(
                "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
                "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        );
        List<InstitutionalCatalogCoverageItem> itens = new ArrayList<>();
        for (DestinatarioInstitucionalKind kind : DestinatarioInstitucionalKind.values()) {
            List<com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional> unidades = catalogo.listarPorTipo(kind).stream()
                    .filter(com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional::ativa)
                    .toList();
            List<String> cobertas = unidades.stream()
                    .map(com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional::uf)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .sorted()
                    .toList();
            List<String> faltantes = ufsNacionais.stream().filter(uf -> !cobertas.contains(uf)).toList();
            itens.add(new InstitutionalCatalogCoverageItem(
                    kind,
                    cobertas.size(),
                    unidades.size(),
                    cobertas,
                    faltantes
            ));
        }
        return new InstitutionalCatalogCoverageSummary(itens, catalogo.version(), clock.instant());
    }

    public InstitutionalCatalogGovernanceEntry saveGovernance(String governanceId,
                                                              String unidadeCodigo,
                                                              DestinatarioInstitucionalKind destinatarioKind,
                                                              String uf,
                                                              String comarca,
                                                              String foro,
                                                              RamoDireito ramoDireito,
                                                              GrauJurisdicao grauJurisdicao,
                                                              AbrangenciaGovernancaInstitucional abrangencia,
                                                              Instant vigenciaInicio,
                                                              Instant vigenciaFim,
                                                              boolean ativa,
                                                              boolean suspendeEntregaExterna,
                                                              boolean exigeHomologacaoAdministrativa,
                                                              Set<CanalComunicacaoInstitucional> canaisPreferenciais,
                                                              String unidadeSubstitutaCodigo,
                                                              String fundamentoAdministrativo,
                                                              String origem) {
        Instant now = clock.instant();
        return overlayService.saveGovernance(new InstitutionalCatalogGovernanceEntry(
                governanceId,
                unidadeCodigo,
                destinatarioKind,
                uf,
                comarca,
                foro,
                ramoDireito,
                grauJurisdicao,
                Objects.requireNonNull(abrangencia, "abrangencia"),
                vigenciaInicio == null ? now : vigenciaInicio,
                vigenciaFim,
                ativa,
                suspendeEntregaExterna,
                exigeHomologacaoAdministrativa,
                canaisPreferenciais,
                unidadeSubstitutaCodigo,
                fundamentoAdministrativo,
                origem == null || origem.isBlank() ? "ADMIN_MANUAL" : origem,
                now,
                now));
    }


    public InstitutionalNationalExpansionResult seedNationalSpecializedBaseline() {
        List<String> ufsNacionais = List.of(
                "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
                "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        );
        Instant now = clock.instant();
        long regrasCriadas = 0;
        for (String uf : ufsNacionais) {
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA, uf, RamoDireito.FAMILIA, "MINISTERIO_PUBLICO-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA, uf, RamoDireito.INFANCIA_JUVENTUDE, "MINISTERIO_PUBLICO-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE, uf, RamoDireito.CIVIL, "DEFENSORIA_PUBLICA-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE, uf, RamoDireito.PENAL, "DEFENSORIA_PUBLICA-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.FAZENDA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE, uf, RamoDireito.TRIBUTARIO, "FAZENDA_PUBLICA-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.CEJUSC, PapelProcessualInstitucional.AUXILIAR_JUSTICA, uf, RamoDireito.FAMILIA, "CEJUSC-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.UNIDADE_PRISIONAL, PapelProcessualInstitucional.UNIDADE_EXECUTORA, uf, RamoDireito.PENAL, "UNIDADE_PRISIONAL-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.POLICIA_PENAL, PapelProcessualInstitucional.UNIDADE_EXECUTORA, uf, RamoDireito.PENAL, "POLICIA_PENAL-" + uf);
            regrasCriadas += upsertBaselineRule(DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL, PapelProcessualInstitucional.APOIO_TECNICO, uf, RamoDireito.FAMILIA, "EQUIPE_PSICOSSOCIAL-" + uf);
        }
        return new InstitutionalNationalExpansionResult(
                regrasCriadas,
                0,
                catalogo.version(),
                now,
                List.of(
                        "baseline especializado criado para família, infância, penal e tributário em todas as UFs",
                        "as regras preservam governança administrativa e podem ser refinadas por comarca/foro"
                )
        );
    }

    public InstitutionalRegionalExpansionResult seedRegionalSpecializedBaseline(String uf, String comarca, String foro) {
        String ufUp = uf == null ? null : uf.trim().toUpperCase(java.util.Locale.ROOT);
        String comarcaNorm = comarca == null ? null : comarca.trim();
        String foroNorm = foro == null || foro.isBlank() ? null : foro.trim();
        Instant now = clock.instant();
        List<com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional> candidatas = catalogo.listarPorTipo(null).stream()
                .filter(com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional::ativa)
                .filter(unidade -> ufUp == null || ufUp.equalsIgnoreCase(unidade.uf()))
                .filter(unidade -> comarcaNorm == null || (unidade.comarca() != null && unidade.comarca().equalsIgnoreCase(comarcaNorm)))
                .filter(unidade -> foroNorm == null || (unidade.foro() != null && unidade.foro().equalsIgnoreCase(foroNorm)))
                .toList();
        long regrasCriadas = 0;
        long regrasAtualizadas = 0;
        for (com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional unidade : candidatas) {
            boolean exists = overlayService.listCompetenceRules(unidade.destinatarioKind(), ufUp).stream()
                    .anyMatch(rule -> rule.matches(unidade.destinatarioKind(), unidade.papelPrincipal(), ufUp, comarcaNorm, foroNorm, unidade.ramoDireito(), unidade.grauJurisdicao(), now)
                            && unidade.codigo().equalsIgnoreCase(rule.unidadeCodigo()));
            if (exists) {
                regrasAtualizadas++;
                continue;
            }
            saveCompetenceRule(
                    null,
                    unidade.destinatarioKind(),
                    unidade.papelPrincipal(),
                    ufUp,
                    comarcaNorm,
                    foroNorm,
                    unidade.ramoDireito(),
                    unidade.grauJurisdicao(),
                    unidade.codigo(),
                    400,
                    now,
                    null,
                    true,
                    "BASELINE_REGIONAL_AUTO",
                    "expansão regional automática por comarca/foro"
            );
            regrasCriadas++;
        }
        return new InstitutionalRegionalExpansionResult(
                ufUp,
                comarcaNorm,
                foroNorm,
                regrasCriadas,
                regrasAtualizadas,
                catalogo.version(),
                now,
                List.of(
                        "baseline regional especializado criado a partir das unidades já catalogadas",
                        "regras regionais recebem prioridade superior à baseline nacional"
                )
        );
    }

    private long upsertBaselineRule(DestinatarioInstitucionalKind destinatarioKind,
                                    PapelProcessualInstitucional papelProcessual,
                                    String uf,
                                    RamoDireito ramoDireito,
                                    String unidadeCodigo) {
        boolean exists = overlayService.listCompetenceRules(destinatarioKind, uf).stream()
                .anyMatch(rule -> rule.matches(destinatarioKind, papelProcessual, uf, null, null, ramoDireito, null, clock.instant())
                        && unidadeCodigo.equalsIgnoreCase(rule.unidadeCodigo()));
        if (exists) {
            return 0;
        }
        saveCompetenceRule(
                null,
                destinatarioKind,
                papelProcessual,
                uf,
                null,
                null,
                ramoDireito,
                null,
                unidadeCodigo,
                250,
                clock.instant(),
                null,
                true,
                "BASELINE_NACIONAL_AUTO",
                "expansão automática nacional do catálogo institucional"
        );
        return 1;
    }

    public InstitutionalCompetenceRule saveCompetenceRule(String ruleId,
                                                          DestinatarioInstitucionalKind destinatarioKind,
                                                          PapelProcessualInstitucional papelProcessual,
                                                          String uf,
                                                          String comarca,
                                                          String foro,
                                                          RamoDireito ramoDireito,
                                                          GrauJurisdicao grauJurisdicao,
                                                          String unidadeCodigo,
                                                          int prioridade,
                                                          Instant vigenciaInicio,
                                                          Instant vigenciaFim,
                                                          boolean ativa,
                                                          String origem,
                                                          String fundamentoAdministrativo) {
        Instant now = clock.instant();
        return overlayService.saveCompetenceRule(new InstitutionalCompetenceRule(
                ruleId,
                destinatarioKind,
                papelProcessual,
                uf,
                comarca,
                foro,
                ramoDireito,
                grauJurisdicao,
                unidadeCodigo,
                prioridade,
                vigenciaInicio == null ? now : vigenciaInicio,
                vigenciaFim,
                ativa,
                origem == null || origem.isBlank() ? "ADMIN_MANUAL" : origem,
                fundamentoAdministrativo,
                now,
                now));
    }
}
