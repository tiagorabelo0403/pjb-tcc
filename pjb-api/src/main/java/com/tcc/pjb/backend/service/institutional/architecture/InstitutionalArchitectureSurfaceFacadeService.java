package com.tcc.pjb.backend.service.institutional.architecture;

import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalCatalogGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogCoverageSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceSummary;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalArchitectureResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.security.govbr.GovBrAccountEntryGovernanceResponse;
import com.tcc.pjb.backend.model.dto.security.govbr.GovBrIdentityAssuranceResponse;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.security.govbr.GovBrSurfaceFacadeService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

@Service
public class InstitutionalArchitectureSurfaceFacadeService {

    private static final String BLUEPRINT_VERSION = "PJB-INSTITUCIONAL-2026.04-R1";

    private final CatalogoInstitucionalUnificadoService catalogoService;
    private final InstitutionalCatalogGovernanceApplicationService governanceApplicationService;
    private final GovBrSurfaceFacadeService govBrSurfaceFacadeService;
    private final FederalismoJudicialEngine federalismoJudicialEngine;
    private final InstitutionalVisibilityGuardrailService visibilityGuardrailService;
    private final InstitutionalFederatedShardBlueprintService federatedShardBlueprintService;
    private final InstitutionalPublicRecognitionPolicyService publicRecognitionPolicyService;
    private final Clock clock;

    @Inject
    public InstitutionalArchitectureSurfaceFacadeService(CatalogoInstitucionalUnificadoService catalogoService,
                                                         InstitutionalCatalogGovernanceApplicationService governanceApplicationService,
                                                         GovBrSurfaceFacadeService govBrSurfaceFacadeService,
                                                         FederalismoJudicialEngine federalismoJudicialEngine,
                                                         InstitutionalVisibilityGuardrailService visibilityGuardrailService,
                                                         InstitutionalFederatedShardBlueprintService federatedShardBlueprintService,
                                                         InstitutionalPublicRecognitionPolicyService publicRecognitionPolicyService) {
        this(catalogoService,
                governanceApplicationService,
                govBrSurfaceFacadeService,
                federalismoJudicialEngine,
                visibilityGuardrailService,
                federatedShardBlueprintService,
                publicRecognitionPolicyService,
                Clock.systemUTC());
    }

    InstitutionalArchitectureSurfaceFacadeService(CatalogoInstitucionalUnificadoService catalogoService,
                                                  InstitutionalCatalogGovernanceApplicationService governanceApplicationService,
                                                  GovBrSurfaceFacadeService govBrSurfaceFacadeService,
                                                  FederalismoJudicialEngine federalismoJudicialEngine,
                                                  InstitutionalVisibilityGuardrailService visibilityGuardrailService,
                                                  InstitutionalFederatedShardBlueprintService federatedShardBlueprintService,
                                                  InstitutionalPublicRecognitionPolicyService publicRecognitionPolicyService,
                                                  Clock clock) {
        this.catalogoService = Objects.requireNonNull(catalogoService);
        this.governanceApplicationService = Objects.requireNonNull(governanceApplicationService);
        this.govBrSurfaceFacadeService = Objects.requireNonNull(govBrSurfaceFacadeService);
        this.federalismoJudicialEngine = Objects.requireNonNull(federalismoJudicialEngine);
        this.visibilityGuardrailService = Objects.requireNonNull(visibilityGuardrailService);
        this.federatedShardBlueprintService = Objects.requireNonNull(federatedShardBlueprintService);
        this.publicRecognitionPolicyService = Objects.requireNonNull(publicRecognitionPolicyService);
        this.clock = Objects.requireNonNull(clock);
    }

