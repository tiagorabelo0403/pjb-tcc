package com.tcc.pjb.backend.service.profile;

import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaOperationalContextResponse;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public class PerfilCapabilityMatrixService {

    public List<String> capacidadesOficial(Usuario usuario) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("MANDADOS");
        out.add("ROTA_DIA");
        out.add("CERTIDAO_NEGATIVA_AUTOMATICA");
        out.add("LOCALIZADOR_PESSOAS_CPF");
        out.add("TRILHA_DILIGENCIA_COM_GOVERNANCA");
        out.add("PRECHECK_CIRCUNSCRICAO");
        out.add("CONSULTAS_RECENTES_AUDITAVEIS");
        out.add("TRADUTOR_JURIDICO_IA");
        out.add("CONTROLE_PRAZOS_FATAIS");
        out.add("IDENTIDADE_GOVBR_FEDERADA");
        out.add("MINIMIZACAO_ENDERECO_POR_POLITICA");
        out.add("ROTEIRIZACAO_OTIMIZADA_DILIGENCIAS");
        out.add("TELEMETRIA_OPERACIONAL_DILIGENCIAS");
        out.add("CADEIA_CUSTODIA_DIGITAL");
        out.add("LEDGER_CADEIA_CUSTODIA_IMUTAVEL");
        out.add("CHECKPOINT_CHEGADA_GEOFENCE");
        out.add("SINCRONIZACAO_OFFLINE_FIRST_DILIGENCIAS");
        out.add("MALHA_CUSTODIA_INTERINSTITUCIONAL");
        out.add("VINCULO_DIRETO_CHECKPOINT_MANDADO");
        out.add("CERTIDAO_AUTOMATICA_GEOTAGGEADA");
        out.add("TRILHA_TENTATIVA_ASSINADA");
        out.add("ENCERRAMENTO_OPERACIONAL_IDEMPOTENTE");
        out.add("VINCULO_DOCUMENTAL_CERTIDAO");
        out.add("TEMPLATE_INSTITUCIONAL_CERTIDAO_V2");
        out.add("FORMALIZACAO_PROCESSUAL_AUDITAVEL");
        out.add("MINUTA_OPERACIONAL_ASSINADA");
        out.add("OFICIO_DO_OFICIAL_NATIVO_PJB");
        out.add("RESPOSTA_A_OFICIO_DO_OFICIAL_NATIVA_PJB");
        out.add("CATALOGO_TIPOS_OFICIO_OFICIAL");
        out.add("MINUTAS_GOVERNADAS_OFICIO_OFICIAL");
        out.add("DESTINATARIO_INSTITUCIONAL_ESTRUTURADO_OFICIAL");
        out.add("TRACEABLE_DELIVERY_OFICIO_OFICIAL");
        out.add("MALHA_INSTITUCIONAL_EXTERNA_OFICIO_OFICIAL");
        out.add("ACK_CANAL_OFICIO_OFICIAL");
        out.add("ACK_CARTORARIO_OFICIO_OFICIAL");
        out.add("RECONCILIACAO_MATERIALIZADA_OFICIO_OFICIAL");
        out.add("RASTREIO_OPERACIONAL_POR_MANDADO_OFICIAL");
        out.add("TRIAGEM_PLANILHADA_ENDERECOS_OFICIAL");
        out.add("SINAIS_IDENTIDADE_RECEITA_NO_RASTREIO_OFICIAL");
        out.add("RASTREIO_PROCESSO_POLO_OFICIAL");
        out.add("ROTA_SUGERIDA_A_PARTIR_DA_TRIAGEM_OFICIAL");
        out.add("PENDENCIAS_PROCESSUAIS_PLANILHADAS_OFICIAL");
        out.add("PORTFOLIO_PROCESSOS_NOMEADOS_OFICIAL");
        out.add("ACESSO_PROCESSO_POR_NOMEACAO_OFICIAL");
        out.add("TRILHA_NOMEACAO_AUDITAVEL_OFICIAL");
        out.add("FILA_VIVA_DILIGENCIA_OFICIAL");
        out.add("WORKBENCH_PROCESSUAL_OFICIAL");
        out.add("SEGURANCA_TRIPLA_CAMADA_ENVIO_OFICIAL");
        out.add("BLOQUEIO_POS_CUMPRIMENTO_ENVIO_OFICIAL");
        out.add("FILTROS_RITOS_E_PASTAS_OFICIAL");
        out.add("CORES_ANDAMENTO_CONECTADAS_AO_OFICIAL");
        out.add("RESUMO_PROCESSUAL_FUNDAMENTADO_OFICIAL");
        out.add("CALCULADORA_JUDICIAL_NO_WORKBENCH_OFICIAL");
        out.add("IA_OPERACIONAL_GOVERNADA_OFICIAL");
        out.add("OFICIO_SOMENTE_EM_PROCESSO_COM_VINCULO_MATERIALIZADO");
        out.add("ESCOPO_ORGANIZACIONAL_POR_VARA_OFICIAL");
        out.add("FILTRO_PROCESSUAL_POR_RITO_E_VARA_OFICIAL");
        out.add("NUMERACAO_PROCESSUAL_ORGANIZADA_POR_RITO_OFICIAL");
        out.add("LOTAÇÃO_INSTITUCIONAL_GOVERNADA_OFICIAL");
        out.add("AGENDA_TERRITORIAL_INTELIGENTE_OFICIAL");
        out.add("PARIDADE_FUNCIONAL_OFICIAL_FEDERAL");
        out.add("BALCAO_VIRTUAL_GOVERNADO_OFICIAL");
        out.add("EXECUCAO_VIVA_DA_AGENDA_OFICIAL");
        out.add("NOTIFICACAO_INSTANTANEA_NOMEACAO_OFICIAL");
        out.add("SSE_PRIVADO_NOTIFICACOES_OFICIAL");
        out.add("CHAT_BALCAO_PROCESSUAL_OFICIAL");
        out.add("SUBSECAO_SECAO_TRIBUNAL_NO_ESCOPO_OFICIAL");
        out.add("EVENT_STORE_PROCESSUAL_INTEGRADO");
        out.add("JUNTADA_AUTOMATICA_PROCESSUAL");
        out.add("TIMELINE_OPERACIONAL_UNIFICADA");
        out.add("ANEXACAO_INSTITUCIONAL_INTEROPERAVEL");
        out.add("ANALYTICS_OPERACIONAIS_POR_PROCESSO");
        out.add("ANALYTICS_OPERACIONAIS_UNIDADE_OPERADOR");
        out.add("TEMPLATE_INSTITUCIONAL_CERTIDAO_V3");
        out.add("OUTBOX_TRANSACIONAL_MALHA_INSTITUCIONAL");
        out.add("REPLAY_CONTROLADO_MALHA_INSTITUCIONAL");
        out.add("ACK_EXTERNO_AUDITAVEL_MALHA");
        out.add("PAINEL_COMANDO_OPERACIONAL_MULTIUNIDADE");
        out.add("INDICES_OTIMIZADOS_NOTIFICACAO_E_WORKITEM_OFICIAL");
        out.add("BALCAO_VIRTUAL_COM_SLA_E_TEMPLATES_OFICIAL");
        out.add("PROCESSO_ENTRA_NO_PAINEL_POR_NOMEACAO_OFICIAL");
        out.add("CENTRAL_NOTIFICACOES_PROCESSUAIS_OFICIAL");
        out.add("REPLANEJAMENTO_VIVO_DA_ROTA_OFICIAL");
        out.add("TENTATIVA_POR_ENDERECO_OFICIAL");
        out.add("MOTIVO_FRUSTRACAO_ESTRUTURADO_OFICIAL");
        out.add("AGRUPAMENTO_BAIRRO_MICROTERRITORIO_OFICIAL");
        out.add("REORDENACAO_TERRITORIAL_TEMPO_REAL_OFICIAL");
        out.add("CALENDARIO_OPERACIONAL_OFICIAL_NO_PAINEL");
        out.add("CIENCIA_DA_INTIMACAO_PELO_OFICIAL");
        out.add("JUNTADA_DIRETA_DO_OFICIO_ORIGINAL_NO_PROCESSO");
        out.add("ORIGINAL_GOVERNADO_ONLY_NA_JUNTADA_DIRETA");
        out.add("CLASSIFICADOR_AUTOMATICO_RETORNO_FORUM_TRIBUNAL");
        out.add("FILA_POR_UNIDADE_RESPONSAVEL_NO_RETORNO_OFICIAL");
        out.add("REABERTURA_AUDITAVEL_POR_REINTIMACAO_OFICIAL");
        out.add("BALCAO_CONSUMO_RETORNO_OFICIAL_FORUM_SECRETARIA");
        out.add("REATIVACAO_COM_REAPARICAO_NO_PAINEL_OFICIAL");
        out.add("REINTIMACAO_AUTOMATICA_POR_EXPEDICAO_SECRETARIA");
        out.add("REATIVACAO_AUTOMATICA_DO_OFICIAL_NO_MESMO_ATO");
        out.add("SECRETARIA_GABINETE_ACOPLADOS_A_REABERTURA_DO_OFICIAL");
        out.add("ORDEM_JUDICIAL_DE_CUMPRIMENTO_NO_TRILHO_DO_OFICIAL");
        out.add("CIENCIA_OBRIGATORIA_EM_ORDEM_JUDICIAL_DO_OFICIAL");
        out.add("ENCERRAMENTO_COM_OFICIO_ORIGINAL_GOVERNADO_ONLY");
        out.add("CHECKLIST_FORMAL_ENCERRAMENTO_CUMPRIMENTO_OFICIAL");
        out.add("ENCERRAMENTO_SOBERANO_AUDITAVEL_DO_OFICIAL");
        out.add("JUNTADA_AUTOMATICA_CERTIDAO_E_MINUTA_NO_ENCERRAMENTO_OFICIAL");
        out.add("RECEPCAO_CLASSIFICADA_DO_CUMPRIMENTO_DO_OFICIAL_NA_SECRETARIA");
        out.add("ABERTURA_AUTOMATICA_DA_PROXIMA_PROVIDENCIA_CARTORARIA");
        out.add("RECLASSIFICACAO_AUDITAVEL_DO_RESULTADO_DO_CUMPRIMENTO_DO_OFICIAL");
        if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            out.add("AVALIACAO_PATRIMONIAL");
            out.add("INTEL_RESTRICOES_PATRIMONIAIS");
            out.add("PRECHECK_CNIB_RENAJUD");
            out.add("TRILHA_PENHORA_INTELIGENTE");
        }
        return List.copyOf(out);
    }

    public List<String> capacidadesDelegado(Usuario usuario) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("INQUERITOS");
        out.add("MANDADOS");
        out.add("REQUISICAO_DILIGENCIA_MP");
        out.add("LOCALIZADOR_PESSOAS_CPF");
        out.add("TRILHA_INVESTIGATIVA_AUDITAVEL");
        out.add("MALHA_CIRCUNSCRICAO");
        out.add("INTEL_RESTRICOES");
        out.add("CONSULTAS_RECENTES_AUDITAVEIS");
        out.add("CORRELACAO_MANDADOS_E_RESTRICOES");
        out.add("ROTEIRIZACAO_OTIMIZADA_DILIGENCIAS");
        out.add("TELEMETRIA_OPERACIONAL_DILIGENCIAS");
        out.add("CADEIA_CUSTODIA_DIGITAL");
        out.add("LEDGER_CADEIA_CUSTODIA_IMUTAVEL");
        out.add("CHECKPOINT_CHEGADA_GEOFENCE");
        out.add("SINCRONIZACAO_OFFLINE_FIRST_DILIGENCIAS");
        out.add("MALHA_CUSTODIA_INTERINSTITUCIONAL");
        out.add("VINCULO_DIRETO_CHECKPOINT_MANDADO");
        out.add("CERTIDAO_AUTOMATICA_GEOTAGGEADA");
        out.add("TRILHA_TENTATIVA_ASSINADA");
        out.add("ENCERRAMENTO_OPERACIONAL_IDEMPOTENTE");
        out.add("VINCULO_DOCUMENTAL_CERTIDAO");
        out.add("TEMPLATE_INSTITUCIONAL_CERTIDAO_V2");
        out.add("FORMALIZACAO_PROCESSUAL_AUDITAVEL");
        out.add("MINUTA_OPERACIONAL_ASSINADA");
        out.add("EVENT_STORE_PROCESSUAL_INTEGRADO");
        out.add("JUNTADA_AUTOMATICA_PROCESSUAL");
        out.add("TIMELINE_OPERACIONAL_UNIFICADA");
        out.add("ANEXACAO_INSTITUCIONAL_INTEROPERAVEL");
        out.add("ANALYTICS_OPERACIONAIS_POR_PROCESSO");
        out.add("ANALYTICS_OPERACIONAIS_UNIDADE_OPERADOR");
        out.add("TEMPLATE_INSTITUCIONAL_CERTIDAO_V3");
        out.add("OUTBOX_TRANSACIONAL_MALHA_INSTITUCIONAL");
        out.add("REPLAY_CONTROLADO_MALHA_INSTITUCIONAL");
        out.add("ACK_EXTERNO_AUDITAVEL_MALHA");
        out.add("PAINEL_COMANDO_OPERACIONAL_MULTIUNIDADE");
        out.add("PROTOCOLO_INQUERITO_DIRETO");
        if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            out.add("COOPERACAO_FEDERAL");
            out.add("CAMADA_INTERINSTITUCIONAL_FEDERAL");
            out.add("ARTICULACAO_INTERESTADUAL");
        }
        return List.copyOf(out);
    }

    public List<String> capacidadesMagistratura(Usuario usuario, UserPersona persona, boolean delegated) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("PAINEL_DECISORIO_UNIFICADO");
        out.add("GESTAO_DE_GABINETE");
        out.add("MALHA_RECURSAL_NACIONAL");
        out.add("TRIAGEM_POR_RITO");
        out.add("DISTRIBUICAO_POR_COMPETENCIA");
        out.add("LOCALIZADOR_PESSOAS_CPF");
        out.add("PAINEL_ORGAO_JULGADOR");
        out.add("CONSULTAS_RECENTES_AUDITAVEIS");
        out.add("TRADUTOR_JURIDICO_IA");
        out.add("CONTROLE_PRAZOS_FATAIS");
        out.add("IDENTIDADE_GOVBR_FEDERADA");
        appendByEsfera(out, persona != null ? persona.esfera() : null);
        appendByGrau(out, persona != null ? persona.grau() : null);
        appendByPerfilMagistratura(out, usuario != null ? usuario.getTipoUsuario() : null);
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura()) {
            out.add("ORDEM_EXECUTIVA_DIRETA");
            out.add("EXPEDICAO_DETERMINACOES_EXTERNAS");
            out.add("ORDENACAO_JUDICIAL_DIRETA_DE_CUMPRIMENTO_POR_OFICIAL");
        }
        if (delegated) {
            out.add("OPERACAO_DELEGADA_COM_CREDENCIAL");
        }
        return List.copyOf(out);
    }

    public List<String> filasMagistratura(Usuario usuario, UserPersona persona) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("MINUTAS_PENDENTES");
        out.add("PRAZOS_CRITICOS");
        out.add("PROCESSOS_SIGILOSOS_LIBERADOS");
        if (persona != null && persona.grau() != null) {
            out.add("GRAU_" + persona.grau().name());
        }
        if (persona != null && persona.esfera() != null) {
            out.add("ESFERA_" + persona.esfera().name());
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().name().contains("FEDERAL")) {
            out.add("COOPERACAO_INTERFEDERATIVA");
        }
        if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.MINISTRO) {
            out.add("TEMAS_E_PRECEDENTES_ESTRUTURANTES");
        }
        return List.copyOf(out);
    }

    public List<String> camadasRecursais(Usuario usuario, UserPersona persona) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        GrauJurisdicao grau = persona != null ? persona.grau() : null;
        EsferaJurisdicao esfera = persona != null ? persona.esfera() : null;
        if (grau == GrauJurisdicao.PRIMEIRO_GRAU) {
            out.add("PRIMEIRO_GRAU");
            out.add("SEGUNDO_GRAU");
            if (esfera == EsferaJurisdicao.JUSTICA_FEDERAL || esfera == EsferaJurisdicao.JUSTICA_ESTADUAL) {
                out.add("TRIBUNAL_SUPERIOR_COMPETENTE");
            }
            out.add("SUPREMO_TRIBUNAL_FEDERAL_QUANDO_CABIVEL");
        } else if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
            out.add("SEGUNDO_GRAU");
            out.add("TRIBUNAL_SUPERIOR_COMPETENTE");
            out.add("SUPREMO_TRIBUNAL_FEDERAL_QUANDO_CABIVEL");
        } else if (grau == GrauJurisdicao.SUPERIOR || isMinistro(usuario)) {
            out.add("TRIBUNAL_SUPERIOR");
            out.add("SUPREMO_TRIBUNAL_FEDERAL_QUANDO_CABIVEL");
        } else {
            out.add("MALHA_RECURSAL_UNIFICADA");
        }
        return List.copyOf(out);
    }

    public List<String> canaisExecutivos(Usuario usuario, boolean delegated) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura()) {
            out.add("INFOJUD");
            out.add("SISBAJUD");
            out.add("SERASAJUD");
            out.add("COOPERACAO_JUDICIARIA");
        }
        if (delegated) {
            out.add("DELEGACAO_CONTROLADA");
        }
        return List.copyOf(out);
    }

    public List<String> trilhasPorRito(UserPersona persona) {
        if (persona == null || persona.esfera() == null) {
            return List.of("RITO_COMUM");
        }
        return persona.esfera().getRitosPermitidos().stream().map(Enum::name).sorted().toList();
    }

    public String modoAtuacaoMagistratura(Usuario usuario, UserPersona persona) {
        TipoUsuario tipo = usuario != null ? usuario.getTipoUsuario() : null;
        if (tipo == TipoUsuario.MINISTRO) {
            return "COLEGIADO_SUPERIOR_OU_CONSTITUCIONAL";
        }
        if (tipo == TipoUsuario.DESEMBARGADOR || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return "COLEGIADO_REVISOR";
        }
        if (tipo != null && tipo.isAssessor()) {
            return "GABINETE_DELEGADO";
        }
        GrauJurisdicao grau = persona != null ? persona.grau() : null;
        if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
            return "REVISAO_E_UNIFORMIZACAO";
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return "PRECEDENTES_E_ADMISSIBILIDADE";
        }
        return "INSTRUCAO_E_DECISAO";
    }

    public String nivelSegurancaOperacional(Usuario usuario, UserPersona persona) {
        TipoUsuario tipo = usuario != null ? usuario.getTipoUsuario() : null;
        if (tipo == TipoUsuario.MINISTRO) {
            return "MAXIMO";
        }
        if (tipo == TipoUsuario.DESEMBARGADOR || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return "MUITO_ALTO";
        }
        GrauJurisdicao grau = persona != null ? persona.grau() : null;
        if (grau == GrauJurisdicao.SUPERIOR) {
            return "MUITO_ALTO";
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
            return "ALTO";
        }
        return "REFORCADO";
    }

    public List<String> widgetsMagistratura(Usuario usuario, UserPersona persona, boolean delegated) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("ACERVO_PRIORIZADO");
        out.add("PRAZOS_FATAIS");
        out.add("ORDENS_EXTERNAS");
        out.add("TRILHAS_POR_RITO");
        out.add("PRECEDENTES_ESTRATEGICOS");
        if (persona != null && persona.grau() == GrauJurisdicao.PRIMEIRO_GRAU) {
            out.add("MESA_AUDIENCIA");
            out.add("SANEAMENTO_E_INSTRUCAO");
        }
        if (persona != null && persona.grau() == GrauJurisdicao.SEGUNDO_GRAU) {
            out.add("PAUTA_COLEGIADA");
            out.add("SUSTENTACAO_ORAL");
        }
        if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.MINISTRO) {
            out.add("REPERCUSSAO_GERAL_E_TEMAS");
            out.add("GOVERNANCA_PRECEDENTES_NACIONAIS");
        }
        if (delegated) {
            out.add("CAIXA_DELEGADA_COM_CREDENCIAL");
        }
        return List.copyOf(out);
    }

    public List<MagistraturaOperationalContextResponse.RitoOperationalLane> trilhasOperacionaisPorRito(UserPersona persona, Usuario usuario) {
        if (persona == null || persona.esfera() == null) {
            return List.of(toLane(RitoProcessual.COMUM_ORDINARIO));
        }
        return persona.esfera().getRitosPermitidos().stream()
                .sorted(java.util.Comparator.comparing(Enum::name))
                .map(this::toLane)
                .toList();
    }

    private MagistraturaOperationalContextResponse.RitoOperationalLane toLane(RitoProcessual rito) {
        return new MagistraturaOperationalContextResponse.RitoOperationalLane(
                rito.name(),
                atosPrioritariosPorRito(rito),
                canaisPreferenciaisPorRito(rito)
        );
    }

    private List<String> atosPrioritariosPorRito(RitoProcessual rito) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (rito == null) {
            out.add("ANALISE_INICIAL");
            out.add("DESPACHO");
            return List.copyOf(out);
        }
        if (rito.isPenal()) {
            out.add("CONTROLE_MANDADOS");
            out.add("ANALISE_CAUTELARES");
            out.add("CUSTODIA_E_EXECUCAO");
        }
        if (rito.isTrabalhista()) {
            out.add("GESTAO_AUDIENCIA_UNA");
            out.add("EXECUCAO_TRABALHISTA");
        }
        if (rito.isPrevidenciario()) {
            out.add("PROVA_TECNICA_BENEFICIO");
            out.add("CROSSCHECK_CADASTRAL");
        }
        if (rito.isTribFazenda()) {
            out.add("EXECUCAO_FISCAL_E_CAUTELARES");
            out.add("BLOQUEIOS_PATRIMONIAIS");
        }
        if (rito.isEleitoral()) {
            out.add("JANELA_ELEITORAL_CRITICA");
            out.add("PAUTA_PRIORITARIA");
        }
        if (rito.isMilitar()) {
            out.add("CONTROLE_HIERARQUICO_MILITAR");
            out.add("CADEIA_DE_CUSTODIA");
        }
        String name = rito.name();
        if (name.contains("JUIZADO_ESPECIAL")) {
            out.add("ORALIDADE_E_CELERIDADE");
        }
        if (name.contains("MANDADO_SEGURANCA")) {
            out.add("ANALISE_LIMINAR");
        }
        if (name.contains("FAMILIA") || name.contains("ADOCAO") || name.contains("CURATELA")) {
            out.add("PROTECAO_INTEGRAL_E_SIGILO");
        }
        if (out.isEmpty()) {
            out.add("ANALISE_INICIAL");
            out.add("DESPACHO");
            out.add("DECISAO_E_CUMPRIMENTO");
        }
        return List.copyOf(out);
    }

    private List<String> canaisPreferenciaisPorRito(RitoProcessual rito) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("PJB_INTERNO");
        if (rito == null) {
            return List.copyOf(out);
        }
        if (rito.isPenal()) {
            out.add("BNMP");
            out.add("RENAJUD");
        }
        if (rito.isTribFazenda() || rito.name().contains("EXECUCAO") || rito.name().contains("CUMPRIMENTO")) {
            out.add("SISBAJUD");
            out.add("SERASAJUD");
            out.add("CNIB");
        }
        if (rito.isPrevidenciario() || rito.name().contains("FAZENDA_PUBLICA")) {
            out.add("INFOJUD");
        }
        if (rito.isEleitoral()) {
            out.add("BASE_ELEITORAL_ESPECIALIZADA");
        }
        if (rito.isMilitar()) {
            out.add("MALHA_MILITAR_ESPECIALIZADA");
        }
        return List.copyOf(out);
    }

    private static void appendByEsfera(LinkedHashSet<String> out, EsferaJurisdicao esfera) {
        if (esfera == null) {
            return;
        }
        switch (esfera) {
            case JUSTICA_ESTADUAL -> {
                out.add("TJ_PRIMEIRO_E_SEGUNDO_GRAU");
                out.add("INTEROPERABILIDADE_STJ_STF");
            }
            case JUSTICA_FEDERAL -> {
                out.add("VARAS_FEDERAIS_TRF");
                out.add("INTEROPERABILIDADE_STJ_STF");
            }
            case JUSTICA_TRABALHO -> {
                out.add("VARAS_TRT_TST");
                out.add("FLUXO_TRABALHISTA_ESPECIALIZADO");
            }
            case JUSTICA_ELEITORAL -> {
                out.add("ZONAS_TRE_TSE");
                out.add("FLUXO_ELEITORAL_ESPECIALIZADO");
            }
            case JUSTICA_MILITAR -> {
                out.add("AUDITORIAS_TJM_STM");
                out.add("FLUXO_MILITAR_ESPECIALIZADO");
            }
        }
    }

    private static void appendByGrau(LinkedHashSet<String> out, GrauJurisdicao grau) {
        if (grau == null) {
            return;
        }
        switch (grau) {
            case PRIMEIRO_GRAU -> out.add("ATUACAO_PRIMEIRO_GRAU");
            case SEGUNDO_GRAU -> out.add("ATUACAO_SEGUNDO_GRAU");
            case SUPERIOR, CONSTITUCIONAL -> out.add("ATUACAO_TRIBUNAL_SUPERIOR");
        }
    }

    private static void appendByPerfilMagistratura(LinkedHashSet<String> out, TipoUsuario tipo) {
        if (tipo == null) {
            return;
        }
        switch (tipo) {
            case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR, MAGISTRADO -> {
                out.add("MESA_DE_AUDIENCIA");
                out.add("ORDEM_CUMPRIMENTO_PRIMEIRO_GRAU");
            }
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> {
                out.add("PAUTA_COLEGIADA");
                out.add("UNIFORMIZACAO_REGIONAL");
            }
            case MINISTRO -> {
                out.add("FILTRO_NACIONAL_PRECEDENTES");
                out.add("GOVERNANCA_RECURSAL_SUPERIOR");
            }
            default -> {
            }
        }
    }

    private static boolean isMinistro(Usuario usuario) {
        return usuario != null && usuario.getTipoUsuario() == TipoUsuario.MINISTRO;
    }
}
