package com.tcc.pjb.backend.core.comunicacao.institucional.processual.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessAuthorityBand;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessQueueSectionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessSeparatorSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessVisualLaneSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspaceSummary;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

final class InstitutionalProcessWorkspaceAssembler {

    private static final String RED = "#dc2626";
    private static final String AMBER = "#f59e0b";
    private static final String BLUE = "#2563eb";
    private static final String EMERALD = "#059669";
    private static final String VIOLET = "#7c3aed";
    private static final String SLATE = "#334155";
    private static final String SKY = "#0284c7";
    private static final String FUCHSIA = "#c026d3";

    private final InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService;

    InstitutionalProcessWorkspaceAssembler(InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService) {
        this.panelBlueprintApplicationService = Objects.requireNonNull(panelBlueprintApplicationService);
    }

    InstitutionalProcessWorkspaceSummary summarize(InstitutionalAccessProfileCatalogEntry entry, InstitutionalProcessWorkspaceSnapshot snapshot) {
        InstitutionalProcessWorkspace workspace = toWorkspace(entry, snapshot);
        return new InstitutionalProcessWorkspaceSummary(
                workspace.profileCode(),
                workspace.displayName(),
                workspace.panel(),
                workspace.processProfile(),
                workspace.trustFloor(),
                workspace.accentColor(),
                workspace.actions().size(),
                workspace.sections().size(),
                workspace.authorityBands().size(),
                workspace.separators().size(),
                workspace.tabs(),
                workspace.fundamentos()
        );
    }

    InstitutionalProcessWorkspace toWorkspace(InstitutionalAccessProfileCatalogEntry entry, InstitutionalProcessWorkspaceSnapshot snapshot) {
        String scopePrefix = extractScopePrefix(entry.codigo());
        List<InstitutionalPanelBlueprintSpec> panelSpecs = scopePrefix == null ? List.of() : panelBlueprintApplicationService.listar(scopePrefix, entry.panel().name());
        String accentColor = resolveAccentColor(entry, snapshot);
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(entry.fundamentos());
        panelSpecs.forEach(spec -> fundamentos.addAll(spec.fundamentos()));
        List<InstitutionalProcessActionSpec> actions = buildActions(entry, snapshot, panelSpecs);
        List<InstitutionalProcessQueueSectionSpec> sections = buildSections(entry, snapshot, panelSpecs, accentColor);
        List<String> tabs = buildTabs(entry, snapshot, panelSpecs);
        List<String> quickFilters = buildQuickFilters(snapshot, sections);
        List<InstitutionalProcessVisualLaneSpec> visualLanes = buildVisualLanes(entry, snapshot, sections);
        List<InstitutionalProcessAuthorityBand> authorityBands = buildAuthorityBands(entry, snapshot, actions);
        List<InstitutionalProcessSeparatorSpec> separators = buildSeparators(entry, snapshot, tabs, sections, visualLanes, accentColor);
        return new InstitutionalProcessWorkspace(
                entry.codigo(),
                entry.nomeExibicao(),
                entry.panel().name(),
                entry.processProfile().name(),
                entry.trustFloor().name(),
                accentColor,
                snapshot.rito() == null ? null : snapshot.rito().name(),
                snapshot.fase() == null ? null : snapshot.fase().name(),
                snapshot.status() == null ? null : snapshot.status().name(),
                snapshot.ramo() == null ? null : snapshot.ramo().name(),
                tabs,
                quickFilters,
                buildRecursos(entry, snapshot),
                buildEmbargos(entry, snapshot),
                actions,
                sections,
                visualLanes,
                authorityBands,
                separators,
                List.copyOf(fundamentos)
        );
    }

    private List<String> buildQuickFilters(InstitutionalProcessWorkspaceSnapshot snapshot, List<InstitutionalProcessQueueSectionSpec> sections) {
        LinkedHashSet<String> filters = new LinkedHashSet<>();
        sections.forEach(section -> filters.addAll(section.filtros()));
        if (snapshot.urgente()) {
            filters.add("urgencia=SIM");
        }
        if (snapshot.recursal()) {
            filters.add("faixa=RECURSAL");
        }
        if (snapshot.embargos()) {
            filters.add("faixa=EMBARGOS");
        }
        if (snapshot.execucao()) {
            filters.add("faixa=EXECUCAO");
        }
        return List.copyOf(filters);
    }

    private List<InstitutionalProcessVisualLaneSpec> buildVisualLanes(InstitutionalAccessProfileCatalogEntry entry,
                                                                      InstitutionalProcessWorkspaceSnapshot snapshot,
                                                                      List<InstitutionalProcessQueueSectionSpec> sections) {
        ArrayList<InstitutionalProcessVisualLaneSpec> lanes = new ArrayList<>();
        lanes.add(lane("TRILHA_RECEBIMENTO", "Recebimento e ciência", BLUE, 1, true,
                List.of("status=DISPONIBILIZADA", "status=RECEBIDA", "status=AGUARDANDO_PARECER"),
                List.of("nova_entrada", "ciencia_pendente"),
                List.of("Separa o que entrou, foi recebido e ainda exige leitura oficial.")));
        lanes.add(lane("TRILHA_MINUTA", "Minutas, pareceres e respostas", VIOLET, 2,
                hasAnyCapacity(entry.capacidadesPadrao(), CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO),
                List.of("fila=minutas", "fila=pareceres", "fila=respostas"),
                List.of("minuta", "parecer", "assinatura"),
                List.of("Destaca o que precisa ser preparado por assessor e assinado por titular.")));
        lanes.add(lane("TRILHA_RECURSAL", "Recursos e contrarrazões", FUCHSIA, 3, snapshot.recursal() || allowsPetitioning(entry),
                List.of("fase=RECURSAL", "status=RECURSO_INTERPOSTO"),
                List.of("apelacao", "agravo", "contrarrazoes"),
                List.of("Faixa própria para movimentação recursal e remessa ao órgão revisor.")));
        lanes.add(lane("TRILHA_EMBARGOS", "Embargos e integração", AMBER, 4, snapshot.embargos() || allowsPetitioning(entry),
                List.of("status=EMBARGOS_DECLARACAO", "fila=embargos"),
                List.of("integracao", "omissao", "obscuridade", "contradicao"),
                List.of("Embargos entram em separador próprio para evitar mistura com mérito ordinário.")));
        lanes.add(lane("TRILHA_EXECUCAO", "Cumprimento e execução", SKY, 5, snapshot.execucao() || entry.processProfile() == InstitutionalProcessProfile.CONTADOR_JUDICIAL,
                List.of("fase=EXECUCAO", "fase=CUMPRIMENTO_SENTENCA", "rito=EXECUCAO_FISCAL"),
                List.of("bloqueio", "penhora", "cumprimento", "calculo"),
                List.of("Execução, cálculo, cumprimento e certidões materiais ficam reunidos nesta trilha.")));
        lanes.add(lane("TRILHA_URGENTE", "Urgências e tutela imediata", RED, 6, snapshot.urgente(),
                List.of("urgencia=SIM", "assunto=liminar", "assunto=custodia"),
                List.of("urgente", "liminar", "custodia"),
                List.of("Tudo que exige atuação imediata entra em destaque máximo visual.")));
        lanes.add(lane("TRILHA_TECNICA", "Laudos, estudos e diligências", EMERALD, 7,
                isTechnical(entry.processProfile()),
                List.of("fila=laudos", "fila=estudos", "fila=diligencias"),
                List.of("laudo", "estudo", "resposta_tecnica"),
                List.of("Apoio técnico fica isolado para preservar trilha de autoria especializada.")));
        lanes.add(lane("TRILHA_GOVERNANCA", "Governança, delegação e cobertura", SLATE, 8,
                entry.nominationRole() != null && entry.nominationRole().isGestaoMestre(),
                List.of("fila=delegacoes", "fila=substituicoes", "fila=plantao"),
                List.of("gestao", "delegacao", "substituicao"),
                List.of("Painel de gestão institucional para lotação, cobertura, plantão e revogação.")));
        if (sections.stream().noneMatch(section -> section.code().equals("SECAO_TRILHA_URGENTE")) && snapshot.urgente()) {
            lanes.add(lane("TRILHA_CUSTODIA", "Custódia e apresentação", RED, 9,
                    entry.processProfile() == InstitutionalProcessProfile.POLICIAL_PENAL
                            || entry.processProfile() == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL
                            || entry.processProfile() == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL,
                    List.of("fila=custodia", "fase=AUDIENCIA_CUSTODIA"),
                    List.of("custodia", "escolta", "alvara"),
                    List.of("Fluxo próprio para preso, alvará, transferência e apresentação.")));
        }
        return lanes.stream().sorted(Comparator.comparing(InstitutionalProcessVisualLaneSpec::ordem)).toList();
    }