    public AdminInstitutionalArchitectureResponse blueprint() {
        InstitutionalCatalogCoverageSummary coverageSummary = governanceApplicationService.coverageSummary();
        var governanceSummary = governanceApplicationService.summarize();
        var govBrReadiness = govBrSurfaceFacadeService.readiness();
        var govBrIdentity = govBrSurfaceFacadeService.identityAssurance();
        var federacaoHealth = federalismoJudicialEngine.healthFederacao();
        var defaultVisibility = visibilityGuardrailService.simulate(true, false, false, false, false, false);
        var defaultCluster = federatedShardBlueprintService.resolve("TJCE", "CE");
        return new AdminInstitutionalArchitectureResponse(
                BLUEPRINT_VERSION,
                clock.instant(),
                buildTopology(coverageSummary, governanceSummary),
                buildActivation(),
                buildVisibility(defaultVisibility),
                buildIdentity(govBrReadiness, govBrIdentity),
                buildFederation(defaultCluster, federacaoHealth),
                buildReadiness(govBrReadiness, govBrIdentity)
        );
    }

    public AdminInstitutionalArchitectureResponse.VisibilitySimulation simulateVisibility(boolean sameJurisdictionUnit,
                                                                                          boolean funcionalCompetence,
                                                                                          boolean cooperativeGrantActive,
                                                                                          boolean systemicSupervision,
                                                                                          boolean breakGlassActive,
                                                                                          boolean sigiloProcessual) {
        return visibilityGuardrailService.simulate(
                sameJurisdictionUnit,
                funcionalCompetence,
                cooperativeGrantActive,
                systemicSupervision,
                breakGlassActive,
                sigiloProcessual
        );
    }

    public AdminInstitutionalArchitectureResponse.ClusterResolution resolveShard(String tribunalCodigo, String uf) {
        return federatedShardBlueprintService.resolve(tribunalCodigo, uf);
    }

    public AdminInstitutionalPublicRecognitionResponse assessPublicRecognition(String scope,
                                                                               boolean officialCatalogMatch,
                                                                               boolean publicCnpjActive,
                                                                               boolean publicNatureCompatible,
                                                                               boolean officialEmailChannel,
                                                                               boolean officialDomain,
                                                                               boolean legalActPresent,
                                                                               boolean territorialMatch,
                                                                               boolean representativeGovBrGold,
                                                                               boolean representativeIcpBrasilValid,
                                                                               boolean subordinateUnitWithoutOwnCnpj,
                                                                               boolean parentInstitutionRecognized) {
        return publicRecognitionPolicyService.assess(
                scope,
                officialCatalogMatch,
                publicCnpjActive,
                publicNatureCompatible,
                officialEmailChannel,
                officialDomain,
                legalActPresent,
                territorialMatch,
                representativeGovBrGold,
                representativeIcpBrasilValid,
                subordinateUnitWithoutOwnCnpj,
                parentInstitutionRecognized
        );
    }

