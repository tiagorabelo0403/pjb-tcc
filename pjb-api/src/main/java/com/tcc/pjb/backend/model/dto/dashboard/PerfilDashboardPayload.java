package com.tcc.pjb.backend.model.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoGovernanceMetricas;

public sealed interface PerfilDashboardPayload permits
        PerfilDashboardPayload.AssessorPayload,
        PerfilDashboardPayload.DelegadoPayload,
        PerfilDashboardPayload.OficialJusticaPayload,
        PerfilDashboardPayload.PeritoPayload,
        PerfilDashboardPayload.MinisterioPublicoPayload,
        PerfilDashboardPayload.DefensorPublicoPayload,
        PerfilDashboardPayload.ConciliadorMediadorPayload,
        PerfilDashboardPayload.CartorioExtrajudicialPayload,
        PerfilDashboardPayload.LeiloeiroJudicialPayload,
        PerfilDashboardPayload.PsicossocialJudicialPayload,
        PerfilDashboardPayload.CuradorAusentesPayload,
        PerfilDashboardPayload.AuxiliarJusticaPayload {

    String etag();
    LocalDateTime generatedAt();
    String perfilAtivo();
    String tratamento();
    List<String> pendencias();
    List<PrazoRadarItem> prazoRadar();
    SessionRiskResumo sessionRisk();
    SigiloAtivoResumo sigiloAtivo();
    PlantaoResumo plantao();
    OnboardingResumo onboarding();
    List<ExternalSystemStatus> externalSystems();
    BehavioralAuditResumo behavioralAudit();

    record PrazoRadarItem(Long workItemId, String titulo, Instant dueAt, String categoria, long diasRestantes) {
    }

    record SessionRiskResumo(String level, int score, String network, boolean suspectNetwork, String reason) {
    }

    record SigiloAtivoResumo(boolean possuiAcessoAtivo, int quantidade, LocalDateTime expiraEm, long minutosRestantes) {
    }

    record PlantaoResumo(String status, LocalDateTime inicio, LocalDateTime fim, String referencia) {
    }

    record OnboardingResumo(boolean concluido, List<String> pendenciasBloqueantes, List<String> pendenciasInformativas) {
    }

    record ExternalSystemStatus(String system, boolean enabled, boolean permitted, boolean realtime, String mode, int itemCount, List<String> highlights) {
    }

    record BehavioralAuditResumo(String baselineKey, int observedVolume, int expectedMax, String level, boolean anomalous, String rationale) {
    }

    record LocalizadorGovernadoResumo(
            boolean habilitado,
            PessoaLocalizacaoGovernanceMetricas metricas
    ) {
    }

    record AssessorPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String magistradoVinculado,
            String gabineteId,
            List<String> minutasParaRevisar,
            List<String> decisionsDraft,
            List<String> audienciasHoje,
            int processosPendentesDespacho,
            int processosPendentesAssinatura,
            boolean delegacaoAtiva,
            String pastaTrabalho
    ) implements PerfilDashboardPayload {
    }

    record DelegadoPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String delegaciaId,
            String delegaciaNome,
            String esfera,
            int inqueritosPendentes,
            int termosCircularPendentes,
            int mandadosCumprirPendentes,
            List<String> boletinsOcorrenciaRecentes,
            List<String> alertasCrime,
            boolean integraBo,
            boolean localizadorPessoasHabilitado,
            List<String> capacidadesOperacionais,
            LocalizadorGovernadoResumo localizadorGovernado,
            Map<String, Object> institutionalBranding,
            Map<String, Object> panelVisualIdentity,
            Map<String, Object> policeSystemLandscape,
            Map<String, Object> investigativeWorkstation,
            Map<String, Object> operationalSignals,
            Map<String, Object> nativeComposition,
            Map<String, Object> collectionComposition,
            Map<String, Object> actionSurface,
            Map<String, Object> executionSurface,
            Map<String, Object> sharedExperience
    ) implements PerfilDashboardPayload {
    }

    record OficialJusticaPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String circunscricaoId,
            int mandadosPendentes,
            int mandadosCumpridos,
            int mandadosFrustrados,
            List<MandadoResumo> proximosMandados,
            boolean avaliador,
            List<String> penhorasAgendadas,
            boolean localizadorPessoasHabilitado,
            List<String> capacidadesOperacionais,
            LocalizadorGovernadoResumo localizadorGovernado,
            Map<String, Object> institutionalBranding,
            Map<String, Object> panelVisualIdentity,
            Map<String, Object> organizacaoOperacional,
            Map<String, Object> pendenciasOperacionais,
            Map<String, Object> portfolioProcessualNomeado,
            Map<String, Object> rastreioOperacional,
            Map<String, Object> operationalWorkbench,
            Map<String, Object> agendaOperacional,
            Map<String, Object> calendarioOperacional,
            Map<String, Object> balcaoVirtual,
            Map<String, Object> notificationCenter,
            CalendarInstitutionalFocusResponse institutionalCalendarFocus,
            CalendarInstitutionalBridgeResponse institutionalCalendarBridge,
            Map<String, Object> operationalSignals,
            Map<String, Object> nativeComposition,
            Map<String, Object> collectionComposition,
            Map<String, Object> actionSurface,
            Map<String, Object> executionSurface,
            Map<String, Object> sharedExperience
    ) implements PerfilDashboardPayload {
        public record MandadoResumo(String mandadoId, String tipo, String enderecoDestinatario, String prazo, String processoNumero) {
        }
    }

    record PeritoPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String subtipoPericia,
            int laudosPendentes,
            int laudosAtrasados,
            int nomeacoesAguardandoAceite,
            List<NomeacaoResumo> nomeacoesAtivas,
            List<String> prazosVencendo,
            boolean possuiRegistroProfissional,
            String conselhoProfissional,
            String registroConselho,
            Map<String, Object> institutionalBranding,
            Map<String, Object> panelVisualIdentity,
            CalendarInstitutionalFocusResponse institutionalCalendarFocus,
            CalendarInstitutionalBridgeResponse institutionalCalendarBridge
    ) implements PerfilDashboardPayload {
        public record NomeacaoResumo(Long nomeacaoId, String processoNumero, String assunto,
                @Schema(description = "Prazo para entrega do laudo pericial", format = "date",
                        example = "2026-08-31") String prazoLaudo, String status) {
        }
    }

    record MinisterioPublicoPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String promotoriaId,
            String ramo,
            int manifestacoesPendentes,
            int recursosParaInterpor,
            int prazosDentroDe48h,
            List<String> processosPrioridadeAlta,
            List<String> inqueritosEmAcompanhamento,
            boolean podeRequisitarDiligencia,
            Map<String, Object> institutionalBranding,
            Map<String, Object> panelVisualIdentity,
            Map<String, Object> operationalSignals,
            Map<String, Object> nativeComposition,
            Map<String, Object> collectionComposition,
            Map<String, Object> actionSurface,
            Map<String, Object> executionSurface,
            Map<String, Object> sharedExperience
    ) implements PerfilDashboardPayload {
    }

    record DefensorPublicoPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String defensoriaNucleo,
            boolean federal,
            int assistidosAtivos,
            int audienciasHoje,
            int peticoesIniciaisPendentes,
            int recursosParaInterpor,
            int prazosDentroDe48h,
            List<String> processosPrioridadeAlta,
            boolean possuiGratuidade,
            Map<String, Object> institutionalBranding,
            Map<String, Object> panelVisualIdentity,
            Map<String, Object> operationalSignals,
            Map<String, Object> nativeComposition,
            Map<String, Object> collectionComposition,
            Map<String, Object> actionSurface,
            Map<String, Object> executionSurface,
            Map<String, Object> sharedExperience
    ) implements PerfilDashboardPayload {
    }

    record ConciliadorMediadorPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String cejuscId,
            String cejuscNome,
            String funcao,
            int sessoesPendentes,
            int sessoesHoje,
            int acordosHomologadosNoMes,
            int acordosPendentesHomologacao,
            List<SessaoResumo> proximasSessoes,
            double taxaAcordoPercentual
    ) implements PerfilDashboardPayload {
        public record SessaoResumo(String sessaoId, String tipo, String horario, String sala, String processoNumero, List<String> participantes) {
        }
    }

    record CartorioExtrajudicialPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String serventia,
            String subperfil,
            int certidoesPendentes,
            int indisponibilidadesPendentes,
            int averbacoesPendentes,
            List<String> atosPrioritarios
    ) implements PerfilDashboardPayload {
    }

    record LeiloeiroJudicialPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            int leiloesPendentes,
            int editaisPendentes,
            int prestacoesContasPendentes,
            List<String> hastasProximas
    ) implements PerfilDashboardPayload {
    }

    record PsicossocialJudicialPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String subperfil,
            int estudosPendentes,
            int visitasAgendadas,
            int pareceresUrgentes,
            List<String> casosPrioritarios,
            Map<String, Object> institutionalBranding,
            Map<String, Object> panelVisualIdentity
    ) implements PerfilDashboardPayload {
    }

    record CuradorAusentesPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            int bensSobGuarda,
            int prestacoesContasPendentes,
            int medidasPatrimoniaisUrgentes,
            List<String> expedientesPrioritarios
    ) implements PerfilDashboardPayload {
    }

    record AuxiliarJusticaPayload(
            String etag,
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            List<String> pendencias,
            List<PrazoRadarItem> prazoRadar,
            SessionRiskResumo sessionRisk,
            SigiloAtivoResumo sigiloAtivo,
            PlantaoResumo plantao,
            OnboardingResumo onboarding,
            List<ExternalSystemStatus> externalSystems,
            BehavioralAuditResumo behavioralAudit,
            String subtipo,
            int tarefasPendentes,
            List<String> processosVinculados,
            List<String> prazosCriticos
    ) implements PerfilDashboardPayload {
    }
}