    private List<InstitutionalProcessAuthorityBand> buildAuthorityBands(InstitutionalAccessProfileCatalogEntry entry,
                                                                        InstitutionalProcessWorkspaceSnapshot snapshot,
                                                                        List<InstitutionalProcessActionSpec> actions) {
        ArrayList<InstitutionalProcessAuthorityBand> bands = new ArrayList<>();
        bands.add(authorityBand("AUTORIDADE_RECEBIMENTO", "Recebimento e ciência", BLUE,
                actionsForBand(actions, "RECEBER_", "DAR_CIENCIA", "GERAR_CERTIDAO", "CLASSIFICAR_FLUXO"),
                prohibitedForBand(entry, "ciência irrestrita", "assinatura final sem step-up"),
                List.of("mfa", "vinculo_institucional_ativo"),
                List.of("Toda atuação começa pela leitura institucional rastreável.", "Recebimento não autoriza petição automática em nome do órgão.")));
        bands.add(authorityBand("AUTORIDADE_MANIFESTACAO", "Minutas, pareceres e assinatura", VIOLET,
                actionsForBand(actions, "PREPARAR_MINUTA", "ASSINAR_MANIFESTACAO", "EMITIR_PARECER", "MINUTA_", "PROMOVER_", "APRESENTAR_DEFESA", "APRESENTAR_INFORMACOES"),
                prohibitedForBand(entry, "decisão jurisdicional", "aprovação final por perfil sem legitimidade"),
                List.of("segregacao_funcoes", "step_up_assinatura", "carimbo_do_tempo"),
                List.of("Parecer, minuta e assinatura permanecem segregados quando o papel não for titular.", "Titular, defensor e procurador concentram atos finais do mérito institucional.")));
        bands.add(authorityBand("AUTORIDADE_RECURSAL", "Recursos, contrarrazões e embargos", FUCHSIA,
                actionsForBand(actions, "PETICIONAR_ORGAO", "ATUAR_EMBARGOS", "INTERPOR_", "APRESENTAR_", "SUSTENTAR_", "NEGOCIAR_", "RECORRER_", "RECURSO_", "EMBARGOS_"),
                prohibitedForBand(entry, "recurso fora da trilha adequada", "mistura de embargos com mérito ordinário"),
                List.of("controle_de_prazo", "caixa_recursal", "assinatura_reforcada"),
                List.of("Recursos e embargos usam aba própria e gates próprios.", "Atos recursais não podem ficar soterrados em fila genérica.")));
        bands.add(authorityBand("AUTORIDADE_EXECUCAO", "Cumprimento, execução e certidões materiais", SKY,
                actionsForBand(actions, "REGISTRAR_CUMPRIMENTO", "OFERECER_ACORDO", "OPOR_EMBARGOS_EXECUCAO", "SUBMETER_LAUDO", "CONFIRMAR_CUSTODIA", "REGISTRAR_APRESENTACAO"),
                prohibitedForBand(entry, "cumprimento sem prova material", "execução sem segregação por fase"),
                List.of("trilha_material", "evidencia_operacional"),
                List.of("Execução e cumprimento têm separador visual e filtros próprios.", "Certidão operacional não substitui manifestação jurídica quando houver mérito.")));
        bands.add(authorityBand("AUTORIDADE_GOVERNANCA", "Governança, lotação e cobertura", SLATE,
                actionsForBand(actions, "GERIR_LOTACAO", "HOMOLOGAR_GUARDA", "GOVERNANCA_OAB", "REDISTRIBUIR_INTERNO", "ATRIBUIR_MEMBRO"),
                prohibitedForBand(entry, "peticionar no mérito sem legitimação", "assinar manifestação apenas por ser gestor"),
                List.of("dupla_validacao", "trilha_forense", "vigencia_delegacao"),
                List.of("Gestão institucional não se confunde com poder de manifestação processual.", "Plantão, substituição e delegação precisam de fundamento próprio.")));
        if (snapshot.urgente()) {
            bands.add(authorityBand("AUTORIDADE_URGENTE", "Atos urgentes e custódia", RED,
                    actionsForBand(actions, "ESCALAR_TITULAR", "CONFIRMAR_CUSTODIA", "REGISTRAR_APRESENTACAO"),
                    prohibitedForBand(entry, "represamento de urgência", "manter custódia sem confirmação"),
                    List.of("prioridade_maxima", "alerta_imediato", "reautenticacao_contextual"),
                    List.of("Urgência, liminar e custódia sobem para o topo visual do painel.", "Ato urgente exige contexto ativo e fila destacada.")));
        }
        return bands.stream().filter(band -> band.enabled()).sorted(Comparator.comparing(InstitutionalProcessAuthorityBand::title)).toList();
    }

    private InstitutionalProcessAuthorityBand authorityBand(String code,
                                                            String title,
                                                            String accentColor,
                                                            List<String> allowedActions,
                                                            List<String> prohibitedActions,
                                                            List<String> requiredGuards,
                                                            List<String> fundamentos) {
        return new InstitutionalProcessAuthorityBand(code, title, accentColor, !allowedActions.isEmpty(), containsSensitiveAction(allowedActions), allowedActions, prohibitedActions, requiredGuards, fundamentos);
    }

