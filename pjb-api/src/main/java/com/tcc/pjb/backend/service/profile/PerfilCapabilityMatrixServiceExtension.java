package com.tcc.pjb.backend.service.profile;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
@Service
public class PerfilCapabilityMatrixServiceExtension {
public List<String> capacidadesJuiz(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("FILA_DECISIONAL");
out.add("PAINEL_GABINETE");
out.add("ASSINATURA_DESPACHO");
out.add("PROLACAO_SENTENCA");
out.add("DESIGNACAO_AUDIENCIA");
out.add("AUTORIZACAO_DILIGENCIA");
out.add("CONTROLE_PRAZOS_PROCESSUAIS");
out.add("ACESSO_SIGILO_COMPLETO");
out.add("DELEGACAO_ASSESSOR");
out.add("GESTAO_PREVENÇÃO_CONEXAO");
out.add("REDISTRIBUICAO_IMPEDIMENTO");
out.add("AUTORIZACAO_PERICIA");
out.add("HOMOLOGACAO_ACORDO");
out.add("EXPEDICAO_ORDEM_CUMPRIMENTO");
out.add("TRILHA_AUDITORIA_DECISORIAL");
out.add("AI_JURIDICA_SUPORTE_DECISAO");
out.add("ANALYTICS_ACERVO_PESSOAL");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.JUIZ_ELEITORAL) {
out.add("MODULO_ELEITORAL_ESPECIFICO");
}
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.JUIZ_TRABALHISTA) {
out.add("MODULO_TRABALHISTA_CLT");
}
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.JUIZ_MILITAR) {
out.add("MODULO_MILITAR_ESPECIFICO");
}
return List.copyOf(out);
}
public List<String> capacidadesDesembargador(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("PAINEL_COLEGIADO");
out.add("RELATORIO_RECURSO");
out.add("PROLACAO_VOTO");
out.add("LAVRATURA_ACORDAO");
out.add("PEDIDO_VISTA");
out.add("DESTAQUE_PAUTA");
out.add("PREFERENCIA_JULGAMENTO");
out.add("ACESSO_SIGILO_COMPLETO");
out.add("DELEGACAO_ASSESSOR_DESEMBARGADOR");
out.add("TRILHA_AUDITORIA_COLEGIADO");
out.add("AI_JURIDICA_SUPORTE_DECISAO");
out.add("ANALYTICS_RELATORIAS");
out.add("SESSAO_TURMA_CAMARA");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR_FEDERAL) {
out.add("MODULO_FEDERAL_TRF");
}
return List.copyOf(out);
}