    private AdminInstitutionalArchitectureResponse.Topology buildTopology(InstitutionalCatalogCoverageSummary coverageSummary,
                                                                           InstitutionalCatalogGovernanceSummary governanceSummary) {
        var unidadesCatalogadas = catalogoService.listarPorTipo(null);
        int catalogUnits = unidadesCatalogadas.size();
        int catalogKinds = (int) unidadesCatalogadas.stream().map(unit -> unit.destinatarioKind().name()).distinct().count();
        int governanceEntries = Math.toIntExact(governanceSummary.totalGovernancasAtivas());
        int governanceKindsCovered = (int) coverageSummary.itens().stream().filter(item -> item.totalUnidadesAtivas() > 0).count();
        int activeUfCoverage = (int) unidadesCatalogadas.stream()
                .filter(com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional::ativa)
                .map(com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional::uf)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        LinkedHashMap<String, Integer> officialCounts = new LinkedHashMap<>();
        officialCounts.put("tribunaisSuperiores", 5);
        officialCounts.put("tjs", 27);
        officialCounts.put("trfs", 6);
        officialCounts.put("trts", 24);
        officialCounts.put("tres", 27);
        officialCounts.put("tjms", 3);
        officialCounts.put("cjmsPrimeiraInstancia", 12);
        return new AdminInstitutionalArchitectureResponse.Topology(
                List.of(
                        new AdminInstitutionalArchitectureResponse.TopologyLayer(1, "CORTES_SUPERIORES", "Cortes superiores", "STF · STJ · TST · TSE · STM", "TRIBUNAIS_REGIONAIS_OU_ESTADUAIS"),
                        new AdminInstitutionalArchitectureResponse.TopologyLayer(2, "TRIBUNAIS_REGIONAIS_E_ESTADUAIS", "Tribunais regionais e estaduais", "TJ · TRF · TRT · TRE · TJM", "COMARCA_SUBSECAO_ZONA_AUDITORIA"),
                        new AdminInstitutionalArchitectureResponse.TopologyLayer(3, "BASE_TERRITORIAL", "Base territorial", "Comarca · Subseção · Vara do Trabalho · Zona Eleitoral · CJM", "VARAS_JUIZADOS_POSTOS"),
                        new AdminInstitutionalArchitectureResponse.TopologyLayer(4, "UNIDADES_EXECUCAO", "Unidades de execução", "Vara · Juizado · Posto · Fórum", "USUARIOS_E_CAIXAS_TOPOLOGICAS")
                ),
                List.of(
                        new AdminInstitutionalArchitectureResponse.ExternalInstitutionNetwork("MP", "Ministério Público", "estadual_federal_trabalhista_militar_dfdt", "catalogo_e_governanca_ja_presentes"),
                        new AdminInstitutionalArchitectureResponse.ExternalInstitutionNetwork("DEFENSORIA", "Defensoria Pública", "estadual_e_uniao", "catalogo_e_governanca_ja_presentes"),
                        new AdminInstitutionalArchitectureResponse.ExternalInstitutionNetwork("PROCURADORIAS", "AGU · PGE · PGM", "federal_estadual_municipal", "catalogo_e_governanca_ja_presentes"),
                        new AdminInstitutionalArchitectureResponse.ExternalInstitutionNetwork("POLICIAS", "Polícia Civil · Polícia Federal · Polícia Penal", "estadual_federal_penal", "catalogo_e_topologia_ja_presentes"),
                        new AdminInstitutionalArchitectureResponse.ExternalInstitutionNetwork("CONSELHO_TUTELAR", "Conselho Tutelar", "municipal", "catalogo_presente_sem_onboarding_automatico"),
                        new AdminInstitutionalArchitectureResponse.ExternalInstitutionNetwork("EXTRAJUDICIAL", "Cartórios e órgãos técnicos", "estadual_conveniado", "catalogo_ja_presente")
                ),
                Map.copyOf(officialCounts),
                new AdminInstitutionalArchitectureResponse.CoverageSnapshot(
                        catalogUnits,
                        catalogKinds,
                        activeUfCoverage,
                        governanceEntries,
                        governanceKindsCovered,
                        List.of(
                                "catálogo institucional unificado já semeado para múltiplas redes externas",
                                "governança e expansão regional já disponíveis para catálogo institucional",
                                "topologia judicial-administrativa oficial ainda não estava formalizada como blueprint consultável"
                        )
                ),
                List.of(
                        "catálogo institucional unificado preservado",
                        "governança institucional existente reutilizada sem duplicação",
                        "novo blueprint separa topologia judicial da malha de destinatários externos"
                )
        );
    }