    private List<String> actionsForBand(List<InstitutionalProcessActionSpec> actions, String... fragments) {
        LinkedHashSet<String> allowed = new LinkedHashSet<>();
        for (InstitutionalProcessActionSpec action : actions) {
            String code = normalize(action.code());
            for (String fragment : fragments) {
                if (code.contains(normalize(fragment))) {
                    allowed.add(action.title());
                    break;
                }
            }
        }
        return List.copyOf(allowed);
    }

    private List<String> prohibitedForBand(InstitutionalAccessProfileCatalogEntry entry, String... defaults) {
        LinkedHashSet<String> prohibited = new LinkedHashSet<>(List.of(defaults));
        prohibited.addAll(entry.restricoes());
        if (!allowsPetitioning(entry)) {
            prohibited.add("peticionar em nome do órgão sem delegação própria");
        }
        if (!hasAnyCapacity(entry.capacidadesPadrao(), CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)) {
            prohibited.add("assinar manifestação final");
        }
        return List.copyOf(prohibited);
    }

    private boolean containsSensitiveAction(List<String> allowedActions) {
        return allowedActions.stream().map(this::normalize).anyMatch(item -> item.contains("ASSIN") || item.contains("CIENCIA") || item.contains("CUSTODIA") || item.contains("HOMOLOGAR"));
    }

    private List<InstitutionalProcessSeparatorSpec> buildSeparators(InstitutionalAccessProfileCatalogEntry entry,
                                                                   InstitutionalProcessWorkspaceSnapshot snapshot,
                                                                   List<String> tabs,
                                                                   List<InstitutionalProcessQueueSectionSpec> sections,
                                                                   List<InstitutionalProcessVisualLaneSpec> visualLanes,
                                                                   String accentColor) {
        ArrayList<InstitutionalProcessSeparatorSpec> separators = new ArrayList<>();
        separators.add(separator("SEP_RECEBIMENTO", "Separador de recebimento", BLUE, 1, true,
                List.of("tab=recebidos", "tab=pendentes_de_ciencia"),
                List.of("entrada_nova", "ciencia", "prazo"),
                List.of("Comunicações recebidas não se misturam com minutas ou embargos.")));
        separators.add(separator("SEP_RITO", "Separador por rito e ramo", accentColor, 2, snapshot.rito() != null || snapshot.ramo() != null,
                List.of(snapshot.rito() == null ? "rito=NAO_INFORMADO" : "rito=" + snapshot.rito().name(), snapshot.ramo() == null ? "ramo=NAO_INFORMADO" : "ramo=" + snapshot.ramo().name()),
                List.of(groupToken(snapshot), ramoToken(snapshot)),
                List.of("Rito e ramo puxam filtros e caixas corretas para cada órgão.")));
        separators.add(separator("SEP_MINUTA", "Separador de minutas e pareceres", VIOLET, 3, tabs.contains("minutas_e_pareceres"),
                List.of("tab=minutas_e_pareceres", "fila=pareceres"),
                List.of("minuta", "parecer", "assinatura"),
                List.of("Titular, assessor e triagem não compartilham a mesma subfila de aprovação.")));
        separators.add(separator("SEP_RECURSAL", "Separador recursal", FUCHSIA, 4, snapshot.recursal() || tabs.contains("recursos"),
                List.of("tab=recursos", "fase=RECURSAL"),
                List.of("apelacao", "agravo", "contrarrazoes"),
                List.of("Recursos saem do fluxo ordinário e ganham ordenação por prazo e órgão revisor.")));
        separators.add(separator("SEP_EMBARGOS", "Separador de embargos", AMBER, 5, snapshot.embargos() || tabs.contains("embargos"),
                List.of("tab=embargos", "fila=embargos"),
                List.of("omissao", "contradicao", "obscuridade"),
                List.of("Embargos têm cor e guardas próprias para evitar mistura com recurso principal.")));
        separators.add(separator("SEP_EXECUCAO", "Separador de cumprimento e execução", SKY, 6, snapshot.execucao() || tabs.contains("cumprimento_execucao"),
                List.of("tab=cumprimento_execucao", "fase=EXECUCAO"),
                List.of("penhora", "bloqueio", "calculo"),
                List.of("Execução, cumprimento e liquidação seguem esteira própria.")));
        separators.add(separator("SEP_URGENTE", "Separador de urgência", RED, 7, snapshot.urgente() || tabs.contains("urgencias"),
                List.of("tab=urgencias", "urgencia=SIM"),
                List.of("liminar", "custodia", "plantao"),
                List.of("Urgências sempre sobem visualmente acima das demais filas.")));
        separators.add(separator("SEP_TECNICO", "Separador técnico", EMERALD, 8, isTechnical(entry.processProfile()) || tabs.contains("laudos_e_estudos"),
                List.of("fila=laudos", "fila=estudos"),
                List.of("laudo", "estudo", "resposta_tecnica"),
                List.of("Produção técnica não pode contaminar fila de mérito jurídico.")));
        separators.add(separator("SEP_GOVERNANCA", "Separador de governança", SLATE, 9, entry.nominationRole() != null && entry.nominationRole().isGestaoMestre(),
                List.of("fila=delegacao", "fila=substituicao", "fila=plantao"),
                List.of("governanca", "cobertura", "vigencia"),
                List.of("Gestão institucional fica isolada de atos de petição e resposta.")));
        if (sections.stream().anyMatch(section -> section.code().equals("SECAO_CUSTODIA")) || visualLanes.stream().anyMatch(lane -> lane.code().equals("TRILHA_CUSTODIA"))) {
            separators.add(separator("SEP_CUSTODIA", "Separador de custódia", RED, 10, true,
                    List.of("tab=custodia", "fila=custodia"),
                    List.of("apresentacao", "transferencia", "alvara"),
                    List.of("Custódia e apresentação de preso exigem controle material e prova de execução.")));
        }
        return separators.stream().sorted(Comparator.comparing(InstitutionalProcessSeparatorSpec::ordem)).toList();
    }

    private InstitutionalProcessSeparatorSpec separator(String code,
                                                       String title,
                                                       String accentColor,
                                                       int ordem,
                                                       boolean active,
                                                       List<String> filtros,
                                                       List<String> marcadores,
                                                       List<String> fundamentos) {
        return new InstitutionalProcessSeparatorSpec(code, title, accentColor, ordem, active, filtros, marcadores, fundamentos);
    }

    private String groupToken(InstitutionalProcessWorkspaceSnapshot snapshot) {
        if (snapshot.rito() == null) {
            return "grupo=NEUTRO";
        }
        if (snapshot.rito().isTrabalhista()) {
            return "grupo=TRABALHISTA";
        }
        if (snapshot.rito().isEleitoral()) {
            return "grupo=ELEITORAL";
        }
        if (snapshot.rito().isMilitar()) {
            return "grupo=MILITAR";
        }
        if (snapshot.rito().isPenal()) {
            return "grupo=PENAL";
        }
        if (snapshot.rito().isExecucaoFiscalEstrita()) {
            return "grupo=EXECUCAO_FISCAL";
        }
        return "grupo=COMUM";
    }

    private String ramoToken(InstitutionalProcessWorkspaceSnapshot snapshot) {
        return snapshot.ramo() == null ? "ramo=NAO_INFORMADO" : "ramo=" + snapshot.ramo().name();
    }

