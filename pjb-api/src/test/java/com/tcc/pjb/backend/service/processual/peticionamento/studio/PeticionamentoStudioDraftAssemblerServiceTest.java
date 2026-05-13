package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioDraftAssemblerServiceTest {

    @Test
    void deveMontarMinutaRapidaComRitoJurisprudenciaMapaProbatorioETimeline() {
        PeticionamentoStudioDraftAssemblerService service = new PeticionamentoStudioDraftAssemblerService();

        LaianePeticaoInicialDraftService.DraftView manualDraft = new LaianePeticaoInicialDraftService.DraftView(
                null,
                77L,
                "0001234-55.2026.8.06.0001",
                "Ação de obrigação de fazer com tutela de urgência",
                "CIVIL",
                "COMUM_ORDINARIO",
                "OBRIGACAO_DE_FAZER",
                85,
                88,
                List.of("O autor contratou o serviço e não recebeu a prestação prometida em 10/03/2026."),
                List.of("Seja compelida a ré a cumprir a obrigação no prazo judicial."),
                List.of("Aplicam-se a boa-fé objetiva, a força obrigatória do contrato e a tutela específica."),
                List.of("Contrato assinado e comprovantes de pagamento."),
                List.of("Revisar legitimidade e anexos essenciais."),
                "",
                null,
                null,
                null
        );

        var report = service.assemble(new PeticionamentoStudioDraftAssemblerService.ResolveRequest(
                "Ação de obrigação de fazer com tutela de urgência",
                Map.of("parteAutora", "Maria de Souza", "parteRe", "Empresa X"),
                Map.of(
                        "justicaSugerida", "Justiça Estadual",
                        "ritoProcessual", "COMUM_ORDINARIO",
                        "classeProcessual", "OBRIGACAO_DE_FAZER",
                        "comarca", "Fortaleza",
                        "uf", "CE"
                ),
                Map.of("items", List.of(
                        Map.of("label", "print_whatsapp.png", "summary", "Print indicado para demonstrar cobrança e recusa da ré.")
                )),
                Map.of("items", List.of(
                        Map.of("titulo", "TJCE - obrigação de fazer", "tese", "É cabível tutela específica quando houver inadimplemento contratual.", "fonte", "TJCE")
                )),
                Map.of("items", List.of(
                        Map.of("title", "Fato estruturado 1", "detail", "Contrato e inadimplemento", "dateHint", "10/03/2026")
                )),
                Map.of("items", List.of(
                        Map.of("requestLabel", "Cumprimento da obrigação", "strength", "ROBUSTO", "supportFacts", List.of("Fato 1"), "supportEvidence", List.of("print_whatsapp.png"), "supportGrounds", List.of("boa-fé objetiva"))
                )),
                Map.of("summary", List.of("Checklist procedimental sem pendências críticas visíveis na janela atual.")),
                Map.of(
                        "checklist", List.of("Revisar pedido liminar."),
                        "alerts", List.of("Confirmar multa diária proporcional.")
                ),
                manualDraft,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new BigDecimal("12500.00")
        ));

        assertTrue(report.markdown().contains("## Competência, rito e enquadramento"));
        assertTrue(report.markdown().contains("TJCE - obrigação de fazer"));
        assertTrue(report.markdown().contains("print_whatsapp.png"));
        assertTrue(report.markdown().contains("## Timeline do caso"));
        assertTrue(report.markdown().contains("## Matriz prova x pedido"));
        assertTrue(report.markdown().contains("R$"));
    }

    @Test
    void deveMontarMinutaRecursalComCabimentoTimelineEMatrizProbatoria() {
        PeticionamentoStudioDraftAssemblerService service = new PeticionamentoStudioDraftAssemblerService();

        var report = service.assemble(new PeticionamentoStudioDraftAssemblerService.ResolveRequest(
                "Apelação cível contra sentença de improcedência",
                Map.of("parteAutora", "Maria de Souza", "parteRe", "Empresa X"),
                Map.of(
                        "petitionFamily", "RECURSAL",
                        "canonicalAppealType", "APELACAO",
                        "justicaSugerida", "Justiça Estadual",
                        "ritoProcessual", "COMUM_ORDINARIO",
                        "classeProcessual", "APELACAO",
                        "comarca", "Fortaleza",
                        "uf", "CE",
                        "recursalBlueprint", Map.of(
                                "travasDeValidacao", List.of("Conferir tempestividade e representação recursal."),
                                "documentosObrigatorios", List.of(
                                        Map.of("label", "Decisão recorrida"),
                                        Map.of("label", "Comprovação da intimação")
                                )
                        )
                ),
                Map.of(),
                Map.of("items", List.of(
                        Map.of("titulo", "TJCE - apelação", "tese", "É exigido ataque dialético aos fundamentos da sentença.", "fonte", "TJCE")
                )),
                Map.of("items", List.of(Map.of("title", "Sentença recorrida", "detail", "Improcedência do pedido"))),
                Map.of("items", List.of(Map.of("requestLabel", "Provimento da apelação", "strength", "MODERADO", "supportFacts", List.of("Fato principal"), "supportEvidence", List.of("Sentença recorrida"), "supportGrounds", List.of("error in judicando")))),
                Map.of("summary", List.of("Há checkpoint recursal crítico em revisão assistida.")),
                Map.of("checklist", List.of("Fechar preparo recursal.")),
                null,
                List.of("A sentença julgou improcedente o pedido apesar da prova documental suficiente."),
                List.of("Há error in judicando e indevida valoração da prova documental."),
                List.of("Conhecimento e provimento da apelação para reformar integralmente a sentença."),
                List.of("Sentença recorrida", "Certidão de intimação"),
                null
        ));

        assertTrue(report.markdown().contains("## Cabimento, tempestividade e regularidade formal"));
        assertTrue(report.markdown().contains("## Timeline do caso"));
        assertTrue(report.markdown().contains("## Matriz prova x pedido"));
        assertTrue(report.markdown().contains("Decisão recorrida") || report.markdown().contains("Sentença recorrida"));
    }

    @Test
    void deveMontarEmbargosComSecaoDeVicioTimelineEMatriz() {
        PeticionamentoStudioDraftAssemblerService service = new PeticionamentoStudioDraftAssemblerService();

        var report = service.assemble(new PeticionamentoStudioDraftAssemblerService.ResolveRequest(
                "Embargos de declaração",
                Map.of("parteAutora", "Maria de Souza", "parteRe", "Empresa X"),
                Map.of(
                        "petitionFamily", "EMBARGOS",
                        "canonicalAppealType", "EMBARGOS_DECLARACAO",
                        "classeProcessual", "EMBARGOS_DECLARACAO",
                        "ramoDireito", "CIVIL",
                        "embargosGrounds", List.of("OMISSAO", "CONTRADICAO"),
                        "recursalBlueprint", Map.of(
                                "documentosObrigatorios", List.of(Map.of("label", "Decisão embargada"))
                        )
                ),
                Map.of(),
                Map.of(),
                Map.of("items", List.of(Map.of("title", "Decisão embargada", "detail", "Acórdão com omissão"))),
                Map.of("items", List.of(Map.of("requestLabel", "Integração do acórdão", "strength", "MODERADO", "supportFacts", List.of("Decisão embargada"), "supportEvidence", List.of("Decisão embargada"), "supportGrounds", List.of("omissão")))),
                Map.of("summary", List.of("Embargos exigem vício individualizado.")),
                Map.of("checklist", List.of("Fechar vício da decisão.")),
                null,
                List.of("A decisão deixou de enfrentar pedido de dano moral e apresentou contradição na fundamentação."),
                List.of("Há omissão e contradição na decisão embargada."),
                List.of("Conhecimento e acolhimento dos embargos para integrar a decisão."),
                List.of("Decisão embargada"),
                null
        ));

        assertTrue(report.markdown().contains("## Vício da decisão"));
        assertTrue(report.markdown().contains("## Timeline do caso"));
        assertTrue(report.markdown().contains("## Matriz prova x pedido"));
        assertTrue(report.markdown().contains("OMISSAO") || report.markdown().contains("omissão"));
        assertTrue(report.markdown().contains("## Pedidos integrativos"));
    }
}
