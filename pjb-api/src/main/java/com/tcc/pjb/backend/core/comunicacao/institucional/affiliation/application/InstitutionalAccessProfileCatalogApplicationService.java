package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAccessProfileCatalogApplicationService {

    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;

    public InstitutionalAccessProfileCatalogApplicationService(InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService) {
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
    }

    public List<InstitutionalAccessProfileCatalogEntry> listarPerfis() {
        ArrayList<InstitutionalAccessProfileCatalogEntry> entries = new ArrayList<>();
        entries.add(entry("CIDADAO_DIRETO", "Cidadão/parte", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.PERFIL_HIBRIDO,
                InstitutionalEntryLandingPanel.PAINEL_UNIDADE, InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Não assume contexto institucional."),
                List.of("Acesso aos próprios processos, comunicações e notificações.")));
        entries.add(entry("ADVOGADO_DIRETO", "Advogado", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.PERFIL_HIBRIDO,
                InstitutionalEntryLandingPanel.PAINEL_UNIDADE, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Não representa órgão institucional por padrão."),
                List.of("Entrada pessoal, OAB e certificado conforme o ato.")));
        entries.add(entry("OAB_SECCIONAL_DIRETO", "Presidência seccional da OAB", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.PERFIL_HIBRIDO,
                InstitutionalEntryLandingPanel.PAINEL_ADMINISTRATIVO, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Não substitui a atuação pessoal da advocacia privada.", "Atuação institucional limitada à governança seccional."),
                List.of("Entrada pessoal da presidência seccional com OAB validada, MFA e certificado quando o ato exigir.")));
        entries.add(entry("MAGISTRADO_DIRETO", "Magistrado", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Decisão jurisdicional permanece no fluxo próprio."),
                List.of("Perfil direto do sistema, separado do bloco institucional afiliado.")));
        entries.add(entry("JUIZ_ESTADUAL_DIRETO", "Juiz estadual", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Escopo jurisdicional limitado à justiça estadual e à unidade competente."),
                List.of("Acesso direto do juiz estadual com segregação por unidade e caixa institucional.")));
        entries.add(entry("JUIZ_FEDERAL_DIRETO", "Juiz federal", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Escopo jurisdicional limitado à justiça federal e à unidade competente."),
                List.of("Acesso direto do juiz federal com segregação por seção, subseção e vara.")));
        entries.add(entry("DESEMBARGADOR_ESTADUAL_DIRETO", "Desembargador estadual", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Escopo jurisdicional limitado ao tribunal estadual competente."),
                List.of("Acesso direto para gabinete e colegiado de tribunal de justiça.")));
        entries.add(entry("DESEMBARGADOR_FEDERAL_DIRETO", "Desembargador federal", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Escopo jurisdicional limitado ao tribunal regional federal competente."),
                List.of("Acesso direto para gabinete e colegiado de TRF.")));
        entries.add(entry("MINISTRO_DIRETO", "Ministro", InstitutionalEntryMode.DIRETO_PESSOA, null, InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                EnumSet.noneOf(CapacidadeCaixaInstitucional.class),
                List.of("Escopo jurisdicional limitado ao tribunal superior competente."),
                List.of("Acesso direto do ministro com segregação por corte superior, órgão fracionário e plenário.")));
        for (InstitutionalOrganizationBlueprint blueprint : blueprintCatalogApplicationService.listar()) {
            blueprint.lanes().forEach(lane -> entries.add(entry(
                    blueprint.codigo() + "__" + lane.codigo(),
                    blueprint.nomeExibicao() + " / " + lane.nomeExibicao(),
                    blueprint.entryMode(),
                    lane.nominationRole(),
                    lane.processProfile(),
                    lane.panel(),
                    lane.trustFloor(),
                    lane.capacidadesPadrao(),
                    lane.restricoes(),
                    mergeFundamentos(blueprint.fundamentos(), lane.fundamentos())
            )));
        }
        return List.copyOf(entries);
    }

    private List<String> mergeFundamentos(List<String> left, List<String> right) {
        ArrayList<String> merged = new ArrayList<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        return List.copyOf(merged);
    }

    private InstitutionalAccessProfileCatalogEntry entry(String codigo,
                                                         String nome,
                                                         InstitutionalEntryMode mode,
                                                         com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole role,
                                                         InstitutionalProcessProfile profile,
                                                         InstitutionalEntryLandingPanel panel,
                                                         InstitutionalTrustLevel trust,
                                                         java.util.Set<CapacidadeCaixaInstitucional> capacidades,
                                                         java.util.List<String> restricoes,
                                                         java.util.List<String> fundamentos) {
        return new InstitutionalAccessProfileCatalogEntry(codigo, nome, mode, role, profile, panel, trust, capacidades, restricoes, fundamentos);
    }
}