    private List<String> buildTabs(InstitutionalAccessProfileCatalogEntry entry,
                                   InstitutionalProcessWorkspaceSnapshot snapshot,
                                   List<InstitutionalPanelBlueprintSpec> panelSpecs) {
        LinkedHashSet<String> tabs = new LinkedHashSet<>();
        tabs.add("recebidos");
        tabs.add("pendentes_de_ciencia");
        if (supportsHearingSurface(entry)) {
            tabs.add("audiencias_e_calendario");
            tabs.add("pauta_e_salas");
        }
        if (hasAnyCapacity(entry.capacidadesPadrao(), CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO) || supportsOpinionFlow(entry)) {
            tabs.add("minutas_e_pareceres");
            tabs.add("pareceres_institucionais");
        }
        if (supportsCalculatorSurface(entry)) {
            tabs.add("calculadora_judicial");
        }
        if (allowsPetitioning(entry)) {
            tabs.add("peticoes_e_respostas");
            tabs.add("recursos");
            tabs.add("embargos");
        }
        if (snapshot.execucao() || entry.processProfile() == InstitutionalProcessProfile.CONTADOR_JUDICIAL) {
            tabs.add("cumprimento_execucao");
        }
        if (entry.nominationRole() != null && entry.nominationRole().isGestaoMestre()) {
            tabs.add("governanca");
            tabs.add("administracao_institucional");
        }
        if (snapshot.urgente()) {
            tabs.add("urgencias");
        }
        if (entry.processProfile() == InstitutionalProcessProfile.POLICIAL_PENAL
                || entry.processProfile() == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL
                || entry.processProfile() == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL) {
            tabs.add("custodia");
            tabs.add("apresentacoes");
        }
        if (isTechnical(entry.processProfile())) {
            tabs.add("laudos_e_estudos");
        }
        panelSpecs.forEach(spec -> tabs.addAll(spec.secoesPrimarias()));
        return List.copyOf(tabs);
    }

    private List<InstitutionalProcessQueueSectionSpec> buildSections(InstitutionalAccessProfileCatalogEntry entry,
                                                                     InstitutionalProcessWorkspaceSnapshot snapshot,
                                                                     List<InstitutionalPanelBlueprintSpec> panelSpecs,
                                                                     String accentColor) {
        ArrayList<InstitutionalProcessQueueSectionSpec> sections = new ArrayList<>();
        sections.add(section("SECAO_RECEBIMENTO", "Recebimento e ciência", BLUE, 1,
                List.of("status=DISPONIBILIZADA", "status=RECEBIDA"),
                List.of("contador_sem_leitura", "prazo_mais_curto", "ultimo_ingresso"),
                List.of("prioridade_desc", "prazo_asc", "recebido_em_desc")));
        if (supportsHearingSurface(entry)) {
            sections.add(section("SECAO_AUDIENCIAS", "Audiências, pauta e calendário", EMERALD, 2,
                    List.of("fila=audiencias", "fila=pauta", "evento=audiencia_designada"),
                    List.of("sala_reservada", "link_virtual", "comparecimento_confirmado"),
                    List.of("data_audiencia_asc", "prioridade_desc")));
        }
        if (supportsOpinionFlow(entry)) {
            sections.add(section("SECAO_PARECERES", "Pareceres, manifestações e minutas", VIOLET, 3,
                    List.of("fila=pareceres", "fila=manifestacoes", "fila=minutas"),
                    List.of("assinatura_pendente", "revisao_minuta", "prazo_manifestacao"),
                    List.of("prazo_asc", "entrada_desc")));
        }
        if (supportsCalculatorSurface(entry)) {
            sections.add(section("SECAO_CALCULADORA", "Calculadora judicial e memória de cálculo", SKY, 4,
                    List.of("fila=calculadora", "fase=LIQUIDACAO", "fase=EXECUCAO"),
                    List.of("memoria_calculo", "parametros_atualizados", "indice_correcao"),
                    List.of("prioridade_desc", "valor_desc")));
        }
        if (hasAnyCapacity(entry.capacidadesPadrao(), CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)) {
            sections.add(section("SECAO_MINUTAS", "Minutas, pareceres e assinaturas", VIOLET, 2,
                    List.of("fila=minutas", "fila=pareceres"),
                    List.of("minutas_pendentes", "assinatura_pendente", "delegacao_ativa"),
                    List.of("urgencia_desc", "atribuido_ao_usuario_desc", "fila_desc")));
        }
        if (allowsPetitioning(entry)) {
            sections.add(section("SECAO_RECURSAL", "Recursos, contrarrazões e incidentes", FUCHSIA, 3,
                    List.of("fase=RECURSAL", "status=RECURSO_INTERPOSTO"),
                    List.of("prazo_recursal", "ultima_decisao", "orgao_revisor"),
                    List.of("prazo_asc", "movimentacao_desc")));
            sections.add(section("SECAO_EMBARGOS", "Embargos e integração de decisão", AMBER, 4,
                    List.of("status=EMBARGOS_DECLARACAO", "fila=embargos"),
                    List.of("tipo_embargo", "prazo_embargos", "ato_integrado"),
                    List.of("prazo_asc", "entrada_desc")));
        }
        if (snapshot.execucao() || entry.processProfile() == InstitutionalProcessProfile.CONTADOR_JUDICIAL) {
            sections.add(section("SECAO_EXECUCAO", "Cumprimento, execução e cálculo", SKY, 5,
                    List.of("fase=EXECUCAO", "fase=CUMPRIMENTO_SENTENCA", "rito=EXECUCAO_FISCAL"),
                    List.of("bloqueio_pendente", "penhora", "memoria_calculo"),
                    List.of("valor_desc", "prazo_asc", "movimentacao_desc")));
        }
        if (snapshot.urgente()) {
            sections.add(section("SECAO_TRILHA_URGENTE", "Urgências, liminares e custódia", RED, 6,
                    List.of("urgencia=SIM", "assunto=liminar", "fase=AUDIENCIA_CUSTODIA"),
                    List.of("sla_critico", "ordem_imediata", "janela_horas"),
                    List.of("urgencia_desc", "prazo_asc")));
        }
        if (isTechnical(entry.processProfile())) {
            sections.add(section("SECAO_TECNICA", "Laudos, estudos e devoluções técnicas", EMERALD, 7,
                    List.of("fila=laudos", "fila=estudos", "fase=PERICIA_TECNICA"),
                    List.of("documentos_pendentes", "resposta_tecnica", "prazo_laudo"),
                    List.of("prazo_asc", "distribuicao_desc")));
        }
        if (entry.processProfile() == InstitutionalProcessProfile.POLICIAL_PENAL
                || entry.processProfile() == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL
                || entry.processProfile() == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL) {
            sections.add(section("SECAO_CUSTODIA", "Custódia, transferência e apresentação", RED, 8,
                    List.of("fila=custodia", "fila=apresentacao", "rito=EXECUCAO_PENAL"),
                    List.of("preso_apresentado", "alvara_pendente", "transferencia_pendente"),
                    List.of("prazo_asc", "entrada_desc")));
        }
        if (entry.nominationRole() != null && entry.nominationRole().isGestaoMestre()) {
            sections.add(section("SECAO_GOVERNANCA", "Governança, lotação e cobertura", SLATE, 9,
                    List.of("fila=plantao", "fila=substituicao", "fila=delegacao"),
                    List.of("nomeacao_ativa", "autorizacao_remota", "recertificacao"),
                    List.of("criticidade_desc", "vigencia_asc")));
        }
        panelSpecs.forEach(spec -> sections.add(section(
                "PANEL_" + spec.codigo(),
                spec.titulo(),
                accentColor,
                50 + sections.size(),
                spec.secoesPrimarias(),
                spec.guardasSeguranca(),
                List.of("ordem_painel"))));
        return sections.stream()
                .collect(java.util.stream.Collectors.toMap(InstitutionalProcessQueueSectionSpec::code, item -> item, (left, right) -> left))
                .values().stream()
                .sorted(Comparator.comparing(InstitutionalProcessQueueSectionSpec::ordem))
                .toList();
    }

