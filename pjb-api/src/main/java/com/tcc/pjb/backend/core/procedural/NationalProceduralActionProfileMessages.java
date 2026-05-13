package com.tcc.pjb.backend.core.procedural;

import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfileMessages {

    public String mandadoSegurancaReason() {
        return "Vara com competência para mandado de segurança";
    }

    public String mandadoSegurancaLegalBase() {
        return "CF/88 e Lei 12.016/2009";
    }

    public String habeasCorpusReason() {
        return "Órgão com competência originária ou recursal para habeas corpus";
    }

    public String habeasCorpusLegalBase() {
        return "CF/88 e legislação processual específica";
    }

    public String execucaoFiscalReason() {
        return "Vara de Execução Fiscal";
    }

    public String execucaoFiscalLegalBase() {
        return "Lei 6.830/1980";
    }

    public String eleitoralZonaOuTreReason() {
        return "Zona Eleitoral ou TRE conforme competência";
    }

    public String aijeLegalBase() {
        return "LC 64/1990 e Justiça Eleitoral";
    }

    public String aimeLegalBase() {
        return "CF/88, art. 14, §10, e Justiça Eleitoral";
    }

    public String direitoRespostaEleitoralReason() {
        return "Zona Eleitoral ou TRE conforme calendário e competência";
    }


    public String eleitoralRcedReason() {
        return "TRE ou TSE conforme competência recursal eleitoral";
    }

    public String eleitoralRcedLegalBase() {
        return "Código Eleitoral e legislação eleitoral aplicável ao RCED";
    }

    public String eleitoralRcedChecklist() {
        return "Conferir diplomação impugnada, legitimidade ativa e substrato fático-eleitoral específico do RCED.";
    }

    public String eleitoralCaptacaoSufragioReason() {
        return "Zona Eleitoral ou TRE conforme competência para repressão à captação ilícita de sufrágio";
    }

    public String eleitoralCaptacaoSufragioLegalBase() {
        return "Lei 9.504/1997, art. 41-A, e legislação eleitoral";
    }

    public String eleitoralCaptacaoSufragioChecklist() {
        return "Validar prova mínima da captação ilícita de sufrágio, temporalidade eleitoral e nexo com pedido de voto.";
    }

    public String especialDescumprimentoObrigacaoReason() {
        return "Órgão com competência para tutela específica de descumprimento de obrigação";
    }

    public String especialDescumprimentoObrigacaoLegalBase() {
        return "CPC/2015 e legislação especial aplicável";
    }

    public String especialDescumprimentoObrigacaoChecklist() {
        return "Conferir obrigação específica, inadimplemento, prova documental mínima e adequação da tutela pretendida.";
    }

    public String direitoRespostaEleitoralLegalBase() {
        return "Código Eleitoral e legislação eleitoral";
    }

    public String prestacaoContasReason() {
        return "Justiça Eleitoral";
    }

    public String prestacaoContasLegalBase() {
        return "Código Eleitoral e resoluções eleitorais";
    }

    public String eleitoralGeralReason() {
        return "Zona Eleitoral ou TRE conforme competência";
    }

    public String eleitoralGeralLegalBase() {
        return "CF/88, art. 121, Código Eleitoral e legislação eleitoral";
    }

    public String varaTrabalhoSumarissimoReason() {
        return "Vara do Trabalho - rito sumaríssimo";
    }

    public String varaTrabalhoReason() {
        return "Vara do Trabalho";
    }

    public String trabalhistaLegalBase() {
        return "CF/88, art. 114, CLT e Lei 9.957/2000";
    }

    public String trabalhistaSumarissimoAlert() {
        return "Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada.";
    }

    public String trabalhistaSumarissimoChecklist() {
        return "Validar requisitos específicos do rito sumaríssimo trabalhista antes do protocolo.";
    }

    public String trabalhistaPublicEntitySumarissimoAlert() {
        return "Demandas contra Administração Pública direta, autárquica ou fundacional não devem seguir pelo sumaríssimo trabalhista.";
    }

    public String trabalhistaAlcadaReason() {
        return "Vara do Trabalho - rito de alçada";
    }

    public String trabalhistaAlcadaLegalBase() {
        return "Lei 5.584/1970, art. 2º";
    }

    public String trabalhistaAlcadaAlert() {
        return "Rito de alçada trabalhista exige controle do valor da causa, revisão em 48 horas e restrição recursal, salvo matéria constitucional.";
    }

    public String trabalhistaAlcadaChecklist() {
        return "Conferir se o valor da causa não excede duas vezes o salário mínimo vigente e registrar trilha de revisão do valor da alçada.";
    }

    public String trabalhistaInqueritoReason() {
        return "Vara do Trabalho - inquérito judicial para apuração de falta grave";
    }

    public String trabalhistaInqueritoLegalBase() {
        return "CLT, arts. 492, 494 e 853";
    }

    public String trabalhistaInqueritoAlert() {
        return "Inquérito judicial para falta grave depende de estabilidade, suspensão e controle decadencial de 30 dias.";
    }

    public String trabalhistaInqueritoChecklist() {
        return "Validar suspensão do empregado, estabilidade invocada e prazo decadencial contado da suspensão.";
    }

    public String trabalhistaAcaoCumprimentoReason() {
        return "Vara do Trabalho - ação de cumprimento";
    }

    public String trabalhistaAcaoCumprimentoLegalBase() {
        return "CLT, art. 872";
    }

    public String trabalhistaAcaoCumprimentoChecklist() {
        return "Conferir título normativo coletivo, cláusula descumprida e documentação sindical mínima para a ação de cumprimento.";
    }


    public String militarIpmReason() {
        return "Auditoria Militar competente para controle do inquérito policial militar";
    }

    public String militarIpmLegalBase() {
        return "CPPM e legislação da Justiça Militar";
    }

    public String militarIpmChecklist() {
        return "Verificar portaria de instauração, autoridade encarregada, materialidade militar e cadeia de custódia mínima do IPM.";
    }

    public String militarConselhoJusticaReason() {
        return "Conselho de Justiça e Auditoria Militar competentes";
    }

    public String militarConselhoJusticaLegalBase() {
        return "CF/88, CPPM e organização judiciária militar";
    }

    public String militarConselhoJusticaChecklist() {
        return "Conferir composição do conselho, condição militar do acusado e competência castrense para julgamento.";
    }

    public String militarReason() {
        return "Auditoria Militar ou órgão da Justiça Militar competente";
    }

    public String militarLegalBase() {
        return "CF/88, arts. 124 e 125, §4º, e CPPM";
    }

    public String tribunalJuriReason() {
        return "Vara do Tribunal do Júri";
    }

    public String tribunalJuriLegalBase() {
        return "CPP e competência do júri";
    }

    public String execucaoPenalReason() {
        return "Vara de Execuções Penais";
    }

    public String execucaoPenalLegalBase() {
        return "Lei de Execução Penal";
    }

    public String jecrimReason() {
        return "Juizado Especial Criminal";
    }

    public String jecrimLegalBase() {
        return "Lei 9.099/1995";
    }

    public String acaoPenalReason() {
        return "Vara Criminal";
    }

    public String acaoPenalLegalBase() {
        return "CPP";
    }

    public String familiaReason() {
        return "Vara de Família";
    }

    public String familiaLegalBase() {
        return "CPC/2015 e legislação de família";
    }

    public String sucessoesReason() {
        return "Vara de Sucessões ou Família e Sucessões";
    }

    public String sucessoesLegalBase() {
        return "CPC/2015";
    }

    public String usucapiaoReason() {
        return "Vara Cível com competência imobiliária";
    }

    public String usucapiaoLegalBase() {
        return "CPC/2015 e legislação civil";
    }

    public String civelReason() {
        return "Vara Cível";
    }

    public String civelLegalBase() {
        return "CPC/2015";
    }

    public String empresarialReason() {
        return "Vara Empresarial ou de Falências";
    }

    public String empresarialLegalBase() {
        return "Lei 11.101/2005";
    }


    public String administrativoPadReason() {
        return "Vara da Fazenda Pública ou órgão com competência judicial para controle disciplinar";
    }

    public String administrativoPadLegalBase() {
        return "Lei 8.112/1990, estatutos disciplinares correlatos e devido processo administrativo";
    }

    public String administrativoPadAlert() {
        return "PAD exige controle de competência instauradora, comissão processante, contraditório, ampla defesa e prescrição disciplinar.";
    }

    public String administrativoPadChecklist() {
        return "Conferir portaria de instauração, composição da comissão, ciência do acusado, tipificação disciplinar e cronologia mínima dos atos.";
    }

    public String infanciaAdocaoReason() {
        return "Vara da Infância e Juventude para habilitação e adoção";
    }

    public String infanciaAdocaoLegalBase() {
        return "ECA, CF/88 art. 227 e legislação de adoção";
    }

    public String infanciaAdocaoChecklist() {
        return "Validar habilitação, estudo psicossocial, estágio de convivência e documentação civil mínima da criança e dos pretendentes.";
    }

    public String infanciaInfracionalReason() {
        return "Vara da Infância e Juventude para apuração de ato infracional";
    }

    public String infanciaInfracionalLegalBase() {
        return "ECA e legislação processual socioeducativa aplicável";
    }

    public String infanciaInfracionalAlert() {
        return "Ato infracional exige sigilo reforçado, prioridade absoluta e observância da escuta protegida e das garantias socioeducativas.";
    }

    public String infanciaInfracionalChecklist() {
        return "Conferir representação ou peça equivalente, idade do adolescente, prova mínima do ato infracional e trilha socioeducativa inicial.";
    }

    public String infanciaProtecaoReason() {
        return "Vara da Infância e Juventude para medidas protetivas e tutela integral";
    }

    public String infanciaProtecaoLegalBase() {
        return "ECA e CF/88 art. 227";
    }

    public String infanciaProtecaoAlert() {
        return "Medidas da infância exigem prioridade absoluta, sigilo adequado e proteção integral sem exposição indevida do menor.";
    }

    public String infanciaProtecaoChecklist() {
        return "Validar risco atual, relatórios da rede protetiva, manifestação do conselho tutelar ou equipe técnica e urgência concreta da medida.";
    }

    public String infanciaTutelaMenorReason() {
        return "Vara da Infância e Juventude para guarda, tutela ou curatela de menor";
    }

    public String infanciaTutelaMenorLegalBase() {
        return "ECA, Código Civil e CPC/2015 conforme a medida de proteção ou representação do menor";
    }

    public String infanciaTutelaMenorChecklist() {
        return "Conferir vínculo com o menor, estudo social disponível, certidão de nascimento e motivo protetivo ou representativo da tutela pretendida.";
    }

    public String improbidadeReason() {
        return "Vara da Fazenda Pública ou órgão com competência especializada";
    }

    public String improbidadeLegalBase() {
        return "Lei 8.429/1992 e CPC/2015";
    }

    public String improbidadeJuizadoAlert() {
        return "Improbidade não tramita em juizado especial.";
    }

    public String acaoCivilPublicaReason() {
        return "Vara Cível, Fazenda Pública ou especializada conforme o objeto";
    }

    public String acaoCivilPublicaLegalBase() {
        return "Lei 7.347/1985 e CPC/2015";
    }

    public String desapropriacaoReason() {
        return "Vara Agrária, Federal ou Fazenda Pública conforme o ente e o imóvel";
    }

    public String desapropriacaoLegalBase() {
        return "CPC/2015 e legislação de desapropriação";
    }

    public String desapropriacaoJuizadoAlert() {
        return "Desapropriação é matéria típica fora do sistema dos juizados especiais.";
    }


    public String previdenciarioBpcReason() {
        return "Trilha previdenciária assistencial para BPC/LOAS";
    }

    public String previdenciarioBpcChecklist() {
        return "Conferir deficiência ou idade, miserabilidade e documentação socioassistencial mínima do BPC/LOAS.";
    }

    public String previdenciarioAuxilioReason() {
        return "Trilha previdenciária de incapacidade laboral";
    }

    public String previdenciarioAuxilioChecklist() {
        return "Validar incapacidade, carência, qualidade de segurado e suporte médico-pericial mínimo.";
    }

    public String previdenciarioAposentadoriaReason() {
        return "Trilha previdenciária de aposentadoria";
    }

    public String previdenciarioAposentadoriaChecklist() {
        return "Conferir tempo de contribuição, DER, qualidade de segurado e CNIS/documentação equivalente.";
    }

    public String previdenciarioRevisaoReason() {
        return "Trilha previdenciária revisional";
    }

    public String previdenciarioRevisaoChecklist() {
        return "Validar benefício originário, tese revisional, memória mínima do cálculo e marco temporal relevante.";
    }

    public String previdenciarioRestabelecimentoReason() {
        return "Trilha previdenciária de restabelecimento de benefício";
    }

    public String previdenciarioRestabelecimentoChecklist() {
        return "Conferir cessação indevida, manutenção dos requisitos e prova mínima da interrupção do benefício.";
    }

    public String previdenciarioSalarioMaternidadeReason() {
        return "Trilha previdenciária de salário-maternidade";
    }

    public String previdenciarioSalarioMaternidadeChecklist() {
        return "Conferir evento gerador, qualidade de segurada e documentação civil/previdenciária mínima.";
    }

    public String previdenciarioPensaoReason() {
        return "Trilha previdenciária de pensão por morte";
    }

    public String previdenciarioPensaoChecklist() {
        return "Validar óbito, qualidade de segurado do instituidor e dependência econômica quando exigível.";
    }

    public String previdenciarioRuralReason() {
        return "Trilha previdenciária rural";
    }

    public String previdenciarioRuralChecklist() {
        return "Conferir início de prova material rural, período de carência e enquadramento do segurado especial.";
    }

    public String previdenciarioEspecialReason() {
        return "Trilha previdenciária de aposentadoria especial";
    }

    public String previdenciarioEspecialChecklist() {
        return "Validar exposição a agentes nocivos, PPP/LTCAT e período especial convertido ou reconhecido.";
    }

    public String previdenciarioRppsReason() {
        return "Trilha previdenciária de regime próprio";
    }

    public String previdenciarioRppsChecklist() {
        return "Conferir regime próprio aplicável, vínculo estatutário e ato administrativo previdenciário correlato.";
    }

    public String previdenciarioJefReason() {
        return "Juizado Especial Federal ou Vara Federal Previdenciária";
    }

    public String previdenciarioComumReason() {
        return "Vara Federal Previdenciária";
    }

    public String previdenciarioLegalBase() {
        return "CF/88, art. 109, e legislação previdenciária";
    }

    public String fazendaReason() {
        return "Vara da Fazenda Pública";
    }

    public String fazendaLegalBase() {
        return "CPC/2015, CF/88 e legislação fazendária";
    }

    public String fazendaServidorChecklist() {
        return "Verificar necessidade de vara fazendária especializada em servidor ou concurso público.";
    }

    public String consumoReason() {
        return "Vara Cível ou Juizado Especial Cível";
    }

    public String consumoLegalBase() {
        return "CPC/2015, CDC e legislação especial";
    }
}
