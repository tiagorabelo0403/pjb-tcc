package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.time.LocalDate;
import java.util.List;

public record PjbJuizadoAdjuntoPublicGuidance(String tribunal,
                                              String comarca,
                                              String nucleoName,
                                              LocalDate startsAt,
                                              String systemOfRecord,
                                              boolean optionalElection,
                                              boolean electionAtProtocolOnly,
                                              boolean petitionMentionIsInsufficient,
                                              boolean immutableAfterDistribution,
                                              boolean noAutomaticRedistribution,
                                              List<String> publicMessages,
                                              List<String> benefits,
                                              List<String> legalBasis,
                                              String supportChannel) {

    public static PjbJuizadoAdjuntoPublicGuidance moradaNova() {
        return new PjbJuizadoAdjuntoPublicGuidance(
                "TJCE",
                "Morada Nova",
                "Núcleo de Justiça 4.0 – Juizados Especiais Cíveis Adjuntos",
                LocalDate.of(2026, 5, 18),
                "PJe",
                true,
                true,
                true,
                true,
                true,
                List.of(
                        "A opção pelo Núcleo 4.0 é facultativa",
                        "A opção deve ocorrer no momento do protocolo no cadastro da ação",
                        "Não basta mencionar a opção na petição inicial",
                        "A escolha não poderá ser alterada após a distribuição",
                        "Sem opção no cadastro, o processo segue na vara comum",
                        "Não há redistribuição automática para o Núcleo 4.0",
                        "A escolha da parte autora é respeitada integralmente"),
                List.of(
                        "mais agilidade nos processos",
                        "maior capacidade de atendimento",
                        "modernização e inovação"),
                List.of(
                        "Portaria TJCE nº 73/2026",
                        "Resolução do Tribunal Pleno TJCE nº 13/2024",
                        "Orientação Normativa CGJE nº 05/2025"),
                "Diretoria do Foro da Comarca de Morada Nova");
    }
}