    private AdminInstitutionalArchitectureResponse.Activation buildActivation() {
        return new AdminInstitutionalArchitectureResponse.Activation(
                List.of(
                        new AdminInstitutionalArchitectureResponse.ActivationLayer(1, "Âncoras nacionais", "pre_cadastro_automatico", "ADAPTADO_NESTA_RODADA", List.of("pré-mapear tribunais e redes essenciais", "vincular fonte oficial por tipo institucional", "evitar cadastro manual do zero")),
                        new AdminInstitutionalArchitectureResponse.ActivationLayer(2, "Ativação por representante legal", "ativacao_assistida", "PARCIAL", List.of("exigir identidade forte", "amarrar jurisdição superior", "registrar chave institucional")),
                        new AdminInstitutionalArchitectureResponse.ActivationLayer(3, "Expansão interna", "delegada_a_instituicao", "JA_SUPORTADO_POR_TOPOLOGIA_E_PERFIS", List.of("gerir usuários locais", "atribuir cargos e permissões", "provisionar caixas topológicas"))
                ),
                List.of(
                        new AdminInstitutionalArchitectureResponse.ActivationSource("CNPJ", "Receita Federal / CNPJ", "identidade_juridica_da_instituicao", false),
                        new AdminInstitutionalArchitectureResponse.ActivationSource("SIORG", "SIORG", "estrutura_e_contatos_da_administracao_federal", false),
                        new AdminInstitutionalArchitectureResponse.ActivationSource("DATAJUD", "CNJ DataJud", "tribunais_varas_metadados_processuais_e_chaves_publicas_de_integracao", false),
                        new AdminInstitutionalArchitectureResponse.ActivationSource("IBGE", "IBGE Localidades", "municipios_ufs_divisao_territorial", false)
                ),
                List.of(
                        "framework de expansão interna já alinhado ao catálogo e à governança institucional",
                        "malha topológica existente pode receber ativação em vez de cadastro manual isolado",
                        "novo blueprint separa ingestão oficial, ativação e expansão"
                ),
                List.of(
                        "job automático de pré-cadastro a partir de CNPJ/SIORG/DataJud/IBGE",
                        "fluxo de código de ativação institucional e validação do representante legal",
                        "persistência da chave institucional PJB para assinatura de atos da entidade"
                )
        );
    }

    private AdminInstitutionalArchitectureResponse.Visibility buildVisibility(AdminInstitutionalArchitectureResponse.VisibilitySimulation defaultVisibility) {
        return new AdminInstitutionalArchitectureResponse.Visibility(
                visibilityGuardrailService.tiers(),
                defaultVisibility,
                List.of(
                        "break-glass e visibilidade pessoal controlada já existiam e foram preservados",
                        "nova política explícita separa jurisdição local, competência funcional, cooperação e supervisão",
                        "simulação dedicada evita espalhar regra de visibilidade por vários módulos"
                ),
                List.of(
                        "vínculo cooperativo temporal persistido por processo e unidade",
                        "policy engine único consumindo cooperação institucional e escopos de segredo",
                        "workflow explícito de carta precatória eletrônica com grant e expiração"
                )
        );
    }

    private AdminInstitutionalArchitectureResponse.Identity buildIdentity(GovBrAccountEntryGovernanceResponse readiness,
                                                                          GovBrIdentityAssuranceResponse identity) {
        return new AdminInstitutionalArchitectureResponse.Identity(
                List.of(
                        new AdminInstitutionalArchitectureResponse.IdentityRail(
                                "TRILHO_A",
                                "magistrados_membros_e_assinantes_de_ato",
                                "maximo",
                                List.of("gov_br_ouro", "certificado_icp_brasil_pf", "sessao_curta", "trilha_hash_e_timestamp"),
                                List.of("assinar_despacho_sentenca_portaria_oficio", "expedir_ato_com_efeito_juridico"),
                                List.of("step_up_constante", "auditoria_criptografica_obrigatoria")
                        ),
                        new AdminInstitutionalArchitectureResponse.IdentityRail(
                                "TRILHO_B",
                                "servidores_operacionais_e_equipes_de_apoio",
                                "alto_operacional",
                                List.of("login_institucional_federado", "gov_br_prata_ou_superior", "contexto_da_instituicao"),
                                List.of("consultar", "movimentar_internamente", "notificar", "protocolar_internamente"),
                                List.of("sem_assinatura_externa", "sem_ato_com_efeito_juridico_externo")
                        ),
                        new AdminInstitutionalArchitectureResponse.IdentityRail(
                                "TRILHO_C",
                                "cidadao_advogado_parte_interessado",
                                "externo_gradual",
                                List.of("gov_br", "oab_validada_quando_cabivel", "contexto_de_representacao"),
                                List.of("peticionar", "consultar", "acompanhar_fluxos_externos"),
                                List.of("sem_malha_interna", "acesso_need_to_know")
                        )
                ),
                readiness.enabled(),
                identity.enabled(),
                identity.tokenVerificationReady(),
                identity.strongBindingReady(),
                List.of("OIDC_GOVBR", "OIDC_FEDERADO_INSTITUCIONAL", "SAML2_ONDE_HOUVER_LEGADO"),
                List.of(
                        "Gov.br step-up e verificação de identidade já existem na base",
                        "trilha de dispositivo confiável e strong binding já aparecem na camada Gov.br",
                        "blueprint novo formaliza trilhos A/B/C sem espalhar regra"),
                List.of(
                        "federação SAML2/OIDC explícita para legados institucionais",
                        "certificado institucional da entidade no PJB",
                        "matriz normativa separando assinatura individual ICP-Brasil e assinatura institucional"
                )
        );
    }

