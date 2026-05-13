package com.tcc.pjb.backend.service.processual.recursal.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalAutomationWorkspaceServiceTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
    private final RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);

    @Test
    void deveGerarTrilhaGuiadaDeApelacaoAdesiva() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of()
        ));

        assertThat(response.rotaPrioritaria()).isEqualTo("APELACAO");
        assertThat(response.nomenclaturaAtiva()).isEqualTo("APELANTE/APELADO");
        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains(
                        "ROTA_PRIORITARIA_GUIADA",
                        "TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL",
                        "COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA",
                        "MALHA_PAINEIS_WORKBENCHES_COMPETENTES",
                        "PAINEL_CIDADAO_RECURSAL_PROPRIO",
                        "PAINEL_RECURSAL_PARTES_REPRESENTANTES",
                        "PAINEL_EXTERNO_OPERACIONAL_RECURSAL",
                        "PAINEL_ADVOGADO_RECURSAL_COMPLETO",
                        "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS",
                        "HABILITACAO_ASSOCIACAO_RECURSAL_GOVERNADA",
                        "ESCRITORIO_ASSISTENTES_SUBSTABELECIMENTO_RECURSAL",
                        "CERTIDOES_EXTERNAS_RECURSAIS",
                        "ORGANIZACAO_INSTITUCIONAL_RECURSAL",
                        "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL",
                        "COLABORACAO_MULTIMIDIA_DOCUMENTAL_RECURSAL",
                        "OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL",
                        "ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS",
                        "ESCALONAMENTO_ALERTAS_POR_PERFIL", "POS_JULGAMENTO_RECURSAL_ESCALONADO", "ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS",
                        "REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL",
                        "MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL",
                        "DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO",
                        "FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA", "COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL",
                        "SHELL_CONTEXTUAL_TATICO_DO_RITO",
                        "SHELL_CONTEXTUAL_POR_ATOR_PERFIL",
                        "MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO",
                        "MALOTES_PETICIONAMENTO_ATOS_RECURSAIS",
                        "SECRETARIA_MULTIGRAU_REFORCADA",
                        "MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU",
                        "CHECKLIST_FORMAL_POR_PECA",
                        "APELACAO_GUIADA",
                        "EFEITOS_RECURSAIS_GUIADOS",
                        "JUIZO_RETRATACAO_POTENCIAL",
                        "CONTRARRAZOES_RECURSAIS_GUIADAS",
                        "APELACAO_ADESIVA_GUIADA",
                        "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL"
                );
    }

    @Test
    void deveGerarTrilhasGuiadasDeAgravoInternoEAgravoExcepcional() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "DECISAO_MONOCRATICA",
                "INVALIDAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                Set.of()
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL", "COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA", "MALHA_PAINEIS_WORKBENCHES_COMPETENTES", "PAINEL_CIDADAO_RECURSAL_PROPRIO", "PAINEL_RECURSAL_PARTES_REPRESENTANTES", "PAINEL_EXTERNO_OPERACIONAL_RECURSAL", "PAINEL_ADVOGADO_RECURSAL_COMPLETO", "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS", "HABILITACAO_ASSOCIACAO_RECURSAL_GOVERNADA", "ESCRITORIO_ASSISTENTES_SUBSTABELECIMENTO_RECURSAL", "CERTIDOES_EXTERNAS_RECURSAIS", "ORGANIZACAO_INSTITUCIONAL_RECURSAL", "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL", "COLABORACAO_MULTIMIDIA_DOCUMENTAL_RECURSAL", "OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL", "ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS", "ESCALONAMENTO_ALERTAS_POR_PERFIL", "POS_JULGAMENTO_RECURSAL_ESCALONADO", "ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS", "REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL", "MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL", "DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO", "FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA", "COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL", "SHELL_CONTEXTUAL_TATICO_DO_RITO", "SHELL_CONTEXTUAL_POR_ATOR_PERFIL", "MALOTES_PETICIONAMENTO_ATOS_RECURSAIS", "SECRETARIA_MULTIGRAU_REFORCADA", "MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU", "CHECKLIST_FORMAL_POR_PECA", "AGRAVO_INTERNO_GUIADO", "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL");

        RecursalAutomationWorkspaceResponse agravoExcepcional = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                Set.of()
        ));

        assertThat(agravoExcepcional.rotaPrioritaria()).isEqualTo("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO");
        assertThat(agravoExcepcional.trilhas()).extracting(track -> track.codigo())
                .contains("TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL", "COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA", "MALHA_PAINEIS_WORKBENCHES_COMPETENTES", "PAINEL_CIDADAO_RECURSAL_PROPRIO", "PAINEL_RECURSAL_PARTES_REPRESENTANTES", "PAINEL_EXTERNO_OPERACIONAL_RECURSAL", "PAINEL_ADVOGADO_RECURSAL_COMPLETO", "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS", "HABILITACAO_ASSOCIACAO_RECURSAL_GOVERNADA", "ESCRITORIO_ASSISTENTES_SUBSTABELECIMENTO_RECURSAL", "CERTIDOES_EXTERNAS_RECURSAIS", "ORGANIZACAO_INSTITUCIONAL_RECURSAL", "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL", "COLABORACAO_MULTIMIDIA_DOCUMENTAL_RECURSAL", "OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL", "ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS", "ESCALONAMENTO_ALERTAS_POR_PERFIL", "POS_JULGAMENTO_RECURSAL_ESCALONADO", "ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS", "REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL", "MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL", "DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO", "FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA", "COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL", "SHELL_CONTEXTUAL_TATICO_DO_RITO", "SHELL_CONTEXTUAL_POR_ATOR_PERFIL", "MALOTES_PETICIONAMENTO_ATOS_RECURSAIS", "SECRETARIA_MULTIGRAU_REFORCADA", "MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU", "AGRAVO_RECURSO_EXCEPCIONAL_GUIADO", "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL");
    }

    @Test
    void deveGerarTrilhasGuiadasDeEmbargosDeclaracaoEEmbargosDeDivergencia() {
        RecursalAutomationWorkspaceResponse embargosDeclaracao = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "INTEGRAR_CORRIGIR",
                false,
                false,
                false,
                true,
                false,
                false,
                true,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of("OMISSAO", "CONTRADICAO")
        ));

        assertThat(embargosDeclaracao.rotaPrioritaria()).isEqualTo("EMBARGOS_DECLARACAO");
        assertThat(embargosDeclaracao.trilhas()).extracting(track -> track.codigo())
                .contains("EMBARGOS_DECLARACAO_GUIADO");

        RecursalAutomationWorkspaceResponse divergenceResponse = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                Set.of()
        ));

        assertThat(divergenceResponse.trilhas()).extracting(track -> track.codigo())
                .contains("TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL", "COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA", "MALHA_PAINEIS_WORKBENCHES_COMPETENTES", "PAINEL_CIDADAO_RECURSAL_PROPRIO", "PAINEL_RECURSAL_PARTES_REPRESENTANTES", "PAINEL_EXTERNO_OPERACIONAL_RECURSAL", "PAINEL_ADVOGADO_RECURSAL_COMPLETO", "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS", "HABILITACAO_ASSOCIACAO_RECURSAL_GOVERNADA", "ESCRITORIO_ASSISTENTES_SUBSTABELECIMENTO_RECURSAL", "CERTIDOES_EXTERNAS_RECURSAIS", "ORGANIZACAO_INSTITUCIONAL_RECURSAL", "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL", "COLABORACAO_MULTIMIDIA_DOCUMENTAL_RECURSAL", "OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL", "ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS", "ESCALONAMENTO_ALERTAS_POR_PERFIL", "POS_JULGAMENTO_RECURSAL_ESCALONADO", "ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS", "REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL", "MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL", "DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO", "FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA", "COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL", "SHELL_CONTEXTUAL_TATICO_DO_RITO", "SHELL_CONTEXTUAL_POR_ATOR_PERFIL", "MALOTES_PETICIONAMENTO_ATOS_RECURSAIS", "SECRETARIA_MULTIGRAU_REFORCADA", "MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU", "CHECKLIST_FORMAL_POR_PECA", "EMBARGOS_DIVERGENCIA_GUIADO", "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL");
        assertThat(divergenceResponse.rotaPrioritaria()).isEqualTo("EMBARGOS_DIVERGENCIA");
        assertThat(divergenceResponse.nomenclaturaAtiva()).isEqualTo("EMBARGANTE/EMBARGADO");
    }

    @Test
    void deveAbrirAgravoDeInstrumentoGuiadoComControleDeInstrumento() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "DECISAO_INTERLOCUTORIA",
                "REFORMAR",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of()
        ));

        assertThat(response.rotaPrioritaria()).isEqualTo("AGRAVO_DE_INSTRUMENTO");
        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL", "COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA", "MALHA_PAINEIS_WORKBENCHES_COMPETENTES", "PAINEL_CIDADAO_RECURSAL_PROPRIO", "PAINEL_RECURSAL_PARTES_REPRESENTANTES", "PAINEL_EXTERNO_OPERACIONAL_RECURSAL", "PAINEL_ADVOGADO_RECURSAL_COMPLETO", "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS", "HABILITACAO_ASSOCIACAO_RECURSAL_GOVERNADA", "ESCRITORIO_ASSISTENTES_SUBSTABELECIMENTO_RECURSAL", "CERTIDOES_EXTERNAS_RECURSAIS", "ORGANIZACAO_INSTITUCIONAL_RECURSAL", "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL", "COLABORACAO_MULTIMIDIA_DOCUMENTAL_RECURSAL", "OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL", "ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS", "ESCALONAMENTO_ALERTAS_POR_PERFIL", "POS_JULGAMENTO_RECURSAL_ESCALONADO", "ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS", "REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL", "MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL", "DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO", "FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA", "COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL", "SHELL_CONTEXTUAL_TATICO_DO_RITO", "SHELL_CONTEXTUAL_POR_ATOR_PERFIL", "MALOTES_PETICIONAMENTO_ATOS_RECURSAIS", "SECRETARIA_MULTIGRAU_REFORCADA", "MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU", "AGRAVO_DE_INSTRUMENTO_GUIADO", "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("AGRAVO_DE_INSTRUMENTO_GUIADO"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("peças do instrumento"));
    }

    @Test
    void deveDirecionarApelacaoParaPainelRecursalDistintoComConsultaNaOrigem() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                false
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("PAINEL_JURISDICIONAL_COMPETENTE", "MALHA_PAINEIS_WORKBENCHES_COMPETENTES");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("PAINEL_JURISDICIONAL_COMPETENTE"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("painel recursal do tribunal competente"));
    }

    @Test
    void deveReforcarSecretariaMultigrauNaRotaExcepcional() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "FEDERAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("SECRETARIA_MULTIGRAU_REFORCADA");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("SECRETARIA_MULTIGRAU_REFORCADA"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("presidência ou vice-presidência"));
    }

    @Test
    void deveManterEmbargosDeclaracaoNoMesmoOrgaoProlatorSemSubidaArtificial() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "INTEGRAR_CORRIGIR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of("OMISSAO"),
                "ESTADUAL",
                "CIVEL",
                false,
                false
        ));

        assertThat(response.rotaPrioritaria()).isEqualTo("EMBARGOS_DECLARACAO");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("PAINEL_JURISDICIONAL_COMPETENTE"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("permanece no mesmo órgão prolator"));
    }

    @Test
    void deveAbrirRecursoInominadoComTurmaRecursalPropria() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                true,
                false
        ));

        assertThat(response.rotaPrioritaria()).isEqualTo("RECURSO_INOMINADO");
        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("PAINEL_JURISDICIONAL_COMPETENTE", "RECURSO_INOMINADO_GUIADO", "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("PAINEL_JURISDICIONAL_COMPETENTE"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("turma recursal"));
    }

    @Test
    void deveAbrirTrilhaExcepcionalComFiltroDePresidenciaParaRecursoEspecial() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.rotaPrioritaria()).isEqualTo("RECURSO_ESPECIAL");
        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("RECURSO_ESPECIAL_GUIADO", "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("FILTRO_PRESIDENCIA_VICE", "SUBIDA_CORTE_SUPERIOR");
    }

    @Test
    void devePersistirMatrizDeCapacidadesAteUltimaInstanciaComSecretariaInstitucional() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "FEDERAL",
                "PENAL",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("REUSAR_SUPERFICIES_SECRETARIA_JUDICIAL", "REUSAR_SUPERFICIES_COLEGIADAS", "REUSAR_SUPERFICIES_SECRETARIA_INSTITUCIONAL", "PRESERVAR_ATE_ULTIMA_INSTANCIA", "ESPINHA_INSTITUCIONAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.descricao())
                .anyMatch(descricao -> descricao.contains("/api/v1/secretariat/queue/governance")
                        && descricao.contains("/api/v1/institutional-support/branchCode-federal/coverage")
                        && descricao.contains("/api/v1/secretariat/julgamentos/0/acordao"));
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("última instância"));
    }

    @Test
    void deveGerarTrilhaDeBloqueioQuandoPoderDeRecorrerEstiverExtinto() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                Set.of()
        ));

        assertThat(response.poderRecorrerBloqueado()).isTrue();
        assertThat(response.trilhas()).extracting(track -> track.codigo()).containsExactly("PODER_RECORRER_BLOQUEADO");
        assertThat(response.motivoBloqueioPoderRecorrer()).contains("desistência");
    }


    @Test
    void deveConectarMalhaRealDePaineisEWorkbenchesNaRotaExcepcional() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "FEDERAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("MALHA_PAINEIS_WORKBENCHES_COMPETENTES");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MALHA_PAINEIS_WORKBENCHES_COMPETENTES"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("/api/v1/magistratura/atos") || alerta.contains("/api/v1/institucional/workbench"));
    }


    @Test
    void deveAbrirEscadaDeVisibilidadeParaPartesRepresentacaoEJuizesNaSubidaDaApelacao() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                false
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("ESCADA_VISIBILIDADE_OPERACIONAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("ESCADA_VISIBILIDADE_OPERACIONAL"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("PUBLICAR_DEGRAU_PARTES", "PUBLICAR_DEGRAU_REPRESENTACAO", "PUBLICAR_DEGRAU_DEFENSORIA", "PUBLICAR_DEGRAU_MAGISTRATURA_ORIGEM", "PUBLICAR_DEGRAU_MAGISTRATURA_DESTINO", "TRAVAR_SIGILO_POR_DEGRAU");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("ESCADA_VISIBILIDADE_OPERACIONAL"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.descricao())
                .anyMatch(descricao -> descricao.contains("/api/v1/public/consultas-publicas/workspace")
                        && descricao.contains("/api/v1/ui/offices/workspace/processes/{processoId}/access")
                        && descricao.contains("/api/v1/ui/professional/workspace/defensoria-executive-dashboard"));
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("ESCADA_VISIBILIDADE_OPERACIONAL"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("degraus"));
    }



    @Test
    void deveAbrirPainelRecursalDePartesERepresentantesComFiltrosDedicados() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("PAINEL_RECURSAL_PARTES_REPRESENTANTES");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("PAINEL_RECURSAL_PARTES_REPRESENTANTES"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("autor/réu") || alerta.contains("advogado"));
    }


    @Test
    void deveAbrirPainelCidadaoSomenteParaProcessosPropriosComMovimentacaoECor() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                false
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("PAINEL_CIDADAO_RECURSAL_PROPRIO");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("PAINEL_CIDADAO_RECURSAL_PROPRIO"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("LIMITAR_PROCESSOS_PROPRIOS", "MOSTRAR_ULTIMAS_MOVIMENTACOES", "REUSAR_CORES_PROCESSUAIS_EXISTENTES");
    }



    @Test
    void deveAbrirMalhaRecursalPorRamoRitoESigiloNoPenal() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "PENAL",
                false,
                false
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("MALHA_RECURSAL_POR_RAMO_RITO_SIGILO");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MALHA_RECURSAL_POR_RAMO_RITO_SIGILO"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("FILTRAR_CIDADAO_POR_RAMO", "FILTRAR_REPRESENTACAO_POR_RAMO", "APLICAR_SIGILO_GRADUADO", "REUSAR_MOVIMENTACOES_E_CORES");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MALHA_RECURSAL_POR_RAMO_RITO_SIGILO"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("PENAL") && alerta.contains("sigilo"));
    }



    @Test
    void deveAbrirTrilhaDeAlertasDePrazoENotificacoesRecursais() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                false
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("feriado local não comprovado") || alerta.contains("janela de contrarrazões"));
    }


    @Test
    void deveAbrirTrilhaDeEscalonamentoDeAlertasPorPerfil() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "QUESTIONAR_VIOLACAO_CONSTITUCIONAL",
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                Set.of(),
                "ESTADUAL",
                "ELEITORAL",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("ESCALONAMENTO_ALERTAS_POR_PERFIL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("ESCALONAMENTO_ALERTAS_POR_PERFIL"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("criticidade") || alerta.contains("ELEITORAL"));
    }


    @Test
    void deveAbrirReusoInteligenteDoPeticionamentoSemCopiarPeticaoInicial() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "INTEGRAR_CORRIGIR",
                false,
                false,
                false,
                true,
                false,
                false,
                true,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of("OMISSAO"),
                "ESTADUAL",
                "CIVEL",
                false,
                false
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("petição de embargos"));
    }

    @Test
    void deveGerarMatrizNacionalDePeticionamentoPorRamoEEspecie() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "TRABALHISTA",
                "TRABALHISTA",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("trabalhista") || alerta.contains("TRABALHISTA"));
    }


    @Test
    void deveGerarMatrizConcretaDePecasPorAtorERito() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "PENAL",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("penal") || alerta.contains("PENAL"));
    }


    @Test
    void deveExporTrilhaDeDiferenciacaoTrabalhistaPorTribunalEPrazo() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "TRABALHO",
                "TRABALHISTA",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alert -> alert.contains("TRABALHISTA"));
    }

    @Test
    void deveComutarPainelProfissionalParaContextoTrabalhistaSemPerderShellPadrao() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "TRABALHISTA",
                "TRABALHISTA",
                false,
                false
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA", "COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("trabalhista") || alerta.contains("TRABALHISTA"));
    }

    @Test
    void deveProjetarShellContextualDoAtorNaDefensoriaPenal() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "PENAL",
                false,
                true,
                "DEFENSORIA"
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("SHELL_CONTEXTUAL_POR_ATOR_PERFIL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("SHELL_CONTEXTUAL_POR_ATOR_PERFIL"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("defensoria penal") || alerta.contains("habeas corpus"));
    }


    @Test
    void deveProjetarTrilhaDePoliticaVisualOperacionalParaSecretariaTrabalhista() {
        var response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "TRABALHISTA",
                "TRABALHISTA",
                false,
                true,
                "SECRETARIA"
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("MATRIZ_FINA_POLITICA_VISUAL_OPERACIONAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("MATRIZ_FINA_POLITICA_VISUAL_OPERACIONAL"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("TRABALHISTA") || alerta.contains("operação interna"));
    }



    @Test
    void deveExporTaxonomiaProcessualUnificadaNoWorkspaceRecursal() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("ALINHAR_CLASSE_CNJ", "ALINHAR_ASSUNTO_MAIS_ESPECIFICO", "ALINHAR_MOVIMENTACAO_REAL", "ALINHAR_TIPO_PETICAO_CNJ");
    }

    @Test
    void deveExporCompetenciaDistribuicaoEPaineisInstitucionaisNoWorkspaceRecursal() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "PENAL",
                true,
                false,
                "PROCURADORIA"
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains("COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA", "PAINEL_EXTERNO_OPERACIONAL_RECURSAL", "ORGANIZACAO_INSTITUCIONAL_RECURSAL");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA"))
                .findFirst()
                .orElseThrow()
                .alertasTaticos())
                .anyMatch(alerta -> alerta.contains("juizado") || alerta.contains("distribuição"));
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("ORGANIZACAO_INSTITUCIONAL_RECURSAL"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("MODELAR_PAPEIS_REPRESENTACAO", "ORGANIZAR_CAIXAS_E_FILTROS", "REUSAR_PROCURADORIA_E_DEFENSORIA", "SINCRONIZAR_PRE_PAUTA_E_COBERTURA");
    }



    @Test
    void deveExporPainelAdvogadoLoteCertidoesCaixasEWizardNoWorkspaceRecursal() {
        RecursalAutomationWorkspaceResponse response = workspaceService.buildWorkspace(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                true,
                "ADVOCACIA"
        ));

        assertThat(response.trilhas()).extracting(track -> track.codigo())
                .contains(
                        "WIZARD_DISTRIBUICAO_ASSISTIDA_IA",
                        "PAINEL_ADVOGADO_RECURSAL_COMPLETO",
                        "CERTIDOES_EXTERNAS_RECURSAIS",
                        "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL",
                        "PETICIONAMENTO_LOTE_ASSINATURA_RECURSAL"
                );
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("PAINEL_ADVOGADO_RECURSAL_COMPLETO"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("ABRIR_LOCALIZADORES_INTIMACAO_E_PRAZO", "SINCRONIZAR_AUDIENCIAS_RECURSOS_E_SESSOES", "PRESERVAR_AREA_TRABALHO_E_RELACAO_PROCESSOS", "FIXAR_ATALHOS_E_PAINEL_DETALHADO");
        assertThat(response.trilhas().stream()
                .filter(track -> track.codigo().equals("PETICIONAMENTO_LOTE_ASSINATURA_RECURSAL"))
                .findFirst()
                .orElseThrow()
                .checklistOperacional())
                .extracting(item -> item.codigo())
                .contains("SALVAR_DISTRIBUICAO_FUTURA", "DISTRIBUIR_EM_LOTE", "ASSINAR_EM_LOTE_COM_GOVERNANCA", "PREPARAR_PETICOES_INTERMEDIARIAS_EM_BLOCO");
    }

}
