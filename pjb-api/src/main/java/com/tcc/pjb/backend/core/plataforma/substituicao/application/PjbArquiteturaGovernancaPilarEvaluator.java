package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.available;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.capacidade;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.pilar;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.rito.RitoResolutionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Avalia o pilar "governança nacional" (Fatia F6 -- extraído de
 * PjbArquiteturaSubstituicaoNacionalApplicationService).
 */
@Component
public class PjbArquiteturaGovernancaPilarEvaluator {

    private final AdministradorNacionalGovernanceService administradorNacionalGovernanceService;
    private final ObjectProvider<CompetenceResolverService> competenceResolverServiceProvider;
    private final ObjectProvider<RitoResolutionService> ritoResolutionServiceProvider;
    private final ObjectProvider<PerfilCapabilityMatrixService> perfilCapabilityMatrixServiceProvider;
    private final ObjectProvider<InstitutionalSensitiveActAuthorizationApplicationService> institutionalSensitiveActAuthorizationApplicationServiceProvider;
    private final ObjectProvider<CapabilityRateLimiter> capabilityRateLimiterProvider;

    public PjbArquiteturaGovernancaPilarEvaluator(
            AdministradorNacionalGovernanceService administradorNacionalGovernanceService,
            ObjectProvider<CompetenceResolverService> competenceResolverServiceProvider,
            ObjectProvider<RitoResolutionService> ritoResolutionServiceProvider,
            ObjectProvider<PerfilCapabilityMatrixService> perfilCapabilityMatrixServiceProvider,
            ObjectProvider<InstitutionalSensitiveActAuthorizationApplicationService> institutionalSensitiveActAuthorizationApplicationServiceProvider,
            ObjectProvider<CapabilityRateLimiter> capabilityRateLimiterProvider) {
        this.administradorNacionalGovernanceService = Objects.requireNonNull(administradorNacionalGovernanceService);
        this.competenceResolverServiceProvider = Objects.requireNonNull(competenceResolverServiceProvider);
        this.ritoResolutionServiceProvider = Objects.requireNonNull(ritoResolutionServiceProvider);
        this.perfilCapabilityMatrixServiceProvider = Objects.requireNonNull(perfilCapabilityMatrixServiceProvider);
        this.institutionalSensitiveActAuthorizationApplicationServiceProvider = Objects.requireNonNull(institutionalSensitiveActAuthorizationApplicationServiceProvider);
        this.capabilityRateLimiterProvider = Objects.requireNonNull(capabilityRateLimiterProvider);
    }

    public PjbArquiteturaSubstituicaoPilar avaliar(boolean buildGateAprovado) {
        ArrayList<PjbArquiteturaSubstituicaoCapacidade> capacidades = new ArrayList<>();
        capacidades.add(capacidade(
                "gov.tribunais-e-ramos",
                "Matriz nacional de tribunal, ramo e conectores",
                NationalCompetenceMatrix.values().length >= 60,
                94,
                List.of("NationalCompetenceMatrix=" + NationalCompetenceMatrix.values().length, "RamoDireito=" + RamoDireito.values().length, "RitoProcessual=" + RitoProcessual.values().length),
                List.of("Seguir refinando diferenças locais por unidade e colegiado sem quebrar o núcleo")
        ));
        capacidades.add(capacidade(
                "gov.competencia-e-rito",
                "Resolução de competência e rito sem duplicar núcleo",
                available(competenceResolverServiceProvider) && available(ritoResolutionServiceProvider),
                91,
                List.of("CompetenceResolverService", "RitoResolutionService"),
                List.of("Endurecer override local parametrizado e rastreável por tribunal")
        ));
        capacidades.add(capacidade(
                "gov.perfis-e-capabilidades",
                "Perfis, capabilidades e autorização sensível",
                available(perfilCapabilityMatrixServiceProvider) && available(institutionalSensitiveActAuthorizationApplicationServiceProvider) && available(capabilityRateLimiterProvider),
                88,
                List.of("PerfilCapabilityMatrixService", "InstitutionalSensitiveActAuthorizationApplicationService", "CapabilityRateLimiter"),
                List.of("Consolidar matriz nacional de perfis especiais e atos sensíveis por microssistema")
        ));
        capacidades.add(capacidade(
                "gov.governanca-operacional",
                "Governança operacional nacional e health checks",
                available(administradorNacionalGovernanceService),
                87,
                List.of("AdministradorNacionalGovernanceService", "metricas por UF/comarca e reconciliação global"),
                List.of("Fechar rollout governado com pilotos, ondas de adesão e comitê executivo nacional")
        ));
        capacidades.add(capacidade(
                "gov.build-gates",
                "Governança estrutural de build e disciplina de superfície",
                buildGateAprovado,
                buildGateAprovado ? 90 : 70,
                List.of("BuildGateGovernanceService", "Disciplina estrutural já incorporada ao herdeiro"),
                List.of("Continuar zerando regressão estrutural e mantendo gate de rollout nacional")
        ));
        return pilar(
                "governanca-nacional",
                "Governança nacional",
                capacidades,
                List.of(
                        "Fechar política nacional de implantação por onda, tribunal e microssistema.",
                        "Preservar parametrização local com trilha auditável e núcleo canônico único."
                )
        );
    }
}
