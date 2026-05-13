package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.catalog;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefensoriaEProcuradoriaInstitutionalPanelBlueprintCatalog implements InstitutionalPanelBlueprintCatalog {

    @Override
    public List<InstitutionalPanelBlueprintSpec> specs() {
        return List.of(
                spec("DEFENSORIA_TITULAR", "NUCLEO_DEFENSORIA", "PAINEL_TITULAR", "Defensor público", "Comunicações recebidas, ciência e petições de resposta", InstitutionalApiRoutes.painelExecutivo("NUCLEO_DEFENSORIA"),
                                        List.of("caixa_do_nucleo", "pendencias_de_ciencia", "peticoes_e_prioridades", "assistidos_urgentes", "agenda_audiencias", "pareceres_defensivos", "calculadora_judicial"),
                                        List.of("dar_ciencia", "peticionar", "assinar_manifestacao", "delegar_preparo", "solicitar_audiencia", "emitir_parecer_defensivo", "abrir_calculadora_judicial"),
                                        List.of("Gov.br forte", "Nomeação homologada", "Certificado quando o ato exigir"),
                                        List.of("Sem conta única da defensoria"),
                                        List.of("Entrada sempre pessoal com contexto institucional ativo.")),
                spec("DEFENSORIA_ASSESSORIA", "NUCLEO_DEFENSORIA", "PAINEL_CAIXA", "Assessoria da Defensoria", "Preparação de peças, organização recursal e fila do assistido", InstitutionalApiRoutes.painelExecutivo("NUCLEO_DEFENSORIA", "ASSESSORIA"),
                                        List.of("respostas_em_preparo", "recursos_pendentes", "documentos_do_assistido", "urgencias_sociais", "dossie_audiencia", "calculadora_judicial"),
                                        List.of("preparar_minuta", "montar_resposta", "sinalizar_urgencia", "escalar_ao_defensor", "organizar_dossie_audiencia", "abrir_calculadora_judicial"),
                                        List.of("Nomeação ativa", "Sem uso de conta única"),
                                        List.of("Sem assinatura final do núcleo"),
                                        List.of("Assessoria defensoria foca preparação, sem romper a atuação pessoal do defensor.")),
                spec("PROCURADORIA_TITULAR", "PROCURADORIA_PUBLICA", "PAINEL_TITULAR", "Procuradoria pública", "Painel para intimações, prazos fazendários e lotação interna", InstitutionalApiRoutes.painelExecutivo("PROCURADORIA_PUBLICA"),
                                        List.of("intimacoes_fazendarias", "prazos_criticos", "peticoes_prioritarias", "caixas_setoriais", "agenda_audiencias", "pareceres_fazendarios", "calculadora_judicial"),
                                        List.of("assumir_expediente", "peticionar", "delegar_assessor", "redistribuir_setorialmente", "solicitar_audiencia", "emitir_parecer_fazendario", "abrir_calculadora_judicial"),
                                        List.of("Nomeação pelo órgão", "Dupla aprovação para administradores", "Certificado institucional conforme política"),
                                        List.of("Sem atuação fora do escopo territorial/material homologado"),
                                        List.of("Abrange AGU, procuradorias estaduais e municipais dentro do mesmo desenho de governança.")),
                spec("PROCURADORIA_ASSESSORIA", "PROCURADORIA_PUBLICA", "PAINEL_CAIXA", "Assessoria fazendária", "Minutas fazendárias, recursos e embargos à execução fiscal", InstitutionalApiRoutes.painelExecutivo("PROCURADORIA_PUBLICA", "ASSESSORIA"),
                                        List.of("embargos_execucao_fiscal", "contrarrazoes", "informacoes_para_mandado_segurança", "controle_de_prazo", "dossie_audiencia", "calculadora_judicial"),
                                        List.of("preparar_minuta", "apontar_risco_recursal", "pedir_documento_setorial", "escalar_ao_procurador", "organizar_dossie_audiencia", "abrir_calculadora_judicial"),
                                        List.of("Nomeação ativa", "Trilha forense de redistribuição"),
                                        List.of("Sem assinatura final do ente"),
                                        List.of("Fluxo próprio para fazenda, execução e incidentes fiscais."))
        );
    }

    private InstitutionalPanelBlueprintSpec spec(String codigo,
                                                 String escopo,
                                                 String panel,
                                                 String audience,
                                                 String titulo,
                                                 String rota,
                                                 List<String> secoes,
                                                 List<String> acoes,
                                                 List<String> guardas,
                                                 List<String> visibilidade,
                                                 List<String> fundamentos) {
        return new InstitutionalPanelBlueprintSpec(codigo, escopo, panel, audience, titulo, rota, secoes, acoes, guardas, visibilidade, fundamentos);
    }
}
