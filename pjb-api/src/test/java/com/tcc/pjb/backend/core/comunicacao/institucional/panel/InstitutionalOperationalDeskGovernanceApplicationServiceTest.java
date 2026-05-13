package com.tcc.pjb.backend.core.comunicacao.institucional.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalOperationalDeskGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalOperationalDeskGovernanceApplicationServiceTest {

    @Test
    void mustReturnMissingProfileGovernanceWhenProfileIsNull() {
        InstitutionalOperationalDeskGovernance governance = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(null, null, null);

        assertNotNull(governance);
        assertFalse(governance.sectionVisible());
        assertTrue(governance.forbiddenActs().contains(com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalOperationalDeskGovernanceMessages.CROSS_VARA_BLOCK));
        assertTrue(governance.findings().contains(com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalOperationalDeskGovernanceMessages.MISSING_PROFILE));
    }

    @Test
    void mustBindOperationalDeskToSpecificVaraAndSecretariatTracks() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                new com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService(),
                mock(ProcessoRepository.class)
        );
        InstitutionalAccessProfileCatalogEntry entry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("CENTRAL_AUDIENCIAS__CENTRAL_AUDIENCIA_SECRETARIA"))
                .findFirst()
                .orElseThrow();
        InstitutionalProcessWorkspace workspace = workspaceService.detalharPerfil(entry.codigo(), null, null, null, null, null);
        InstitutionalOperationalDeskGovernance governance = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(profile(entry, "1ª Vara Cível de Fortaleza", "CENTRAL_AUDIENCIAS"), entry, workspace);

        assertNotNull(governance);
        assertTrue(governance.sectionVisible());
        assertTrue(governance.unitScopeBound());
        assertTrue(governance.segregatedByUnit());
        assertTrue(governance.segregatedByVaraOrSpecialization());
        assertTrue(governance.secretariatWorkflowEnabled());
        assertTrue(governance.communicationWorkflowEnabled());
        assertTrue(governance.distributionWorkflowEnabled());
        assertTrue(governance.expeditionWorkflowEnabled());
        assertTrue(governance.queueManagementWorkflowEnabled());
        assertTrue(governance.unitGroupingKey().contains("VARA_1"));
        assertTrue(governance.assignmentBoundaries().stream().anyMatch(item -> item.contains("vara_cluster=VARA_1")));
        assertTrue(governance.deskQueues().stream().anyMatch(item -> item.contains("fila_secretaria") || item.contains("fila_expedientes")));
        assertTrue(governance.deskQueues().stream().anyMatch(item -> item.contains("fila_publicacoes_prazos") || item.contains("fila_subfluxos_documentos")));
        assertTrue(governance.unitTopology().stream().anyMatch(item -> item.contains("vara_cluster=VARA_1")));
        assertTrue(governance.secretariatActs().contains("organizar_processos_por_unidade_e_vara"));
        assertTrue(governance.secretariatActs().contains("limpar_filas_e_subfluxos_sem_perder_trilha_do_ato"));
        assertTrue(governance.distributionActs().contains("registrar_prevencao_dependencia_redistribuicao_formal"));
        assertTrue(governance.expeditionActs().contains("expedir_mandado_oficio_carta_edital_ou_alvara_no_fluxo_competente"));
        assertTrue(governance.specializedFlows().contains("civel_contestacao_saneamento_instrucao_julgamento_e_cumprimento"));
        assertTrue(governance.operationalDomains().contains("PUBLICACOES_INTIMACOES_E_PRAZOS"));
        assertTrue(governance.counterpartScopes().contains("DIARIO_ELETRONICO_E_CANAIS_DE_INTIMACAO"));
    }

    @Test
    void mustKeepAssessoriaInPreparatoryFlowWithoutReplacingTitular() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                new com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService(),
                mock(ProcessoRepository.class)
        );
        InstitutionalAccessProfileCatalogEntry entry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("PROMOTORIA__PROMOTORIA_DOCUMENTOS"))
                .findFirst()
                .orElseThrow();
        InstitutionalProcessWorkspace workspace = workspaceService.detalharPerfil(entry.codigo(), null, null, null, null, null);
        InstitutionalOperationalDeskGovernance governance = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(profile(entry, "2ª Vara Criminal de Fortaleza", "PROMOTORIA"), entry, workspace);

        assertNotNull(governance);
        assertTrue(governance.sectionVisible());
        assertTrue(governance.assessorWorkflowEnabled());
        assertTrue(governance.opinionWorkflowEnabled());
        assertTrue(governance.conclusionWorkflowEnabled());
        assertFalse(governance.secretariatActs().contains("expedir_mandado_ou_certidao_para_fluxo_de_cumprimento"));
        assertTrue(governance.assessorActs().contains("preparar_minuta_sem_substituir_assinatura_final"));
        assertTrue(governance.assessorActs().contains("preparar_relatorio_de_precedentes_memoria_processual_e_risco"));
        assertTrue(governance.conclusionActs().contains("preparar_conclusao_voto_minuta_ou_sentenca_em_trilha_reservada"));
        assertTrue(governance.specializedFlows().contains("penal_inquerito_acao_penal_audiencias_custodia_ou_juri"));
        assertTrue(governance.forbiddenActs().contains("assessoria_nao_assina_ato_final_sem_fluxo_do_titular"));
        assertTrue(governance.unitGroupingKey().contains("VARA_2"));
    }

    private InstitutionalOperationalProfileProjection profile(InstitutionalAccessProfileCatalogEntry entry,
                                                             String unidadeNome,
                                                             String scope) {
        return new InstitutionalOperationalProfileProjection(
                entry.codigo() + "|NOM-1",
                "ATIVO_NO_PJB",
                true,
                "AFF-1",
                "NOM-1",
                200L,
                entry.nomeExibicao(),
                "SERVIDOR",
                scope,
                scope,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "UNI-1",
                unidadeNome,
                "CX-1",
                "SECRETARIA",
                entry.nominationRole().name(),
                "GESTOR_CAIXA",
                entry.processProfile().name(),
                "PAINEL_TITULAR",
                InstitutionalApiRoutes.painelExecutivo(),
                "#2563eb",
                "AREA-1",
                entry.trustFloor().name(),
                true,
                true,
                true,
                false,
                false,
                true,
                "LOCAL",
                "TJCE",
                "UNI-1",
                unidadeNome,
                "Fortaleza",
                "CE|TJCE|UNI-1|CX-1|0",
                "WRITE-1",
                "READ-1",
                entry.capacidadesPadrao().stream().map(Enum::name).toList(),
                List.of("PJB"),
                List.of("PJB"),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
    }

    @Test
    void mustSegmentProtocolCabinetUpjAndSecondDegreeWithoutParallelFlows() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                new com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService(),
                mock(ProcessoRepository.class)
        );
        InstitutionalAccessProfileCatalogEntry forumEntry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("FORUM__FORUM_SECRETARIA"))
                .findFirst()
                .orElseThrow();
        InstitutionalAccessProfileCatalogEntry assessorEntry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("PROMOTORIA__PROMOTORIA_DOCUMENTOS"))
                .findFirst()
                .orElseThrow();

        InstitutionalProcessWorkspace protocolWorkspace = workspaceService.detalharPerfil(forumEntry.codigo(), null, null, null, null, null);
        InstitutionalOperationalDeskGovernance protocol = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(profile(forumEntry, "Distribuição e Protocolo do Fórum de Fortaleza", "FORUM"), forumEntry, protocolWorkspace);
        InstitutionalOperationalDeskGovernance gabinete = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(profile(assessorEntry, "Gabinete da 3ª Vara Cível de Fortaleza", "PROMOTORIA"), assessorEntry, workspaceService.detalharPerfil(assessorEntry.codigo(), null, null, null, null, null));
        InstitutionalOperationalDeskGovernance upj = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(profile(forumEntry, "UPJ das Varas Cíveis de Fortaleza", "FORUM"), forumEntry, protocolWorkspace);
        InstitutionalOperationalDeskGovernance segundoGrau = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(profile(forumEntry, "Secretaria da 2ª Câmara Cível do TJCE", "FORUM"), forumEntry, protocolWorkspace);

        assertTrue(protocol.unitKind().equals("PROTOCOLO_DISTRIBUICAO"));
        assertTrue(protocol.operationalDomains().contains("PORTA_DE_ENTRADA_E_PREVENCAO"));
        assertTrue(protocol.deskQueues().stream().anyMatch(item -> item.contains("fila_protocolo_autuacao")));
        assertTrue(protocol.distributionActs().contains("registrar_prevencao_dependencia_redistribuicao_formal"));
        assertTrue(protocol.specializedFlows().contains("protocolo_autuacao_prevencao_dependencia_e_redistribuicao"));
        assertTrue(protocol.forbiddenActs().contains("protocolo_distribuicao_nao_encaminha_para_vara_estranha_sem_registro_formal"));
        assertTrue(gabinete.unitKind().equals("GABINETE"));
        assertTrue(gabinete.operationalDomains().contains("GABINETE_JUDICIAL_E_MINUTAS"));
        assertTrue(gabinete.assignmentBoundaries().contains("gabinete_nao_se_confunde_com_secretaria_ou_upj_compartilhada"));
        assertTrue(gabinete.conclusionActs().contains("priorizar_conclusoes_urgentes_modelos_e_pendencias_de_gabinete"));
        assertTrue(upj.unitKind().equals("UPJ"));
        assertTrue(upj.assignmentBoundaries().contains("upj_compartilha_serventia_sem_fundir_vinculo_processual_da_vara_origem"));
        assertTrue(upj.secretariatActs().contains("segmentar_fluxo_upj_por_vara_origem_sem_mistura_de_competencia"));
        assertTrue(upj.specializedFlows().contains("upj_servico_compartilhado_com_vara_origem_preservada"));
        assertTrue(segundoGrau.unitKind().equals("SECRETARIA_SEGUNDO_GRAU"));
        assertTrue(segundoGrau.operationalDomains().contains("SESSAO_COLEGIADA_E_PREVENCAO_RECURSAL"));
        assertTrue(segundoGrau.deskQueues().stream().anyMatch(item -> item.contains("fila_pauta_colegiada")));
        assertTrue(segundoGrau.specializedFlows().contains("segundo_grau_prevencao_pauta_sessao_acordao_e_publicacao"));
    }

    @Test
    void mustPreserveMandateCenterBoundariesAndCounterpartsAfterAssemblerSplit() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                new com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService(),
                mock(ProcessoRepository.class)
        );
        InstitutionalAccessProfileCatalogEntry forumEntry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("FORUM__FORUM_SECRETARIA"))
                .findFirst()
                .orElseThrow();

        InstitutionalOperationalDeskGovernance governance = new InstitutionalOperationalDeskGovernanceApplicationService()
                .avaliar(profile(forumEntry, "Central de Mandados de Fortaleza", "FORUM"), forumEntry, workspaceService.detalharPerfil(forumEntry.codigo(), null, null, null, null, null));

        assertTrue(governance.unitKind().equals("CENTRAL_MANDADOS"));
        assertTrue(governance.counterpartScopes().contains("CENTRAL_DE_MANDADOS_E_OFICIAIS"));
        assertTrue(governance.assignmentBoundaries().contains("central_mandados_separa_grupo_roteiro_resultado_e_devolucao_por_oficial"));
        assertTrue(governance.expeditionActs().contains("roteirizar_cumprimento_distribuir_oficial_e_registrar_resultado_de_mandado"));
        assertTrue(governance.specializedFlows().contains("mandados_diligencias_certidoes_e_devolucoes_por_oficial"));
        assertTrue(governance.forbiddenActs().contains("central_mandados_nao_redefine_competencia_ou_pauta_do_orgao"));
    }

}