    private AdminInstitutionalArchitectureResponse.Federation buildFederation(AdminInstitutionalArchitectureResponse.ClusterResolution defaultCluster,
                                                                              FederalismoJudicialEngine.FederacaoHealth federacaoHealth) {
        LinkedHashMap<String, Object> health = new LinkedHashMap<>();
        health.put("totalNos", federacaoHealth.totalNos());
        health.put("nosOnline", federacaoHealth.nosOnline());
        health.put("nosOffline", federacaoHealth.nosOffline());
        health.put("nosSincronizando", federacaoHealth.nosSincronizando());
        health.put("nosDegradados", federacaoHealth.nosDegradados());
        health.put("backlogTotal", federacaoHealth.backlogTotal());
        health.put("percentualOnline", federacaoHealth.percentualOnline());
        health.put("status", federacaoHealth.status());
        health.put("verificadoEm", federacaoHealth.verificadoEm());
        return new AdminInstitutionalArchitectureResponse.Federation(
                federatedShardBlueprintService.clusters(),
                "PostgreSQL global com localizador de processo, topologia institucional, grants cooperativos, federação de identidade e trilha auditável",
                "FEDERAL_SUPERIOR",
                defaultCluster,
                List.of(
                        "federalismo judicial e health federado já existem",
                        "novo resolvedor de cluster torna explícita a malha de shards por cluster jurisdicional",
                        "metadata store global fica formalizado como fonte soberana de roteamento"
                ),
                List.of(
                        "router efetivo processo→shard persistido por metadata store",
                        "serviço explícito de localização transparente entre cluster regional e federal",
                        "replicação institucional topológica com grants cooperativos no mesmo índice"
                ),
                Map.copyOf(health)
        );
    }

    private AdminInstitutionalArchitectureResponse.Readiness buildReadiness(GovBrAccountEntryGovernanceResponse readiness,
                                                                            GovBrIdentityAssuranceResponse identity) {
        ArrayList<String> implemented = new ArrayList<>(List.of(
                "catálogo institucional unificado",
                "governança institucional com expansão nacional/regional",
                "malha topológica de inboxKey e drawerKey",
                "break-glass institucional",
                "Gov.br step-up e identity assurance",
                "federalismo judicial com health e ledger"
        ));
        if (readiness.enabled()) {
            implemented.add("entrada Gov.br habilitada");
        }
        if (identity.strongBindingReady()) {
            implemented.add("strong binding de identidade habilitado");
        }
        return new AdminInstitutionalArchitectureResponse.Readiness(
                List.copyOf(implemented),
                List.of(
                        "blueprint formal da topologia jurídico-administrativa nacional",
                        "política explícita de visibilidade em quatro níveis com simulação dedicada",
                        "resolvedor de cluster jurisdicional para shard federado",
                        "matriz explícita de trilhos de identidade A/B/C",
                        "relatório único de prontidão institucional para onboarding federado"
                ),
                List.of(
                        "pré-cadastro automático por bases oficiais",
                        "ativação por representante legal com código institucional",
                        "federação institucional SAML/OIDC materializada para legados",
                        "certificado institucional PJB da entidade",
                        "grant cooperativo persistido e consumido por policy engine único"
                ),
                List.of(
                        "ligar jobs de ingestão oficial sem quebrar catálogo existente",
                        "persistir grants cooperativos por processo e unidade",
                        "introduzir federação institucional em camada única de identidade",
                        "conectar resolvedor de cluster ao roteador soberano de data plane"
                )
        );
    }
}
