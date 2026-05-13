package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NationalProceduralOperationalPlaybookService {

    private final NationalProceduralRightsCoverageService rightsCoverageService;

    public NationalProceduralOperationalPlaybookService(NationalProceduralRightsCoverageService rightsCoverageService) {
        this.rightsCoverageService = Objects.requireNonNull(rightsCoverageService);
    }

    public NationalProceduralOperationalPlaybookSnapshot snapshot() {
        List<NationalProceduralOperationalPlaybookRow> rows = Arrays.stream(RitoProcessual.values())
                .map(this::buildRow)
                .sorted(Comparator.comparing(NationalProceduralOperationalPlaybookRow::grupo)
                        .thenComparing(NationalProceduralOperationalPlaybookRow::ramo)
                        .thenComparing(NationalProceduralOperationalPlaybookRow::rito))
                .toList();
        LinkedHashSet<String> tracks = new LinkedHashSet<>();
        rows.forEach(row -> tracks.addAll(row.competenceTracks()));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("playbookFamilies", rows.stream().map(NationalProceduralOperationalPlaybookRow::grupo).distinct().toList());
        metadata.put("requiresSigilo", rows.stream().filter(row -> Boolean.TRUE.equals(row.metadata().get("segredoPadrao"))).count());
        metadata.put("supportsWizardProtocol", true);
        metadata.put("supportsTribunalVariations", true);
        return new NationalProceduralOperationalPlaybookSnapshot(
                Instant.now(),
                true,
                true,
                rows.size(),
                (int) rows.stream().map(NationalProceduralOperationalPlaybookRow::grupo).distinct().count(),
                List.copyOf(tracks),
                rows,
                metadata
        );
    }

    public NationalProceduralOperationalPlaybookRow describe(String ritoRaw) {
        RitoProcessual rito = RitoProcessual.tryParse(ritoRaw).orElse(RitoProcessual.COMUM_ORDINARIO);
        return buildRow(rito);
    }

    private NationalProceduralOperationalPlaybookRow buildRow(RitoProcessual rito) {
        NationalProceduralRightsCoverageRow coverage = rightsCoverageService.describe(rito.name());
        List<String> competenceTracks = resolveCompetenceTracks(rito, coverage);
        List<String> unitAnchors = resolveUnitAnchors(rito);
        List<String> requiredDocuments = resolveRequiredDocuments(rito);
        List<String> warnings = resolveWarnings(rito, coverage);
        List<NationalProceduralOperationalPlaybookStep> steps = List.of(
                new NationalProceduralOperationalPlaybookStep(1, "TRIAGEM_MATERIAL", "TRIAGEM", "Conferir direito material, classe e encaixe do rito", true,
                        List.of("classe processual conferida", "rito compatível", "ramo validado")),
                new NationalProceduralOperationalPlaybookStep(2, "COMPETENCIA_E_ORGAO", "COMPETENCIA", "Fechar competência territorial, funcional e unidade judiciária", true,
                        List.of("tribunal definido", "unidade sugerida", "foro ou seção validados")),
                new NationalProceduralOperationalPlaybookStep(3, "PARTES_E_REPRESENTACAO", "PARTES", "Qualificar partes, capacidade, representação e sigilo", true,
                        List.of("partes identificadas", "instrumento de representação", "sigilo processual")),
                new NationalProceduralOperationalPlaybookStep(4, "PROVA_E_DOCUMENTOS", "PROVA", "Consolidar documentos mínimos, prova e anexos essenciais", true,
                        List.of("documentos mínimos", "prova prioritária", "mídias e anexos")),
                new NationalProceduralOperationalPlaybookStep(5, "PEDIDOS_E_URGENCIA", "PEDIDOS", "Consolidar pedidos, tutela, valor da causa e fecho estratégico", true,
                        List.of("pedidos finais", "urgência ou cautelar", "valor da causa")),
                new NationalProceduralOperationalPlaybookStep(6, "ASSINATURA_E_PROTOCOLO", "PROTOCOLO", "Executar assinatura, step-up, preflight e protocolo judicial", true,
                        List.of("assinatura válida", "step-up quando exigido", "protocolo eletrônico")),
                new NationalProceduralOperationalPlaybookStep(7, "DISTRIBUICAO_E_ACOMPANHAMENTO", "POS_PROTOCOLO", "Conferir distribuição, autuação, prazo e trilha recursal", false,
                        List.of("número distribuído", "ciência inicial", "janela recursal monitorada"))
        );
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("segredoPadrao", coverage.segredoPadrao());
        metadata.put("exigeMinisterioPublico", coverage.exigeMinisterioPublico());
        metadata.put("admiteConciliacao", coverage.admiteConciliacao());
        metadata.put("admiteJuizado", coverage.admiteJuizado());
        metadata.put("autocompositivo", coverage.autocompositivo());
        metadata.put("internacional", coverage.internacional());
        metadata.put("coletivoOuEstrutural", coverage.coletivoOuEstrutural());
        metadata.put("supportsAllBrazilianRights", true);
        return new NationalProceduralOperationalPlaybookRow(
                coverage.rito(),
                coverage.ramo(),
                coverage.grupo(),
                coverage.protocoloSugerido(),
                competenceTracks,
                coverage.checkpointsOperacionais(),
                unitAnchors,
                requiredDocuments,
                coverage.garantiasEssenciais(),
                warnings,
                steps,
                metadata
        );
    }

    private List<String> resolveCompetenceTracks(RitoProcessual rito, NationalProceduralRightsCoverageRow coverage) {
        LinkedHashSet<String> out = new LinkedHashSet<>(coverage.justiceTracks());
        out.add("COMPETENCIA_TERRITORIAL");
        out.add("COMPETENCIA_FUNCIONAL");
        if (rito.isJuizado()) {
            out.add("ALCADA_E_JUIZADO");
        }
        if (rito.isTrabalhista()) {
            out.add("VARA_DO_TRABALHO_E_TRT");
        }
        if (rito.isPenal()) {
            out.add("PLANTAO_CUSTODIA_E_VARA_CRIMINAL");
        }
        if (rito.isInternacional()) {
            out.add("AUTORIDADE_CENTRAL_E_CORTE_SUPERIOR");
        }
        if (coverage.coletivoOuEstrutural()) {
            out.add("EFICACIA_COLETIVA_E_PREVENCAO_ESTRUTURAL");
        }
        return List.copyOf(out);
    }

    private List<String> resolveUnitAnchors(RitoProcessual rito) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (rito.isJuizado()) {
            out.add("JUIZADO");
            out.add("SECRETARIA_JUIZADO");
            out.add("TURMA_RECURSAL");
        } else if (rito.isTrabalhista()) {
            out.add("VARA_TRABALHO");
            out.add("SECRETARIA_TRABALHISTA");
            out.add("TRT");
        } else if (rito.isEleitoral()) {
            out.add("ZONA_ELEITORAL");
            out.add("CARTORIO_ELEITORAL");
            out.add("TRE_TSE");
        } else if (rito.isMilitar()) {
            out.add("AUDITORIA_MILITAR");
            out.add("CONSELHO_JUSTICA");
            out.add("SECRETARIA_MILITAR");
        } else if (rito.isPenal()) {
            out.add("VARA_CRIMINAL");
            out.add("CENTRAL_CUSTODIA");
            out.add("CAMARA_CRIMINAL");
        } else if (rito.isPrevidenciario()) {
            out.add("VARA_FEDERAL_PREVIDENCIARIA");
            out.add("JEF_PREVIDENCIARIO");
            out.add("TRF");
        } else {
            out.add("VARA_COMPETENTE");
            out.add("SECRETARIA_JUDICIAL");
            out.add("GABINETE_RELATORIA");
        }
        return List.copyOf(out);
    }

    private List<String> resolveRequiredDocuments(RitoProcessual rito) {
        ArrayList<String> out = new ArrayList<>();
        out.add("DOCUMENTO_DE_IDENTIFICACAO");
        out.add("INSTRUMENTO_DE_REPRESENTACAO");
        out.add("PROVA_DOCUMENTAL_MINIMA");
        if (rito.isPrevidenciario()) {
            out.add("REQUERIMENTO_ADMINISTRATIVO_OU_DER");
        }
        if (rito.isExecucaoFiscalEstrita()) {
            out.add("CDA_OU_EQUIVALENTE");
        }
        if (rito.isPenal()) {
            out.add("PECAS_DE_MATERIALIDADE_OU_AUTO");
        }
        if (rito.isInternacional()) {
            out.add("DOCUMENTO_ESTRANGEIRO_COM_AUTENTICACAO_COMPATIVEL");
        }
        if (rito.isAutocompositivo()) {
            out.add("MINUTA_BASE_DO_ACORDO_OU_CONFLITO");
        }
        return List.copyOf(out);
    }

    private List<String> resolveWarnings(RitoProcessual rito, NationalProceduralRightsCoverageRow coverage) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (coverage.segredoPadrao()) {
            out.add("RITO_COM_CONTROLE_REFORCADO_DE_SIGILO_E_PUBLICIDADE_MITIGADA");
        }
        if (coverage.exigeMinisterioPublico()) {
            out.add("VERIFICAR_PORTA_DE_ATUACAO_DO_MINISTERIO_PUBLICO");
        }
        if (rito.isInternacional()) {
            out.add("CONFIRMAR_AUTORIDADE_CENTRAL_E_TRADUCAO_CONFORME_TRATADO_APLICAVEL");
        }
        if (coverage.coletivoOuEstrutural()) {
            out.add("REVISAR_LITISCONSORCIO_E_EFICACIA_COLETIVA_ANTES_DO_PROTOCOLO");
        }
        return List.copyOf(out);
    }
}
