package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustMatrixEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalTrustMatrixApplicationService {

    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;

    public InstitutionalTrustMatrixApplicationService(InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService) {
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
    }

    public List<InstitutionalTrustMatrixEntry> listar(String scopeCode) {
        List<InstitutionalOrganizationBlueprint> blueprints = scopeCode == null || scopeCode.isBlank()
                ? blueprintCatalogApplicationService.listar()
                : blueprintCatalogApplicationService.findByScope(InstitutionalOrganizationScope.fromTexto(scopeCode)).stream().toList();
        ArrayList<InstitutionalTrustMatrixEntry> entries = new ArrayList<>();
        entries.add(directEntry("CIDADAO_DIRETO", "PERFIL_DIRETO", "Cidadão e parte", InstitutionalProcessProfile.PERFIL_HIBRIDO,
                InstitutionalEntryLandingPanel.PAINEL_UNIDADE, InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA,
                List.of("LOGIN_GOVBR_OU_IDENTIDADE_PESSOAL"),
                List.of("DISPOSITIVO_HOMOLOGADO"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Sem representação institucional automática.", "Sem caixa institucional."),
                List.of("Painel pessoal e comunicações próprias.")));
        entries.add(directEntry("ADVOGADO_DIRETO", "PERFIL_DIRETO", "Advogado", InstitutionalProcessProfile.PERFIL_HIBRIDO,
                InstitutionalEntryLandingPanel.PAINEL_UNIDADE, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                List.of("LOGIN_GOVBR_OU_OAB_VALIDADA"),
                List.of("CERTIFICADO_ICP_BRASIL", "MFA_ATIVO"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Sem contexto de órgão público por padrão."),
                List.of("Peticionamento e ciência no fluxo próprio.")));
        entries.add(directEntry("OAB_SECCIONAL_DIRETO", "PERFIL_DIRETO", "Presidência seccional da OAB", InstitutionalProcessProfile.PERFIL_HIBRIDO,
                InstitutionalEntryLandingPanel.PAINEL_ADMINISTRATIVO, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                List.of("LOGIN_GOVBR", "OAB_VALIDADA", "MFA_ATIVO", "CERTIFICADO_ICP_BRASIL"),
                List.of("DISPOSITIVO_HOMOLOGADO", "AUTORIZACAO_REMOTA_CERTIFICADO"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Atuação institucional restrita à governança seccional da OAB."),
                List.of("Entrada direta da presidência seccional com trilha reforçada e sem substituição de órgão público.")));
        entries.add(directEntry("MAGISTRADO_DIRETO", "PERFIL_DIRETO", "Magistrado", InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                List.of("LOGIN_GOVBR", "MFA_ATIVO", "CERTIFICADO_ICP_BRASIL"),
                List.of("DISPOSITIVO_HOMOLOGADO", "REDE_INSTITUCIONAL_CONFIAVEL"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Decisão jurisdicional permanece no fluxo próprio.", "Não substitui a adesão institucional do fórum."),
                List.of("Gabinete pessoal, assinatura e jurisdição em trilha própria.")));
        entries.add(directEntry("JUIZ_ESTADUAL_DIRETO", "PERFIL_DIRETO", "Juiz estadual", InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                List.of("LOGIN_GOVBR", "MFA_ATIVO", "CERTIFICADO_ICP_BRASIL"),
                List.of("DISPOSITIVO_HOMOLOGADO", "REDE_INSTITUCIONAL_CONFIAVEL"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Segregação pela justiça estadual e pela unidade judicante competente."),
                List.of("Gabinete estadual direto com trilha de assinatura e segregação institucional.")));
        entries.add(directEntry("JUIZ_FEDERAL_DIRETO", "PERFIL_DIRETO", "Juiz federal", InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                List.of("LOGIN_GOVBR", "MFA_ATIVO", "CERTIFICADO_ICP_BRASIL"),
                List.of("DISPOSITIVO_HOMOLOGADO", "REDE_INSTITUCIONAL_CONFIAVEL"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Segregação pela justiça federal, seção e subseção competentes."),
                List.of("Gabinete federal direto com trilha de assinatura e segregação institucional.")));
        entries.add(directEntry("DESEMBARGADOR_ESTADUAL_DIRETO", "PERFIL_DIRETO", "Desembargador estadual", InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                List.of("LOGIN_GOVBR", "MFA_ATIVO", "CERTIFICADO_ICP_BRASIL"),
                List.of("DISPOSITIVO_HOMOLOGADO", "REDE_INSTITUCIONAL_CONFIAVEL"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Segregação por TJ, órgão fracionário e colegiado competente."),
                List.of("Gabinete e colegiado estadual em trilha direta.")));
        entries.add(directEntry("DESEMBARGADOR_FEDERAL_DIRETO", "PERFIL_DIRETO", "Desembargador federal", InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                List.of("LOGIN_GOVBR", "MFA_ATIVO", "CERTIFICADO_ICP_BRASIL"),
                List.of("DISPOSITIVO_HOMOLOGADO", "REDE_INSTITUCIONAL_CONFIAVEL"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Segregação por TRF, órgão fracionário e colegiado competente."),
                List.of("Gabinete e colegiado federal em trilha direta.")));
        entries.add(directEntry("MINISTRO_DIRETO", "PERFIL_DIRETO", "Ministro", InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                List.of("LOGIN_GOVBR", "MFA_ATIVO", "CERTIFICADO_ICP_BRASIL"),
                List.of("DISPOSITIVO_HOMOLOGADO", "REDE_INSTITUCIONAL_CONFIAVEL", "AUTORIZACAO_REMOTA_CERTIFICADO"),
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Segregação por tribunal superior, turma e plenário competentes."),
                List.of("Gabinete superior em trilha direta com reforço institucional.")));
        for (InstitutionalOrganizationBlueprint blueprint : blueprints) {
            for (InstitutionalAccessLaneBlueprint lane : blueprint.lanes()) {
                entries.add(toEntry(blueprint, lane));
            }
        }
        return entries.stream().sorted(Comparator.comparing(InstitutionalTrustMatrixEntry::codigo)).toList();
    }

    private InstitutionalTrustMatrixEntry toEntry(InstitutionalOrganizationBlueprint blueprint,
                                                  InstitutionalAccessLaneBlueprint lane) {
        ArrayList<String> fatoresObrigatorios = new ArrayList<>();
        fatoresObrigatorios.add("LOGIN_GOVBR");
        fatoresObrigatorios.add("NOMEACAO_ATIVA");
        fatoresObrigatorios.add("AFILIACAO_HOMOLOGADA");
        if (lane.requerStepUpMfa()) {
            fatoresObrigatorios.add("MFA_ATIVO");
        }
        if (lane.requerCertificadoICP()) {
            fatoresObrigatorios.add("CERTIFICADO_ICP_BRASIL");
        }
        if (lane.requerRedeInstitucional()) {
            fatoresObrigatorios.add("REDE_INSTITUCIONAL_CONFIAVEL_OU_AUTORIZACAO_REMOTA_CERTIFICADO");
        }
        if (blueprint.requerDuplaAprovacaoAdministrador() && lane.nominationRole() != null && lane.nominationRole().isGestaoMestre()) {
            fatoresObrigatorios.add("DUPLA_APROVACAO_ADMINISTRADOR_MESTRE");
        }
        ArrayList<String> fatoresComplementares = new ArrayList<>();
        fatoresComplementares.add("DISPOSITIVO_HOMOLOGADO");
        if (blueprint.permiteUsoRemotoComAutorizacao() || lane.permiteUsoRemotoAutorizado()) {
            fatoresComplementares.add("AUTORIZACAO_REMOTA_CERTIFICADO");
        }
        ArrayList<String> guardRails = new ArrayList<>();
        guardRails.add("orgao_nomeia_pessoas_e_pjb_homologa");
        guardRails.add("conta_compartilhada_proibida");
        guardRails.add("recertificacao_periodica_obrigatoria");
        guardRails.add("revogacao_imediata_por_desligamento_ou_lotacao");
        if (blueprint.restringeCertificadoRedeInstitucional() || lane.requerRedeInstitucional()) {
            guardRails.add("certificado_local_ou_autorizacao_remota_de_diretoria");
        }
        if (lane.nominationRole() != null && lane.nominationRole().isGestaoMestre()) {
            guardRails.add("segregacao_entre_gestao_e_assinatura_final");
        }
        ArrayList<String> rotas = new ArrayList<>();
        rotas.add(resolveLandingPath(lane.panel()));
        return new InstitutionalTrustMatrixEntry(
                blueprint.codigo() + "__" + lane.codigo(),
                blueprint.scope().name(),
                blueprint.nomeExibicao() + " / " + lane.nomeExibicao(),
                blueprint.entryMode().name(),
                lane.laneKind().name(),
                lane.nominationRole().name(),
                lane.processProfile().name(),
                lane.panel().name(),
                lane.trustFloor().name(),
                fatoresObrigatorios,
                fatoresComplementares,
                lane.capacidadesPadrao().stream().map(Enum::name).sorted().toList(),
                lane.restricoes(),
                guardRails,
                rotas,
                mergeFundamentos(blueprint.fundamentos(), lane.fundamentos())
        );
    }

    private InstitutionalTrustMatrixEntry directEntry(String codigo,
                                                      String escopo,
                                                      String nome,
                                                      InstitutionalProcessProfile profile,
                                                      InstitutionalEntryLandingPanel panel,
                                                      InstitutionalTrustLevel trust,
                                                      List<String> obrigatorios,
                                                      List<String> complementares,
                                                      java.util.Set<CapacidadeCaixaInstitucional> capacidades,
                                                      List<String> restricoes,
                                                      List<String> fundamentos) {
        return new InstitutionalTrustMatrixEntry(
                codigo,
                escopo,
                nome,
                InstitutionalEntryMode.DIRETO_PESSOA.name(),
                null,
                null,
                profile.name(),
                panel.name(),
                trust.name(),
                obrigatorios,
                complementares,
                capacidades.stream().map(Enum::name).sorted().toList(),
                restricoes,
                List.of("perfil_direto_sem_adesao_institucional"),
                List.of(resolveLandingPath(panel)),
                fundamentos
        );
    }

    private String resolveLandingPath(InstitutionalEntryLandingPanel panel) {
        return switch (panel) {
            case PAINEL_TITULAR, PAINEL_ORGAO, PAINEL_UNIDADE, PAINEL_APOIO_TECNICO, PAINEL_ADMINISTRATIVO, PAINEL_DIRETORIA_FORUM ->
                    InstitutionalApiRoutes.painelExecutivo();
            case PAINEL_SECRETARIA_FORUM -> OperationalApiRoutes.secretariatOperationalSnapshot();
            case PAINEL_AUDIENCIAS_CONCILIACAO -> "/api/v1/conciliacao/operacional/processos/0/agendamento";
            case PAINEL_TRIAGEM, PAINEL_CAIXA -> InstitutionalApiRoutes.inbox();
            case PAINEL_DELEGACIA -> InstitutionalApiRoutes.painelExecutivo("DELEGACIA");
            case PAINEL_CUSTODIA_PRISIONAL -> InstitutionalApiRoutes.painelExecutivo("UNIDADE_PRISIONAL");
            case PAINEL_TECNICO_JUDICIAL -> InstitutionalApiRoutes.painelExecutivo("APOIO_TECNICO");
            case PAINEL_COOPERACAO_JUDICIAL -> InstitutionalApiRoutes.painelExecutivo("COOPERACAO");
        };
    }

    private List<String> mergeFundamentos(List<String> left, List<String> right) {
        ArrayList<String> merged = new ArrayList<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        return List.copyOf(merged);
    }
}
