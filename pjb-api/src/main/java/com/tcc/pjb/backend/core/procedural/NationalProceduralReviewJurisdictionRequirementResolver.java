package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewJurisdictionRequirementResolver {

    private final NationalProceduralReviewMessages messages;

    public NationalProceduralReviewJurisdictionRequirementResolver(NationalProceduralReviewMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralReviewInputSlice assess(NationalProceduralReviewSynthesisContext context) {
        Objects.requireNonNull(context);
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        LinkedHashSet<String> missingInputs = new LinkedHashSet<>();
        LinkedHashSet<String> blockingIssues = new LinkedHashSet<>();
        String rito = normalized(context.selectedRito() != null && context.selectedRito().rito() != null ? context.selectedRito().rito().name() : null);
        String actionNature = normalized(context.actionProfile() != null ? context.actionProfile().actionNature() : null);
        String actionFamily = normalized(context.actionProfile() != null ? context.actionProfile().actionFamily() : null);
        String corpus = normalized(NationalProceduralRoutingSupport.buildCorpus(payload));
        TipoJustica tipoJustica = context.tipoJustica();

        if (isEleitoral(tipoJustica, rito, actionFamily, corpus)) {
            require(missingInputs, blockingIssues, payload, messages.missingZonaOuMunicipioEleitoral(), messages.eleitoralZonaBlocking(),
                    "zonaEleitoral", "numeroZonaEleitoral", "municipioPleito", "cidadeFato", "municipioFato", "municipioEleitoral", "municipioCandidatura", "localVotacao", "zonaDoTitulo", "zonaDoPleito");
            require(missingInputs, blockingIssues, payload, messages.missingPleitoAno(), messages.eleitoralPleitoAnoBlocking(),
                    "anoPleito", "eleicaoAno", "anoEleitoral", "anoEleicao", "cicloEleitoral", "dataPleito");
            require(missingInputs, blockingIssues, payload, messages.missingCargoOuMandato(), messages.eleitoralCargoBlocking(),
                    "cargoOuMandato", "cargoDisputado", "mandatoImpugnado", "cargoEmDisputa", "mandatoEmDiscussao", "cargoDoCandidato");
            require(missingInputs, blockingIssues, payload, messages.missingAtoEleitoral(), messages.eleitoralAtoBlocking(),
                    "atoEleitoral", "atoEleitoralImpugnado", "tipoAcao", "classe", "classeProcessual", "descricaoProblemaEleitoral", "fatoEleitoralNarrado", "tipoIrregularidadeEleitoral");
        }

        if (isMilitar(tipoJustica, rito, actionFamily, corpus)) {
            require(missingInputs, blockingIssues, payload, messages.missingEscopoJusticaMilitar(), messages.militarEscopoBlocking(),
                    "escopoJusticaMilitar", "justicaMilitarEscopo", "militarEsfera", "forcaMilitarEhDaUniaoOuEstado", "casoMilitarDaUniaoOuDoEstado", "justicaMilitarEhUniaoOuEstadual");
            require(missingInputs, blockingIssues, payload, messages.missingCorporacaoOuForca(), messages.militarCorporacaoBlocking(),
                    "corporacaoOuForca", "forcaMilitar", "corporacaoMilitar", "orgaoMilitar", "policiaMilitarEstado", "corpoBombeirosMilitar", "organizacaoMilitar");
            require(missingInputs, blockingIssues, payload, messages.missingCondicaoMilitarOuCivil(), messages.militarCondicaoAgenteBlocking(),
                    "condicaoMilitarOuCivil", "condicaoAgente", "eMilitar", "agenteMilitar", "agenteEraMilitarOuCivil", "postoGraduacaoOuFuncao", "qualidadeDoAgente");
            require(missingInputs, blockingIssues, payload, messages.missingLocalFatoMilitar(), messages.militarLocalFatoBlocking(),
                    "localDoFatoMilitar", "cidadeFato", "municipioFato", "cidadeAutor", "cidadeOcorrencia", "municipioOcorrencia", "localOcorrenciaMilitar");
            require(missingInputs, blockingIssues, payload, messages.missingNaturezaFatoMilitar(), messages.militarNaturezaBlocking(),
                    "naturezaDoFatoMilitar", "tipoAcao", "classe", "classeProcessual", "tipoProblemaMilitar", "fatoAconteceuEmServicoMilitar", "contextoMilitarDoFato");
        }

        if (isTrabalhista(tipoJustica, rito, actionFamily, corpus)) {
            require(missingInputs, blockingIssues, payload, messages.missingLocalPrestacaoServicos(), messages.trabalhistaPrestacaoBlocking(),
                    "localPrestacaoServicos", "cidadeFato", "municipioFato", "cidadeAutor", "cidadeTrabalho", "municipioTrabalho", "enderecoDoTrabalho", "localOndeTrabalhava");
            require(missingInputs, blockingIssues, payload, messages.missingNaturezaEmpregador(), messages.trabalhistaNaturezaEmpregadorBlocking(),
                    "naturezaDoEmpregador", "empregadorNatureza", "entePublico", "parteReuNome", "empresaEhPublicaOuPrivada", "enteEmpregador", "empregadorEhEntePublico");
            if (containsAny(rito, "DISSIDIO") || containsAny(actionNature, "DISSIDIO")) {
                require(missingInputs, blockingIssues, payload, messages.missingCategoriaOuSindicato(), messages.trabalhistaColetivoBlocking(),
                        "sindicatoOuCategoria", "categoriaProfissional", "categoriaEconomica", "sindicatoAutor", "categoriaDaClasse", "sindicatoDaCategoria", "categoriaProfissionalOuEconomica");
            }
        }

        if (isPenal(tipoJustica, rito, actionFamily, corpus)) {
            require(missingInputs, blockingIssues, payload, messages.missingLocalFatoPenal(), messages.penalLocalFatoBlocking(),
                    "localDoFatoPenal", "cidadeFato", "municipioFato", "cidadeOcorrencia", "municipioOcorrencia", "localOcorrencia", "ondeCrimeAconteceu");
            require(missingInputs, blockingIssues, payload, messages.missingNaturezaInfracao(), messages.penalNaturezaBlocking(),
                    "naturezaDaInfracao", "tipificacaoSugerida", "tipoAcao", "classe", "classeProcessual", "tipoCrimeOuFatoCriminal", "descricaoFatoCriminal", "qualCrimeOuInfracao");
            require(missingInputs, blockingIssues, payload, messages.missingProcedimentoInvestigatorio(), messages.penalProcedimentoBlocking(),
                    "procedimentoInvestigatorioOuOrigem", "numeroInquerito", "numeroApf", "numeroProcedimentoOrigem", "boletimOcorrenciaNumero", "delegaciaOrigem", "numeroNoticiaCrime");
            if (tipoJustica == TipoJustica.FEDERAL) {
                require(missingInputs, blockingIssues, payload, messages.missingInteresseFederalPenal(), messages.penalFederalBlocking(),
                        "interesseFederalOuResidualEstadual", "enteOuInteresseFederal", "orgaoFederalLesado", "crimeContraOrgaoFederal", "bemServicoInteresseUniao");
            }
        }

        if (tipoJustica == TipoJustica.FEDERAL && !isTrabalhista(tipoJustica, rito, actionFamily, corpus) && !isEleitoral(tipoJustica, rito, actionFamily, corpus)) {
            require(missingInputs, blockingIssues, payload, messages.missingEnteInteresseFederal(), messages.federalInterestBlocking(),
                    "enteOuInteresseFederal", "orgaoFederal", "autarquiaFederal", "empresaPublicaFederal", "interesseFederalOuResidualEstadual", "enteFederalEnvolvido", "autarquiaFederalEnvolvida", "orgaoFederalEnvolvido", "motivoFederal");
        }

        return new NationalProceduralReviewInputSlice(List.copyOf(missingInputs), List.copyOf(blockingIssues));
    }

    private void require(LinkedHashSet<String> missingInputs,
                         LinkedHashSet<String> blockingIssues,
                         Map<String, Object> payload,
                         String missingCode,
                         String blockingMessage,
                         String... keys) {
        if (!hasAnyText(payload, keys)) {
            missingInputs.add(missingCode);
            blockingIssues.add(blockingMessage);
        }
    }

    private boolean hasAnyText(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (!NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(payload.get(key)))) {
                return true;
            }
        }
        return false;
    }

    private boolean isEleitoral(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return tipoJustica == TipoJustica.ELEITORAL || containsAny(rito, "ELEITORAL", "AIRC", "AIJE", "AIME", "RCED") || containsAny(actionFamily, "ELEITORAL") || containsAny(corpus, "ELEITORAL", "PLEITO", "ZONA ELEITORAL");
    }

    private boolean isMilitar(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL || containsAny(rito, "MILITAR", "IPM", "CONSELHO_JUSTICA") || containsAny(actionFamily, "MILITAR") || containsAny(corpus, "JUSTICA MILITAR", "AUDITORIA MILITAR", "STM", "TJM");
    }

    private boolean isTrabalhista(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return tipoJustica == TipoJustica.TRABALHO || containsAny(rito, "TRABALHISTA", "DISSIDIO") || containsAny(actionFamily, "TRABALHISTA") || containsAny(corpus, "TRABALHO", "TRT", "TST", "VARA DO TRABALHO");
    }

    private boolean isPenal(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return containsAny(rito, "PENAL", "CRIMINAL", "JURI", "CUSTODIA", "MARIA_DA_PENHA")
                || containsAny(actionFamily, "PENAL")
                || containsAny(corpus, "CRIME", "INQUERITO", "DENUNCIA", "REU")
                || (tipoJustica == TipoJustica.MILITAR_FEDERAL && containsAny(rito, "HABEAS", "PENAL"));
    }

    private static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(value, keys);
    }

    private static String normalized(String value) {
        return NationalProceduralRoutingSupport.normalize(value);
    }
}