    private List<InstitutionalProcessActionSpec> buildActions(InstitutionalAccessProfileCatalogEntry entry,
                                                              InstitutionalProcessWorkspaceSnapshot snapshot,
                                                              List<InstitutionalPanelBlueprintSpec> panelSpecs) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<InstitutionalProcessActionSpec> actions = new ArrayList<>();
        addCommonCapacityActions(actions, seen, entry.capacidadesPadrao(), snapshot);
        addProfileActions(actions, seen, entry, snapshot);
        panelSpecs.forEach(spec -> spec.acoesRapidas().forEach(acao -> addAction(actions, seen,
                action("PAINEL_" + acao.toUpperCase(Locale.ROOT), humanize(acao),
                        "Ação rápida do painel institucional conectada ao fluxo do processo.",
                        resolveActionColor(acao), false, false, true,
                        List.of(), List.of(), List.of("Derivada do blueprint do painel " + spec.codigo() + ".")))));
        return actions;
    }

    private void addCommonCapacityActions(List<InstitutionalProcessActionSpec> out,
                                          Set<String> seen,
                                          Set<CapacidadeCaixaInstitucional> capacities,
                                          InstitutionalProcessWorkspaceSnapshot snapshot) {
        if (capacities == null || capacities.isEmpty()) {
            return;
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO)) {
            addAction(out, seen, action("RECEBER_EXPEDIENTE", "Receber expediente", "Formaliza o recebimento institucional do processo e da comunicação.", BLUE, false, false, false,
                    List.of("CONHECIMENTO", "RECURSAL", "EXECUCAO"), List.of(), List.of("Ato inicial de entrada na caixa institucional.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.DAR_CIENCIA)) {
            addAction(out, seen, action("DAR_CIENCIA", "Dar ciência", "Marca a ciência institucional válida e inicia a contagem interna de prazo.", BLUE, false, false, true,
                    List.of(), List.of(), List.of("Ciência é pessoalmente auditável, ainda que represente o órgão.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.PREPARAR_MINUTA)) {
            addAction(out, seen, action("PREPARAR_MINUTA", "Preparar minuta", "Produz minuta de parecer, manifestação, ofício ou resposta no contexto do órgão.", VIOLET, false, false, true,
                    List.of("CONHECIMENTO", "RECURSAL"), List.of(), List.of("A minuta não substitui assinatura do titular quando exigida.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)) {
            addAction(out, seen, action("ASSINAR_MANIFESTACAO", "Assinar manifestação", "Assina parecer, promoção, petição ou resposta institucional no processo.", VIOLET, true, false, true,
                    List.of(), List.of(), List.of("Assinatura exige trilha forte e, quando cabível, certificado.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO)) {
            addAction(out, seen, action("PETICIONAR_ORGAO", "Peticionar em nome do órgão", "Junta manifestação, resposta, requerimento ou incidente processual representando o órgão.", FUCHSIA, true, false, true,
                    List.of("CONHECIMENTO", "RECURSAL", "EXECUCAO"), List.of(), List.of("Somente perfil com legitimação institucional ativa pode peticionar.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE)) {
            addAction(out, seen, action("REDISTRIBUIR_INTERNO", "Redistribuir internamente", "Move o processo entre caixas, unidades ou filas internas autorizadas.", SLATE, false, false, true,
                    List.of(), List.of(), List.of("Redistribuição é rastreada com fundamento e usuário responsável.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO)) {
            addAction(out, seen, action("ATRIBUIR_MEMBRO", "Atribuir membro responsável", "Direciona o processo ao titular, substituto, plantonista ou assessor autorizado.", SLATE, false, true, true,
                    List.of(), List.of(), List.of("Mantém fila clara entre triagem, assessoria e titular.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA)) {
            addAction(out, seen, action("GERAR_CERTIDAO", "Gerar certidão operacional", "Emite certidão de ciência, custódia, apresentação ou cumprimento material.", SKY, true, false, true,
                    List.of("EXECUCAO", "AUDIENCIA_CUSTODIA"), List.of(), List.of("Certidão serve de prova de execução material ou ciência institucional.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS)) {
            addAction(out, seen, action("ORGANIZAR_DOCUMENTOS", "Organizar documentos", "Organiza dossiê, anexos, índice documental e pauta preparatória do processo.", BLUE, false, false, true,
                    List.of(), List.of(), List.of("Organização documental não se converte em poder decisório ou de pauta autônoma.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA)) {
            addAction(out, seen, action("SOLICITAR_AUDIENCIA", "Solicitar audiência", "Formula pedido rastreável de audiência, sessão ou janela de pauta no fluxo do processo.", EMERALD, false, false, true,
                    List.of("CONHECIMENTO", "RECURSAL", "EXECUCAO", "AUDIENCIA_CUSTODIA"), List.of(), List.of("Pedido de audiência não substitui a designação privativa do magistrado.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA)) {
            addAction(out, seen, action("AGENDAR_AUDIENCIA", "Operar pauta de audiência", "Opera slots, proposta de pauta, reserva de agenda e designação operacional submetida à chancela judicial quando exigida.", EMERALD, false, true, true,
                    List.of("CONHECIMENTO", "RECURSAL", "EXECUCAO", "AUDIENCIA_CUSTODIA"), List.of(), List.of("Operação de pauta exige trilha forte e não substitui ato privativo do magistrado.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA)) {
            addAction(out, seen, action("REMARCAR_AUDIENCIA", "Remarcar audiência", "Reorganiza a pauta, propõe nova janela e registra o motivo da remarcação.", EMERALD, false, true, true,
                    List.of(), List.of(), List.of("Remarcação precisa preservar motivo, trilha de autoria e ciência das partes.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA)) {
            addAction(out, seen, action("CANCELAR_AUDIENCIA", "Cancelar pauta", "Registra cancelamento operacional da pauta quando autorizado pelo fluxo competente.", RED, false, true, true,
                    List.of(), List.of(), List.of("Cancelamento de pauta sem autorização adequada é vedado.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA)) {
            addAction(out, seen, action("RESERVAR_SALA_AUDIENCIA", "Reservar sala ou sala virtual", "Reserva sala física, link institucional, conciliador ou infraestrutura de audiência.", EMERALD, false, false, true,
                    List.of(), List.of(), List.of("Reserva de sala integra a governança de pauta do PJB.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA)) {
            addAction(out, seen, action("REGISTRAR_TERMO_AUDIENCIA", "Registrar termo de audiência", "Lança ata, presença, ausência, acordo ou termo operacional da audiência.", EMERALD, true, false, true,
                    List.of(), List.of(), List.of("Ata e termo ficam em trilha própria e não substituem decisão de mérito.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.EMITIR_PARECER)) {
            addAction(out, seen, action("EMITIR_PARECER", "Emitir parecer institucional", "Produz parecer, manifestação ou opinião técnica/institucional dentro da competência do perfil.", VIOLET, true, false, true,
                    List.of("CONHECIMENTO", "RECURSAL"), List.of(), List.of("Parecer segue segregação de função conforme o papel institucional.")));
        }
        if (capacities.contains(CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL)) {
            addAction(out, seen, action("ABRIR_CALCULADORA_JUDICIAL", "Abrir calculadora judicial", "Acessa a calculadora judicial do PJB para liquidação, memória, atualização e apoio técnico de cálculo.", SKY, false, false, true,
                    List.of("EXECUCAO", "LIQUIDACAO", "CONHECIMENTO"), List.of(), List.of("Calculadora permanece auditável e integrada ao processo.")));
        }
        if (snapshot.embargos() && capacities.contains(CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO)) {
            addAction(out, seen, action("ATUAR_EMBARGOS", "Atuar em embargos", "Prepara e protocoliza a resposta ou o próprio embargo conforme o contexto do processo.", AMBER, true, false, true,
                    List.of("RECURSAL"), List.of(), List.of("Embargos ficam em trilha própria no workspace.")));
        }
    }

    private void addProfileActions(List<InstitutionalProcessActionSpec> out,
                                   Set<String> seen,
                                   InstitutionalAccessProfileCatalogEntry entry,
                                   InstitutionalProcessWorkspaceSnapshot snapshot) {
        switch (entry.processProfile()) {
            case PROMOTOR -> {
                addAction(out, seen, action("EMITIR_PARECER_MP", "Emitir parecer", "Redige e assina parecer ministerial nos autos.", VIOLET, true, false, true,
                        List.of("CONHECIMENTO", "RECURSAL"), List.of(), List.of("Promotor atua como titular da manifestação institucional.")));
                addAction(out, seen, action("PROMOVER_DILIGENCIA", "Promover diligência", "Requisita diligência, complementação probatória ou retorno de órgão técnico.", BLUE, false, false, true,
                        List.of("CONHECIMENTO"), List.of(), List.of("Conecta MP, polícia e apoio técnico.")));
                addAction(out, seen, action("RECORRER_MP", "Interpor ou responder recurso", "Atua recursalmente em nome do Ministério Público.", FUCHSIA, true, false, true,
                        List.of("RECURSAL"), List.of(), List.of("Recursal separado do parecer ordinário.")));
            }
            case DEFENSOR -> {
                addAction(out, seen, action("APRESENTAR_DEFESA", "Apresentar defesa", "Produz resposta, contestação, defesa técnica ou pedido urgente em favor do assistido.", VIOLET, true, false, true,
                        List.of("CONHECIMENTO", "RECURSAL"), List.of(), List.of("Defensor atua na caixa do núcleo com assinatura própria.")));
                addAction(out, seen, action("RECURSO_DEFENSIVO", "Interpor recurso defensivo", "Interposição de recurso, contrarrazões e medidas integrativas em favor da parte assistida.", FUCHSIA, true, false, true,
                        List.of("RECURSAL"), List.of(), List.of("Recurso e embargos em fila própria da defensoria.")));
                addAction(out, seen, action("PETICAO_URGENTE_ASSISTIDO", "Petição urgente do assistido", "Liberdade, saúde, tutela ou cumprimento prioritário do assistido.", RED, true, false, true,
                        List.of(), List.of(), List.of("Urgências do assistido não se misturam com a fila ordinária.")));
            }
            case PROCURADOR -> {
                addAction(out, seen, action("APRESENTAR_INFORMACOES", "Apresentar informações", "Contesta, informa, defende e responde em nome da Fazenda ou ente público.", VIOLET, true, false, true,
                        List.of("CONHECIMENTO"), List.of(), List.of("Fluxo fazendário e fazenda pública em trilha própria.")));
                addAction(out, seen, action("NEGOCIAR_ACORDO_PUBLICO", "Negociar acordo público", "Registra tratativas, manifestação de acordo ou composição autorizada pelo ente.", EMERALD, true, true, true,
                        List.of(), List.of(), List.of("Acordo público exige governança e autorização interna.")));
                if (snapshot.rito() != null && snapshot.rito().isExecucaoFiscalEstrita()) {
                    addAction(out, seen, action("EMBARGOS_EXECUCAO_FISCAL", "Atuar em embargos à execução fiscal", "Prepara defesa, impugnação e manifestações específicas da execução fiscal.", AMBER, true, false, true,
                            List.of("EXECUCAO"), List.of("EXECUCAO_FISCAL", "TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL"), List.of("Execução fiscal recebe separador e ações específicas.")));
                }
            }
            case DELEGADO, GESTOR_DELEGACIA -> {
                addAction(out, seen, action("RESPONDER_OFICIO", "Responder ofício judicial", "Responde requisição, diligência, cumprimento e remessa de peças pela autoridade policial.", BLUE, true, false, true,
                        List.of("CONHECIMENTO"), List.of(), List.of("Delegacia entra por caixa própria, não por conta compartilhada.")));
                addAction(out, seen, action("REGISTRAR_DILIGENCIA", "Registrar diligência", "Marca diligências realizadas, diligências pendentes e retorno de campo.", SKY, false, false, true,
                        List.of("CONHECIMENTO"), List.of(), List.of("Diligência policial fica auditável e separada por fila.")));
            }
            case POLICIAL_PENAL, GESTOR_UNIDADE_PRISIONAL, OPERADOR_CUSTODIA_PRISIONAL -> {
                addAction(out, seen, action("CONFIRMAR_CUSTODIA", "Confirmar custódia", "Confirma a custódia, apresentação, escolta, transferência ou cumprimento do alvará.", RED, true, false, true,
                        List.of("AUDIENCIA_CUSTODIA", "EXECUCAO"), List.of("EXECUCAO_PENAL"), List.of("Fluxo prisional com assinatura material e certidão operacional.")));
                addAction(out, seen, action("REGISTRAR_APRESENTACAO", "Registrar apresentação", "Registra a apresentação do preso e a execução material da ordem judicial.", SKY, false, false, true,
                        List.of("AUDIENCIA_CUSTODIA"), List.of(), List.of("Apresentação e escolta em trilha própria.")));
            }
            case PERITO_JUDICIAL, PSICOLOGO_JUDICIAL, ASSISTENTE_SOCIAL_JUDICIAL, CONTADOR_JUDICIAL, ORGAO_TECNICO_CONVENIADO -> {
                addAction(out, seen, action("SUBMETER_LAUDO_ESTUDO", "Submeter laudo ou estudo", "Junta laudo, estudo psicossocial, cálculo, relatório médico ou parecer técnico no processo.", EMERALD, true, false, true,
                        List.of("PERICIA_TECNICA", "LIQUIDACAO", "EXECUCAO"), List.of(), List.of("Apoio técnico possui fila própria de entrega, resposta e complementação.")));
                addAction(out, seen, action("SOLICITAR_COMPLEMENTACAO", "Solicitar complementação documental", "Pede documentos, exames, prontuários ou informações indispensáveis para a conclusão técnica.", EMERALD, false, false, true,
                        List.of("PERICIA_TECNICA"), List.of(), List.of("Complementações preservam integridade do laudo, estudo ou manifestação conveniada.")));
            }
            case CONCILIADOR, MEDIADOR, AGENDADOR_AUDIENCIA, AGENDADOR_CONCILIACAO -> {
                addAction(out, seen, action("AGENDAR_SESSAO", "Agendar sessão", "Reserva pauta, conciliador, sala física ou link virtual no processo.", EMERALD, false, false, true,
                        List.of("CONHECIMENTO"), List.of("MEDIACAO", "CONCILIACAO_EXTRAJUDICIAL"), List.of("Painel de pauta e sessões conectado ao processo.")));
                addAction(out, seen, action("REGISTRAR_TERMO", "Registrar termo de sessão", "Lança termo de audiência, conciliação, comparecimento ou ausência.", EMERALD, true, false, true,
                        List.of(), List.of(), List.of("Sessões não se confundem com decisão de mérito.")));
            }
            case ASSESSOR_INSTITUCIONAL, ANALISTA_INSTITUCIONAL, TECNICO_INSTITUCIONAL -> {
                addAction(out, seen, action("ANALISE_PREVIA", "Fazer análise prévia", "Classifica rito, recurso, embargos, urgência e sugere encaminhamento interno.", BLUE, false, false, true,
                        List.of(), List.of(), List.of("Apoio de gabinete e núcleo não substitui assinatura final.")));
                addAction(out, seen, action("MINUTA_AVANCADA", "Preparar minuta avançada", "Produz minuta estruturada com separação por rito, recurso e embargos.", VIOLET, false, false, true,
                        List.of("CONHECIMENTO", "RECURSAL", "EXECUCAO"), List.of(), List.of("Conexão direta com separadores processuais do workspace.")));
            }
            case SERVIDOR_TRIAGEM -> {
                addAction(out, seen, action("CLASSIFICAR_FLUXO", "Classificar fluxo processual", "Classifica o processo por rito, recurso, embargos, urgência e caixa interna.", BLUE, false, false, true,
                        List.of(), List.of(), List.of("Triagem abre o fluxo correto antes de titular e assessoria.")));
                addAction(out, seen, action("ESCALAR_TITULAR", "Escalar ao titular", "Encaminha ao membro titular quando a triagem detectar ato sensível, prazo crítico ou urgência.", RED, false, false, true,
                        List.of(), List.of(), List.of("Gatilho de escala evita atraso em vista obrigatória e urgências.")));
            }
            case COORDENADOR_UNIDADE, ADMINISTRADOR_INSTITUCIONAL, DIRETOR_FORUM, SECRETARIA_FORUM -> {
                addAction(out, seen, action("GERIR_LOTACAO_CAIXA", "Gerir lotação e caixas", "Altera lotação, prioridade, substituição, plantão e cobertura da unidade no processo institucional.", SLATE, false, true, true,
                        List.of(), List.of(), List.of("Gestão conecta processo, unidade, caixa e trilha institucional.")));
                addAction(out, seen, action("HOMOLOGAR_GUARDA_PROCESSUAL", "Homologar guarda processual", "Homologa delegação, substituição ou autorização sensível ligada ao processo.", SLATE, true, true, true,
                        List.of(), List.of(), List.of("Atos sensíveis de governança não ficam misturados com petições comuns.")));
            }
            case CARTORIO_EXTRAJUDICIAL, COOPERACAO_JUDICIAL, MAGISTRADO_COOPERANTE, PERFIL_HIBRIDO -> {
                if (entry.codigo().contains("OAB_SECCIONAL")) {
                    addAction(out, seen, action("GOVERNANCA_OAB", "Governança institucional da OAB", "Opera fluxo institucional seccional, valida convênios e acompanha integração com o PJB.", SLATE, true, true, true,
                            List.of(), List.of(), List.of("Presidência seccional não substitui a advocacia privada do caso.")));
                }
            }
        }
    }

    private List<String> buildRecursos(InstitutionalAccessProfileCatalogEntry entry, InstitutionalProcessWorkspaceSnapshot snapshot) {
        if (!allowsPetitioning(entry) && entry.processProfile() != InstitutionalProcessProfile.PROMOTOR && entry.processProfile() != InstitutionalProcessProfile.DEFENSOR) {
            return List.of();
        }
        LinkedHashSet<String> recursos = new LinkedHashSet<>();
        recursos.add("apelacao");
        recursos.add("agravo_de_instrumento");
        recursos.add("agravo_interno");
        recursos.add("contrarrazoes");
        recursos.add("recurso_ordinario");
        if (snapshot.rito() != null && snapshot.rito().isTrabalhista()) {
            recursos.add("recurso_ordinario_trabalhista");
            recursos.add("agravo_de_peticao");
        }
        if (snapshot.rito() != null && snapshot.rito().isEleitoral()) {
            recursos.add("recurso_eleitoral");
        }
        if (snapshot.rito() != null && snapshot.rito().isMilitar()) {
            recursos.add("recurso_militar");
        }
        if (snapshot.status() != null && snapshot.status().isPosDecisao()) {
            recursos.add("pedido_de_efeito_suspensivo");
        }
        return List.copyOf(recursos);
    }

    private List<String> buildEmbargos(InstitutionalAccessProfileCatalogEntry entry, InstitutionalProcessWorkspaceSnapshot snapshot) {
        if (!allowsPetitioning(entry) && !hasAnyCapacity(entry.capacidadesPadrao(), CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)) {
            return List.of();
        }
        LinkedHashSet<String> embargos = new LinkedHashSet<>();
        embargos.add("embargos_declaracao");
        if (snapshot.rito() != null && snapshot.rito().isExecucaoFiscalEstrita()) {
            embargos.add("embargos_execucao_fiscal");
        }
        if (snapshot.execucao()) {
            embargos.add("impugnacao_cumprimento_sentenca");
        }
        return List.copyOf(embargos);
    }

    private String resolveAccentColor(InstitutionalAccessProfileCatalogEntry entry, InstitutionalProcessWorkspaceSnapshot snapshot) {
        if (snapshot.urgente()) {
            return RED;
        }
        if (snapshot.embargos()) {
            return AMBER;
        }
        if (snapshot.recursal()) {
            return FUCHSIA;
        }
        if (snapshot.execucao()) {
            return SKY;
        }
        return switch (entry.processProfile()) {
            case PROMOTOR, DEFENSOR, PROCURADOR -> VIOLET;
            case DELEGADO, GESTOR_DELEGACIA, POLICIAL_PENAL, GESTOR_UNIDADE_PRISIONAL, OPERADOR_CUSTODIA_PRISIONAL -> RED;
            case PERITO_JUDICIAL, PSICOLOGO_JUDICIAL, ASSISTENTE_SOCIAL_JUDICIAL, CONTADOR_JUDICIAL, ORGAO_TECNICO_CONVENIADO,
                    CONCILIADOR, MEDIADOR, AGENDADOR_AUDIENCIA, AGENDADOR_CONCILIACAO -> EMERALD;
            case DIRETOR_FORUM, SECRETARIA_FORUM, COORDENADOR_UNIDADE, ADMINISTRADOR_INSTITUCIONAL -> SLATE;
            default -> BLUE;
        };
    }

    private boolean supportsHearingSurface(InstitutionalAccessProfileCatalogEntry entry) {
        return entry != null && (hasAnyCapacity(entry.capacidadesPadrao(),
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA)
                || entry.processProfile() == InstitutionalProcessProfile.AGENDADOR_AUDIENCIA
                || entry.processProfile() == InstitutionalProcessProfile.AGENDADOR_CONCILIACAO
                || entry.processProfile() == InstitutionalProcessProfile.SECRETARIA_FORUM
                || entry.processProfile() == InstitutionalProcessProfile.DIRETOR_FORUM
                || entry.processProfile() == InstitutionalProcessProfile.PROMOTOR
                || entry.processProfile() == InstitutionalProcessProfile.DEFENSOR
                || entry.processProfile() == InstitutionalProcessProfile.PROCURADOR);
    }

    private boolean supportsOpinionFlow(InstitutionalAccessProfileCatalogEntry entry) {
        return entry != null && (hasAnyCapacity(entry.capacidadesPadrao(),
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO,
                CapacidadeCaixaInstitucional.EMITIR_PARECER)
                || entry.processProfile() == InstitutionalProcessProfile.PROMOTOR
                || entry.processProfile() == InstitutionalProcessProfile.DEFENSOR
                || entry.processProfile() == InstitutionalProcessProfile.PROCURADOR
                || entry.processProfile() == InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL
                || entry.processProfile() == InstitutionalProcessProfile.ANALISTA_INSTITUCIONAL);
    }

    private boolean supportsCalculatorSurface(InstitutionalAccessProfileCatalogEntry entry) {
        return entry != null && (hasAnyCapacity(entry.capacidadesPadrao(), CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL)
                || entry.processProfile() == InstitutionalProcessProfile.CONTADOR_JUDICIAL
                || entry.processProfile() == InstitutionalProcessProfile.SECRETARIA_FORUM
                || entry.processProfile() == InstitutionalProcessProfile.PROMOTOR
                || entry.processProfile() == InstitutionalProcessProfile.DEFENSOR
                || entry.processProfile() == InstitutionalProcessProfile.PROCURADOR);
    }

    private boolean allowsPetitioning(InstitutionalAccessProfileCatalogEntry entry) {
        return hasAnyCapacity(entry.capacidadesPadrao(), CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)
                || entry.processProfile() == InstitutionalProcessProfile.PROMOTOR
                || entry.processProfile() == InstitutionalProcessProfile.DEFENSOR
                || entry.processProfile() == InstitutionalProcessProfile.PROCURADOR;
    }

    private boolean isTechnical(InstitutionalProcessProfile profile) {
        return profile == InstitutionalProcessProfile.PERITO_JUDICIAL
                || profile == InstitutionalProcessProfile.PSICOLOGO_JUDICIAL
                || profile == InstitutionalProcessProfile.ASSISTENTE_SOCIAL_JUDICIAL
                || profile == InstitutionalProcessProfile.CONTADOR_JUDICIAL
                || profile == InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO;
    }

    private String extractScopePrefix(String profileCode) {
        if (profileCode == null || profileCode.isBlank()) {
            return null;
        }
        int separator = profileCode.indexOf("__");
        if (separator > 0) {
            return profileCode.substring(0, separator);
        }
        if (profileCode.startsWith("OAB_SECCIONAL")) {
            return "OAB_SECCIONAL";
        }
        return null;
    }

    private boolean hasAnyCapacity(Collection<CapacidadeCaixaInstitucional> capacities, CapacidadeCaixaInstitucional... desired) {
        if (capacities == null || capacities.isEmpty()) {
            return false;
        }
        EnumSet<CapacidadeCaixaInstitucional> current = EnumSet.copyOf(capacities);
        for (CapacidadeCaixaInstitucional item : desired) {
            if (current.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private void addAction(List<InstitutionalProcessActionSpec> out, Set<String> seen, InstitutionalProcessActionSpec action) {
        if (seen.add(action.code())) {
            out.add(action);
        }
    }

    private InstitutionalProcessQueueSectionSpec section(String code,
                                                         String title,
                                                         String accentColor,
                                                         int order,
                                                         List<String> filtros,
                                                         List<String> indicadores,
                                                         List<String> ordenacoes) {
        return new InstitutionalProcessQueueSectionSpec(code, title, accentColor, order, filtros, indicadores, ordenacoes);
    }

    private InstitutionalProcessVisualLaneSpec lane(String code,
                                                    String title,
                                                    String accentColor,
                                                    int order,
                                                    boolean active,
                                                    List<String> filtros,
                                                    List<String> etiquetas,
                                                    List<String> fundamentos) {
        return new InstitutionalProcessVisualLaneSpec(code, title, accentColor, order, active, filtros, etiquetas, fundamentos);
    }

    private InstitutionalProcessActionSpec action(String code,
                                                  String title,
                                                  String description,
                                                  String accentColor,
                                                  boolean requiresCertificate,
                                                  boolean requiresTitularApproval,
                                                  boolean modifiesFlow,
                                                  List<String> fases,
                                                  List<String> ritos,
                                                  List<String> fundamentos) {
        return new InstitutionalProcessActionSpec(code, title, description, accentColor, requiresCertificate, requiresTitularApproval, modifiesFlow, fases, ritos, fundamentos);
    }

    private String resolveActionColor(String code) {
        String normalized = normalize(code);
        if (normalized.contains("CERT") || normalized.contains("ASSIN") || normalized.contains("PARECER") || normalized.contains("MINUTA")) {
            return VIOLET;
        }
        if (normalized.contains("URG") || normalized.contains("CUSTODIA") || normalized.contains("PLANTAO")) {
            return RED;
        }
        if (normalized.contains("RECURSO") || normalized.contains("EMBARGO")) {
            return FUCHSIA;
        }
        if (normalized.contains("AUDIENCIA") || normalized.contains("CONCILIACAO") || normalized.contains("ACORDO")) {
            return EMERALD;
        }
        if (normalized.contains("LOTA") || normalized.contains("DELEGA") || normalized.contains("SUBSTIT")) {
            return SLATE;
        }
        return BLUE;
    }

    private String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Ação institucional";
        }
        String[] tokens = raw.toLowerCase(Locale.ROOT).split("[_\\-]+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
        }
        return sb.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
