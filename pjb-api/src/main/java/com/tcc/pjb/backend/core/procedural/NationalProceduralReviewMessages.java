package com.tcc.pjb.backend.core.procedural;

import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewMessages {

    public String preProtocolSanitizationRequired() {
        return "Pré-protocolo nacional exige saneamento adicional antes da distribuição assistida.";
    }

    public String territorialMeshNeedsHumanValidation() {
        return "Malha territorial e unidade julgadora demandam validação humana complementar.";
    }

    public String heuristicCompatibilityAlert() {
        return "Rito fechado por compatibilidade heurística controlada.";
    }

    public String heuristicCompatibilityChecklist() {
        return "Conferir aderência do rito escolhido ao pedido e ao órgão jurisdicional.";
    }

    public String residualRitoAlert() {
        return "Fluxo caiu em rito residual; revisão manual obrigatória.";
    }

    public String residualRitoBlocking() {
        return "Rito residual fechado sem aderência plena; revisão humana obrigatória antes do protocolo.";
    }

    public String sanityGateAlert(Collection<String> statusCodes) {
        String suffix = statusCodes == null || statusCodes.isEmpty() ? "sem códigos detalhados" : String.join(", ", statusCodes);
        return "Gate procedural detectou inconsistências estruturais: " + suffix;
    }

    public String sanityGateChecklist() {
        return "Sanear classe, rito, ramo e tribunal antes do protocolo.";
    }

    public String sanityGateBlocking() {
        return "Gate procedural marcou inconsistências bloqueantes na combinação entre classe, rito, ramo e tribunal.";
    }

    public String tetoBlockingAlert(String suggested) {
        return nonBlankOrDefault(suggested, "Valor da causa incompatível com a trilha econômica selecionada.");
    }

    public String tetoBlockingChecklist() {
        return "Revisar memória de cálculo e eventual renúncia ao excedente quando juridicamente cabível.";
    }

    public String tetoBlockingReason(String fundamento) {
        return nonBlankOrDefault(fundamento, "Valor da causa incompatível com o rito econômico selecionado.");
    }

    public String tetoWarningAlert() {
        return "Valor da causa próximo ao teto operacional do rito sugerido.";
    }

    public String tetoWarningChecklist() {
        return "Validar se a alçada econômica permanece aderente ao rito especial pretendido.";
    }

    public String missingClasseBlocking() {
        return "Classe processual ausente para fechamento seguro da rota procedimental.";
    }

    public String missingObjetoBlocking() {
        return "Objeto processual insuficiente para consolidar competência material e vara sugerida.";
    }

    public String missingPedidoBlocking() {
        return "Pedido principal ausente para fechamento do rito, da alçada e da competência.";
    }

    public String missingValorBlocking() {
        return "Valor da causa ausente para validar aderência econômica do procedimento selecionado.";
    }

    public String distributionFallbackAlert() {
        return "Distribuição dinâmica não retornou unidade cadastrada; manter sugestão de família de vara e revisar malha local.";
    }

    public String publicPartySpecializedChecklist() {
        return "Verificar se a especialização fazendária ou administrativa local exige vara exclusiva.";
    }

    public String linkageReviewChecklist() {
        return "Conferir distribuição por dependência, prevenção, conexão ou continência antes do protocolo final.";
    }


    public String missingZonaOuMunicipioEleitoral() {
        return "zonaOuMunicipioEleitoral";
    }

    public String eleitoralZonaBlocking() {
        return "Justiça Eleitoral exige zona eleitoral ou município-base do pleito para fechar competência e distribuição.";
    }

    public String missingPleitoAno() {
        return "anoPleito";
    }

    public String eleitoralPleitoAnoBlocking() {
        return "Ano ou ciclo do pleito ausente para distinguir competência eleitoral e calendário processual.";
    }

    public String missingCargoOuMandato() {
        return "cargoOuMandatoEleitoral";
    }

    public String eleitoralCargoBlocking() {
        return "Cargo disputado ou mandato impugnado ausente para diferenciar zona eleitoral, TRE ou TSE.";
    }

    public String missingAtoEleitoral() {
        return "atoEleitoralImpugnado";
    }

    public String eleitoralAtoBlocking() {
        return "Ato eleitoral impugnado não foi descrito com precisão suficiente para classificar a ação e o rito eleitoral.";
    }

    public String missingEscopoJusticaMilitar() {
        return "escopoJusticaMilitar";
    }

    public String militarEscopoBlocking() {
        return "É necessário indicar se o caso pertence à Justiça Militar da União ou à Justiça Militar Estadual.";
    }

    public String missingCorporacaoOuForca() {
        return "corporacaoOuForcaMilitar";
    }

    public String militarCorporacaoBlocking() {
        return "Corporação, força ou organização militar não identificada; a competência militar permanece aberta.";
    }

    public String missingCondicaoMilitarOuCivil() {
        return "condicaoMilitarOuCivil";
    }

    public String militarCondicaoAgenteBlocking() {
        return "A condição do agente como militar, oficial, praça ou civil é necessária para fechar a competência militar.";
    }

    public String missingLocalFatoMilitar() {
        return "localDoFatoMilitar";
    }

    public String militarLocalFatoBlocking() {
        return "Local do fato militar ausente para definição de circunscrição, auditoria ou prevenção.";
    }

    public String missingNaturezaFatoMilitar() {
        return "naturezaDoFatoMilitar";
    }

    public String militarNaturezaBlocking() {
        return "É necessário indicar se o fato militar é penal, disciplinar ou constitucional para definir a trilha adequada.";
    }

    public String missingLocalPrestacaoServicos() {
        return "localPrestacaoServicos";
    }

    public String trabalhistaPrestacaoBlocking() {
        return "Justiça do Trabalho exige o local da prestação dos serviços como âncora territorial principal do ajuizamento.";
    }

    public String missingNaturezaEmpregador() {
        return "naturezaDoEmpregador";
    }

    public String trabalhistaNaturezaEmpregadorBlocking() {
        return "A natureza do empregador deve ser conhecida para diferenciar rito, competência e eventual exclusão de trilhas especiais.";
    }

    public String missingCategoriaOuSindicato() {
        return "categoriaOuSindicato";
    }

    public String trabalhistaColetivoBlocking() {
        return "Dissídio ou demanda coletiva trabalhista exige categoria econômica ou profissional e representação sindical claramente identificadas.";
    }

    public String missingLocalFatoPenal() {
        return "localDoFatoPenal";
    }

    public String penalLocalFatoBlocking() {
        return "Competência penal depende do local do fato e não pode ser fechada com segurança sem essa informação.";
    }

    public String missingNaturezaInfracao() {
        return "naturezaDaInfracao";
    }

    public String penalNaturezaBlocking() {
        return "A natureza da infração penal ou do procedimento criminal precisa estar descrita para definir rito e unidade competente.";
    }

    public String missingProcedimentoInvestigatorio() {
        return "procedimentoInvestigatorioOuOrigem";
    }

    public String penalProcedimentoBlocking() {
        return "Número ou referência do procedimento investigatório, notícia-crime ou origem penal é necessário para prevenção e vinculação segura.";
    }

    public String missingInteresseFederalPenal() {
        return "interesseFederalPenal";
    }

    public String penalFederalBlocking() {
        return "Na trilha penal federal, o interesse federal concreto deve ser identificado antes da distribuição.";
    }

    public String missingEnteInteresseFederal() {
        return "enteOuInteresseFederal";
    }

    public String federalInterestBlocking() {
        return "A Justiça Federal exige indicação do ente ou interesse federal que atrai a competência constitucional.";
    }

    private String nonBlankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
