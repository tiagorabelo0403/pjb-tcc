package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.catalog;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MinisterioPublicoInstitutionalPanelBlueprintCatalog implements InstitutionalPanelBlueprintCatalog {

    @Override
    public List<InstitutionalPanelBlueprintSpec> specs() {
        return List.of(
                spec("PROMOTORIA_TITULAR", "PROMOTORIA", "PAINEL_TITULAR", "Promotor ou membro titular", "Vista obrigatória, urgências e assinatura institucional", InstitutionalApiRoutes.painelExecutivo("PROMOTORIA"),
                                        List.of("vistas_obrigatorias", "minutas_pendentes", "urgencias_criminais_e_civeis", "redistribuicoes", "agenda_audiencias", "pareceres_prioritarios", "calculadora_judicial"),
                                        List.of("dar_ciencia", "assinar_manifestacao", "atribuir_assessor", "abrir_substituicao", "solicitar_audiencia", "emitir_parecer", "abrir_calculadora_judicial"),
                                        List.of("MFA step-up", "Certificado ICP-Brasil", "Nomeação ativa"),
                                        List.of("Sem login compartilhado da promotoria"),
                                        List.of("O órgão nomeia; o PJB valida; a atuação é pessoal e auditável.")),
                spec("PROMOTORIA_ASSESSORIA", "PROMOTORIA", "PAINEL_CAIXA", "Assessoria institucional do MP", "Minutas, pareceres e redistribuições preparatórias", InstitutionalApiRoutes.painelExecutivo("PROMOTORIA", "ASSESSORIA"),
                                        List.of("minutas_de_parecer", "vistas_para_analise", "pendencias_de_prova", "retornos_de_diligencia", "dossie_audiencia", "calculadora_judicial"),
                                        List.of("preparar_minuta", "sugerir_encaminhamento", "escalar_ao_titular", "pedir_complementacao", "organizar_dossie_audiencia", "abrir_calculadora_judicial"),
                                        List.of("Nomeação ativa", "Sem substituição automática do titular"),
                                        List.of("Sem assinatura final", "Sem ciência final quando política exigir titular"),
                                        List.of("Assessoria trabalha em fila própria sem romper a autoria pessoal do membro.")),
                spec("PROMOTORIA_TRIAGEM", "PROMOTORIA", "PAINEL_TRIAGEM", "Triagem institucional do MP", "Classificação do processo por rito, urgência, recurso e caixa interna", InstitutionalApiRoutes.painelExecutivo("PROMOTORIA", "TRIAGEM"),
                                        List.of("entrada_nova", "prioridade_critica", "classificacao_rito", "encaminhamento_para_assessoria", "pedidos_de_audiencia", "documentos_para_pauta"),
                                        List.of("receber_comunicacao", "classificar_fluxo", "atribuir_membro", "devolver_para_fila", "sugerir_audiencia", "organizar_documentos"),
                                        List.of("Trilha forense", "Nomeação de triagem"),
                                        List.of("Sem manifestação jurídica final"),
                                        List.of("Triagem abre o fluxo correto antes de assessoria e titular."))
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