public List<String> capacidadesMinistro(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("PAINEL_PLENARIO");
out.add("DECISAO_MONOCRATICA");
out.add("VOTO_TURMA_PLENARIO");
out.add("LAVRATURA_ACORDAO_STF_STJ");
out.add("INCLUSAO_PAUTA_PLENARIO");
out.add("PEDIDO_VISTA_PLENARIO");
out.add("QUESTAO_ORDEM");
out.add("ACESSO_SIGILO_SUPREMO");
out.add("STEP_UP_OBRIGATORIO");
out.add("DELEGACAO_ASSESSOR_MINISTRO");
out.add("AI_JURIDICA_SUPORTE_SUPREMO");
out.add("ANALYTICS_PLENARIO_NACIONAL");
out.add("TRIBUNAL_PLENO_PARTICIPACAO");
return List.copyOf(out);
}
public List<String> capacidadesAdvogado(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("COCKPIT_ESCRITORIO");
out.add("CARTEIRA_CLIENTES");
out.add("PROTOCOLO_PETICAO");
out.add("CIENCIA_INTIMACOES_LOTE");
out.add("INTERPOSICAO_RECURSO");
out.add("CONSULTA_PROCESSOS_PATROCINADOS");
out.add("AGENDA_AUDIENCIAS");
out.add("ASSINATURA_DIGITAL_OAB");
out.add("NOTIFICACOES_PRAZO_CRITICO");
out.add("ANALYTICS_CARTEIRA_RISCO");
out.add("AI_JURIDICA_MINUTA_ASSISTIDA");
out.add("PASTA_INTELIGENTE_CLIENTE");
out.add("ACESSO_AUTOS_PROCESSO");
return List.copyOf(out);
}
public List<String> capacidadesCidadao(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("DASHBOARD_PROCESSOS_PROPRIOS");
out.add("TIMELINE_VISUAL_LINGUAGEM_SIMPLES");
out.add("ORIENTACAO_AUDIENCIA");
out.add("EXPORTACAO_RESUMO_PROCESSO");
out.add("NOTIFICACOES_MULTICANAL");
out.add("CONSULTA_DOCUMENTOS_PROPRIOS");
out.add("INTEGRACAO_GOVBR");
out.add("MAPA_FORUM_GEOLOCALIZACAO");
out.add("SUPORTE_TRIAGEM_CHAT");
out.add("CALENDARIO_AUDIENCIAS");
return List.copyOf(out);
}
public List<String> capacidadesDefensor(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("PAINEL_ASSISTIDOS");
out.add("PROTOCOLO_DEFESA");
out.add("HABEAS_CORPUS");
out.add("ASSISTENCIA_JUDICIARIA_GRATUITA");
out.add("ACOMPANHAMENTO_PRESOS");
out.add("MANDADO_SEGURANCA");
out.add("RECURSOS_URGENTES");
out.add("ACESSO_AUTOS_ASSISTIDOS");
out.add("NOTIFICACOES_PRAZO_CRITICO");
out.add("AI_JURIDICA_DEFESA");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL) {
out.add("MODULO_FEDERAL_DPU");
}
return List.copyOf(out);
}
public List<String> capacidadesMp(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();

out.add("PAINEL_MP_INSTITUCIONAL");
out.add("MANIFESTACAO_PROCESSUAL");
out.add("REQUISICAO_DILIGENCIA_DELEGADO");
out.add("OFERECIMENTO_DENUNCIA");
out.add("ACORDO_PENAL_NAO_PERSECUCAO");
out.add("ACOMPANHAMENTO_INQUERITOS");
out.add("RECURSOS_MP");
out.add("ACESSO_SIGILO_MP");
out.add("NOTIFICACOES_PRAZO_48H");
out.add("AI_JURIDICA_PARECER");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.PROCURADOR_GERAL_REPUBLICA) {
out.add("MODULO_PGR");
}
return List.copyOf(out);
}
public List<String> capacidadesProcurador(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("PAINEL_PROCURADORIA");
out.add("CONTESTACAO_FAZENDA");
out.add("EXECUCAO_FISCAL_AJUIZAMENTO");
out.add("PARECER_INSTITUCIONAL");
out.add("RECURSOS_FAZENDA");
out.add("CONTROLE_DIVIDA_ATIVA");
out.add("INTEGRACAO_SIDA_PGFN");
out.add("NOTIFICACOES_PRAZO_48H");
out.add("AI_JURIDICA_FAZENDA");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.PROCURADORIA_FEDERAL) {
out.add("MODULO_AGU_FEDERAL");
}
return List.copyOf(out);
}
public List<String> capacidadesPerito(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("ACEITE_NOMEACAO");
out.add("APRESENTACAO_LAUDO");
out.add("RESPOSTA_QUESITOS");
out.add("SOLICITACAO_HONORARIOS");
out.add("ACESSO_AUTOS_PERICIA");
out.add("AGENDA_PERICIAS");
out.add("AI_ASSISTENCIA_LAUDO");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.PERITO_DIGITAL) {
out.add("FORENSE_DIGITAL_AVANCADA");
}
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.PERITO_CONTABIL) {
out.add("ANALISE_CONTABIL_AVANCADA");
}
return List.copyOf(out);
}
public List<String> capacidadesConciliador(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("PAINEL_CEJUSC");
out.add("AGENDAMENTO_SESSAO");
out.add("REGISTRO_RESULTADO_SESSAO");
out.add("LAVRATURA_TERMO_ACORDO");
out.add("TAXA_ACORDO_ANALYTICS");
out.add("AI_SUPORTE_CONCILIACAO");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.MEDIADOR) {
out.add("TECNICAS_MEDIACAO_AVANCADAS");
}
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.ARBITRO) {
out.add("SENTENCA_ARBITRAL");
out.add("MODULO_ARBITRAGEM");
}
return List.copyOf(out);
}
public List<String> capacidadesApoioTecnicoSaude(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("PAINEL_APOIO_TECNICO_SAUDE");
out.add("RECEPCAO_REQUISICAO_JUDICIAL_SAUDE");
out.add("JUNTADA_PRONTUARIO_CONTROLADA");
out.add("SUBMISSAO_RELATORIO_CLINICO");
out.add("RESPOSTA_QUESITOS_MEDICOS");
out.add("SOLICITACAO_COMPLEMENTACAO_EXAMES");
out.add("AGENDA_ATENDIMENTO_PERICIAL");
out.add("TRILHA_AUDITORIA_DADOS_SENSIVEIS");
out.add("MINIMIZACAO_DADOS_SAUDE");
out.add("CONTROLE_COMPARTILHAMENTO_PRONTUARIO");
out.add("AI_ASSISTENCIA_RESPOSTA_TECNICA");
out.add("CANAL_INSTITUCIONAL_SAUDE_JUDICIAL");
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.MEDICO) {
out.add("PARECER_MEDICO_INDIVIDUAL");
out.add("VALIDACAO_ASSINATURA_PROFISSIONAL");
}
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.HOSPITAL) {
out.add("GESTAO_EQUIPE_MULTIDISCIPLINAR");
out.add("RESPOSTA_INSTITUCIONAL_HOSPITALAR");
}
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.UPA) {
out.add("TRIAGEM_URGENTE_SAUDE");
out.add("ENCAMINHAMENTO_REDE_ASSISTENCIAL");
}
if (usuario != null && usuario.getTipoUsuario() == TipoUsuario.CLINICA) {
out.add("GESTAO_ATENDIMENTO_AMBULATORIAL");
out.add("RESPOSTA_TECNICA_CLINICA");
}
return List.copyOf(out);
}
public List<String> capacidadesServidor(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();

out.add("PAINEL_SECRETARIA");
out.add("JUNTADA_DOCUMENTOS");
out.add("EXPEDICAO_INTIMACAO");
out.add("CONCLUSAO_PARA_DESPACHO");
out.add("EXPEDICAO_MANDADO");
out.add("CERTIFICACAO_ATOS");
out.add("SANEAMENTO_FILA");
out.add("AGENDA_AUDIENCIAS_SECRETARIA");
out.add("NOTIFICACOES_SECRETARIA");
return List.copyOf(out);
}
public List<String> capacidadesAdministrador(Usuario usuario) {
LinkedHashSet<String> out = new LinkedHashSet<>();
out.add("DASHBOARD_NACIONAL");
out.add("METRICS_POR_TRIBUNAL");
out.add("METRICS_POR_COMARCA");
out.add("RECONCILIACAO_GLOBAL");
out.add("HEALTH_CHECK_SISTEMA");
out.add("RUNBOOK_OPERACIONAL");
out.add("GESTAO_POLITICAS_SEGURANCA");
out.add("AUDITORIA_LGPD");
out.add("SLA_REPORT");
out.add("MODO_EMERGENCIA");
out.add("GESTAO_USUARIOS_SISTEMA");
out.add("OBSERVABILIDADE_NACIONAL");
return List.copyOf(out);
}
}