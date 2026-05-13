package com.tcc.pjb.backend.service.processual.recursal.automation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationPlaybookResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalAutomationServiceTest {

    private final RecursalAutomationService service = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(service);

    @Test
    void deveRecomendarApelacaoEAdesivoQuandoCenarioForCompativel() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
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

        assertThat(response.candidatos()).extracting(candidate -> candidate.recurso()).containsExactly("APELACAO");
        assertThat(response.admiteRecursoAdesivo()).isTrue();
        assertThat(response.observacaoRecursoAdesivo()).contains("janela de contrarrazões ainda aberta");
        assertThat(response.sinais())
                .extracting(signal -> signal.codigo())
                .contains("APELACAO_ADMISSIBILIDADE_TRIBUNAL", "AUTOS_ELETRONICOS");
    }

    @Test
    void devePrivilegiarEmbargosQuandoHouverFundamentosDeIntegracao() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
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

        assertThat(response.candidatos().getFirst().recurso()).isEqualTo("EMBARGOS_DECLARACAO");
        assertThat(response.sinais())
                .extracting(signal -> signal.codigo())
                .contains("PREPARO_INSUFICIENTE", "FERIADO_LOCAL_NAO_COMPROVADO", "EMBARGOS_DECLARACAO_IDENTIFICADOS");
    }

    @Test
    void deveRecomendarRecursoInominadoQuandoSentencaVierDeJuizadoEspecial() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
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

        assertThat(response.candidatos()).extracting(candidate -> candidate.recurso()).containsExactly("RECURSO_INOMINADO");
        assertThat(response.candidatos().getFirst().prazoDiasUteis()).isEqualTo(10);
        assertThat(response.sinais()).extracting(signal -> signal.codigo()).contains("RECURSO_INOMINADO_TURMA_RECURSAL");
    }

    @Test
    void deveSinalizarFiltroDePresidenciaQuandoHouverRecursoExcepcional() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
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
                Set.of()
        ));

        assertThat(response.candidatos()).extracting(candidate -> candidate.recurso())
                .startsWith("RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO");
        assertThat(response.sinais()).extracting(signal -> signal.codigo())
                .contains("FILTRO_PRESIDENCIA_VICE_TRIBUNAL_RECORRIDO");
    }

    @Test
    void deveMarcarDespachoComoIrrecorrivel() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
                "DESPACHO",
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
                Set.of()
        ));

        assertThat(response.candidatos()).extracting(candidate -> candidate.recurso()).containsExactly("IRRECORRIVEL");
        assertThat(response.sinais()).extracting(signal -> signal.codigo()).contains("DESPACHO_IRRECORRIVEL");
        assertThat(response.admiteRecursoAdesivo()).isFalse();
    }

    @Test
    void devePriorizarAgravoExcepcionalEEmbargosDeDivergenciaQuandoCenarioExigir() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
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
                true,
                false,
                false,
                false,
                false,
                Set.of()
        ));

        assertThat(response.candidatos()).extracting(candidate -> candidate.recurso())
                .startsWith("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO", "EMBARGOS_DIVERGENCIA");
        assertThat(response.sinais()).extracting(signal -> signal.codigo())
                .contains("AGRAVO_RECURSO_EXCEPCIONAL_CABIVEL", "DIVERGENCIA_INTERNA_MAPEADA");
    }

    @Test
    void deveBloquearAdesivoQuandoContrarrazoesJaTiveremSidoProtocoladas() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
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
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of()
        ));

        assertThat(response.admiteRecursoAdesivo()).isFalse();
        assertThat(response.observacaoRecursoAdesivo()).contains("contrarrazões já protocoladas");
        assertThat(response.sinais()).extracting(signal -> signal.codigo()).contains("ADESIVO_PRAZO_CONSUMIDO");
    }

    @Test
    void deveGerarPlaybookVivoComRotaPrioritariaEPassosCriticos() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                false,
                Set.of()
        ));

        assertThat(response.rotaPrioritaria()).isEqualTo("AGRAVO_INTERNO");
        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("QUALIFICAR_PRONUNCIAMENTO", "ALINHAR_TAXONOMIA_CNJ_E_TIPO_PETICAO", "RESOLVER_COMPETENCIA_E_DISTRIBUICAO", "ABRIR_PECA_ESPECIFICA", "VALIDAR_CHECKLIST_FORMAL", "CONECTAR_MALHA_PAINEIS_WORKBENCHES", "PUBLICAR_PAINEL_CIDADAO_RECURSAL_PROPRIO", "ABRIR_PAINEL_RECURSAL_PARTES_REPRESENTANTES", "ORQUESTRAR_PAINEL_EXTERNO_OPERACIONAL", "ORQUESTRAR_REPRESENTACAO_E_CAIXAS_INSTITUCIONAIS", "ORQUESTRAR_ALERTAS_PRAZO_E_NOTIFICACOES", "ESCALONAR_ALERTAS_POR_PERFIL_E_CRITICIDADE", "ORQUESTRAR_POS_JULGAMENTO_RECURSAL", "ATIVAR_ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS", "REUSAR_STUDIO_E_JORNADA_PETICIONAMENTO_RECURSAL", "DIFERENCIAR_PETICIONAMENTO_POR_RITO_E_ESPECIE", "DIFERENCIAR_TRIBUNAL_ORGAO_PRAZO_E_FILTROS", "LIMITAR_COMUTACAO_A_ENVOLVIDOS_E_PRESERVAR_BUSCA_NEUTRA", "MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO", "ORQUESTRAR_MALOTES_PETICIONAMENTO_E_ATOS_RECURSAIS", "REUSAR_SUPERFICIES_EXISTENTES", "ANALISAR_EFEITOS_RECURSAIS", "MONTAR_SECOES", "TRILHA_TRIBUNAL");
    }

    @Test
    void deveGerarPlaybookConectadoAsSuperficiesExistentesSemContratoParalelo() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                true,
                Set.of(),
                "ESTADUAL",
                "TRABALHISTA",
                false,
                true
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("PERSISTIR_MATRIZ_CAPACIDADES_SECRETARIA", "REUSAR_SUPERFICIES_EXISTENTES");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("REUSAR_SUPERFICIES_EXISTENTES"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("/api/v1/secretariat/queue/panel", "/api/v1/institutional-support/branchCode-estadual/coverage", "/api/v1/secretariat/operacional/trabalhista/processos/0/execucao/impulsionamento");
    }

    @Test
    void deveGerarPlaybookComPassoDeContrarrazoesQuandoRecursoPrincipalJaExistir() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ABRIR_PECA_ESPECIFICA", "VALIDAR_CHECKLIST_FORMAL", "PREPARAR_CONTRARRAZOES", "ANALISAR_EFEITOS_RECURSAIS", "CHECAR_RETRATACAO_POTENCIAL");
    }

    @Test
    void deveBloquearPoderDeRecorrerQuandoHouverRenuncia() {
        RecursalAutomationResponse response = service.advise(new RecursalAutomationRequest(
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
                true,
                false,
                false,
                false,
                Set.of()
        ));

        assertThat(response.poderRecorrerBloqueado()).isTrue();
        assertThat(response.candidatos().getFirst().recurso()).isEqualTo("PODER_RECORRER_BLOQUEADO");
        assertThat(response.sinais()).extracting(signal -> signal.codigo()).contains("PODER_RECORRER_BLOQUEADO_RENUNCIA");
    }

    @Test
    void deveGerarPlaybookComPassoDeEscadaDeVisibilidadeSemCockpitParalelo() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ORQUESTRAR_ESCADA_VISIBILIDADE", "PUBLICAR_PAINEL_CIDADAO_RECURSAL_PROPRIO");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("ORQUESTRAR_ESCADA_VISIBILIDADE"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("partes", "representação técnica", "magistratura");
    }


    @Test
    void deveGerarPlaybookComPassoDePainelCidadaoProprioMovimentacaoECor() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("PUBLICAR_PAINEL_CIDADAO_RECURSAL_PROPRIO");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("PUBLICAR_PAINEL_CIDADAO_RECURSAL_PROPRIO"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("processos próprios", "últimas movimentações", "cores processuais");
    }



    @Test
    void deveGerarPlaybookComPassoDeSegmentacaoPorRamoRitoESigilo() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("SEGMENTAR_POR_RAMO_RITO_SIGILO");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("SEGMENTAR_POR_RAMO_RITO_SIGILO"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("TRABALHISTA", "sigilo", "Ministério Público");
    }



    @Test
    void deveGerarPlaybookComAvisosDePrazoENotificacoesSemSchedulerParalelo() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ORQUESTRAR_ALERTAS_PRAZO_E_NOTIFICACOES", "ORQUESTRAR_POS_JULGAMENTO_RECURSAL", "ATIVAR_ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("ORQUESTRAR_ALERTAS_PRAZO_E_NOTIFICACOES"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("prazo real", "preview de notificações", "multicanal");
    }


    @Test
    void deveGerarPlaybookComEscalonamentoDeAlertasPorPerfilECriticidade() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ESCALONAR_ALERTAS_POR_PERFIL_E_CRITICIDADE", "ORQUESTRAR_POS_JULGAMENTO_RECURSAL", "ATIVAR_ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("ESCALONAR_ALERTAS_POR_PERFIL_E_CRITICIDADE"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("perfil", "criticidade", "ELEITORAL");
    }


    @Test
    void deveGerarPlaybookComPosJulgamentoEscalonado() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                true,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ORQUESTRAR_POS_JULGAMENTO_RECURSAL");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("ORQUESTRAR_POS_JULGAMENTO_RECURSAL"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("pós-julgamento", "publicação", "nova subida");
    }

    @Test
    void deveGerarPlaybookComAlertaVermelhoMulticanalEVotosVivos() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
                "ACORDAO",
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
                true,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ATIVAR_ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("ATIVAR_ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("vermelho", "multicanal", "votos vivos");
    }


    @Test
    void deveGerarPlaybookComMalotesPeticionamentoEAtosRecursais() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
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
                false,
                false,
                false,
                false,
                true,
                Set.of(),
                "ESTADUAL",
                "CIVEL",
                false,
                true
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ORQUESTRAR_MALOTES_PETICIONAMENTO_E_ATOS_RECURSAIS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("ORQUESTRAR_MALOTES_PETICIONAMENTO_E_ATOS_RECURSAIS"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("malotes", "peticionamento", "auxiliares da Justiça");
    }


    @Test
    void deveGerarPassoDeDiferenciacaoDePeticionamentoPorRitoEEspecie() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                "ELEITORAL",
                "ELEITORAL",
                false,
                true
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("DIFERENCIAR_PETICIONAMENTO_POR_RITO_E_ESPECIE");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("DIFERENCIAR_PETICIONAMENTO_POR_RITO_E_ESPECIE"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("ELEITORAL");
    }


    @Test
    void deveGerarPlaybookComMatrizConcretaDePecasPorAtorERito() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                "MILITAR",
                false,
                true
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("MILITAR");
    }


    @Test
    void deveProjetarPassoDeDiferenciacaoEleitoralPorPrazoETribunal() {
        var response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                "ELEITORAL",
                "ELEITORAL",
                false,
                true
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("DIFERENCIAR_TRIBUNAL_ORGAO_PRAZO_E_FILTROS", "LIMITAR_COMUTACAO_A_ENVOLVIDOS_E_PRESERVAR_BUSCA_NEUTRA", "COMUTAR_PAINEIS_POR_RITO_TRIBUNAL_E_PERFIL", "AJUSTAR_VOCABULARIO_CARDS_ATALHOS_E_DETALHES", "AJUSTAR_PERFIL_ATOR_CARDS_RISCO_E_QUICK_ACTIONS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("DIFERENCIAR_TRIBUNAL_ORGAO_PRAZO_E_FILTROS"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("ELEITORAL");
    }

    @Test
    void deveProjetarPassoDeComutacaoContextualPenalPorPainel() {
        var response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("COMUTAR_PAINEIS_POR_RITO_TRIBUNAL_E_PERFIL", "AJUSTAR_VOCABULARIO_CARDS_ATALHOS_E_DETALHES", "AJUSTAR_PERFIL_ATOR_CARDS_RISCO_E_QUICK_ACTIONS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("COMUTAR_PAINEIS_POR_RITO_TRIBUNAL_E_PERFIL"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("PENAL");
    }


    @Test
    void deveProjetarShellContextualTaticoTrabalhistaSemRuido() {
        var response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("AJUSTAR_VOCABULARIO_CARDS_ATALHOS_E_DETALHES", "AJUSTAR_PERFIL_ATOR_CARDS_RISCO_E_QUICK_ACTIONS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("AJUSTAR_VOCABULARIO_CARDS_ATALHOS_E_DETALHES"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("vocabulário", "cards", "atalhos");
    }


    @Test
    void deveProjetarPassoDePerfilDeAtorNoContextoDaProcuradoriaTrabalhista() {
        var response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                "PROCURADORIA"
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("AJUSTAR_PERFIL_ATOR_CARDS_RISCO_E_QUICK_ACTIONS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("AJUSTAR_PERFIL_ATOR_CARDS_RISCO_E_QUICK_ACTIONS"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("PROCURADORIA", "TRABALHISTA");
    }


    @Test
    void deveProjetarPassoDePoliticaVisualOperacionalParaMpEleitoral() {
        var response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                "ELEITORAL",
                "ELEITORAL",
                false,
                true,
                "MINISTERIO_PUBLICO"
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("APLICAR_MATRIZ_FINA_POLITICA_VISUAL_OPERACIONAL");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("APLICAR_MATRIZ_FINA_POLITICA_VISUAL_OPERACIONAL"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("MINISTERIO_PUBLICO", "ELEITORAL");
    }



    @Test
    void deveGerarPlaybookComTaxonomiaCompetenciaEPaineisInstitucionais() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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
                true,
                false,
                "PROCURADORIA"
        ));

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains("ALINHAR_TAXONOMIA_CNJ_E_TIPO_PETICAO", "RESOLVER_COMPETENCIA_E_DISTRIBUICAO", "ORQUESTRAR_PAINEL_EXTERNO_OPERACIONAL", "ORQUESTRAR_REPRESENTACAO_E_CAIXAS_INSTITUCIONAIS");
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("RESOLVER_COMPETENCIA_E_DISTRIBUICAO"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("competência", "processo de referência");
    }



    @Test
    void deveGerarPlaybookComPainelAdvogadoLoteCertidoesCaixasEWizardAssistido() {
        RecursalAutomationPlaybookResponse response = playbookService.buildPlaybook(new RecursalAutomationRequest(
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

        assertThat(response.passos()).extracting(step -> step.codigo())
                .contains(
                        "ATIVAR_WIZARD_DISTRIBUICAO_ASSISTIDA_IA",
                        "PUBLICAR_PAINEL_ADVOGADO_RECURSAL_COMPLETO",
                        "EMITIR_CERTIDOES_EXTERNAS_E_EXECUTIVAS",
                        "REFORCAR_CAIXAS_HISTORICO_E_DEVOLUCAO_INSTITUCIONAL",
                        "ORQUESTRAR_PETICIONAMENTO_LOTE_E_ASSINATURA_LOTE"
                );
        assertThat(response.passos().stream()
                .filter(step -> step.codigo().equals("ATIVAR_WIZARD_DISTRIBUICAO_ASSISTIDA_IA"))
                .findFirst()
                .orElseThrow()
                .descricao())
                .contains("IA", "preflight");
    }

}
