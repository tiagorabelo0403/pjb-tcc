package com.tcc.pjb.backend.model.entity.enums.processual;

/**
 * Vocabulário canônico de tipos de documento processual do PJB.
 *
 * <p>Cada constante declara sua {@link CategoriaDocumento}. Não há padrão TPU/CNJ para
 * tipo-de-documento; este enum é o vocabulário interno autoritativo do sistema.
 *
 * <p>Reconciliações em relação ao vocabulário legado:
 * <ul>
 *   <li>{@code DOCUMENTO_IDENTIDADE} unifica o antigo {@code DOCUMENTO_IDENTIFICACAO} do catálogo
 *       e {@code DOCUMENTO_IDENTIDADE} do tipo legado — nome canônico: {@code DOCUMENTO_IDENTIDADE}.</li>
 *   <li>{@code COMPROVANTE_ENDERECO} substitui {@code COMPROVANTE_RESIDENCIA} do tipo legado —
 *       endereço é o conceito correto para competência territorial e intimações.</li>
 *   <li>{@code LAUDO_PERICIAL} e {@code LAUDO_MEDICO} permanecem distintos: laudos periciais
 *       de qualquer natureza técnica vs. laudos especificamente médico-clínicos.</li>
 * </ul>
 */
public enum TipoDocumento {

    /** Peça inaugural que abre o processo judicial (art. 319 CPC). */
    PETICAO_INICIAL(CategoriaDocumento.PECA_INAUGURAL),

    /** Requerimento de instauração em procedimentos autocompositivos. */
    REQUERIMENTO_INICIAL(CategoriaDocumento.PECA_INAUGURAL),

    /** Documento de identidade da parte (RG, CNH, passaporte). */
    DOCUMENTO_IDENTIDADE(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Instrumento de procuração para representação processual. */
    PROCURACAO(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Declaração de hipossuficiência para fins de assistência judiciária gratuita. */
    DECLARACAO_HIPOSSUFICIENCIA(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Carteira de Trabalho e Previdência Social — qualificação do vínculo laboral. */
    CTPS(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Comprovante de endereço para fins de competência territorial e intimações. */
    COMPROVANTE_ENDERECO(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Ato constitutivo da pessoa jurídica (contrato social, estatuto). */
    ATO_CONSTITUTIVO(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Documentos pessoais genéricos de qualificação dos envolvidos. */
    DOCUMENTOS_PESSOAIS(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Documentos pessoais do candidato em processo eleitoral. */
    DOCUMENTOS_PESSOAIS_CANDIDATO(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Habilitação do pretendente à adoção ou status no cadastro de adoção. */
    HABILITACAO_ADOTANTE(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Qualificação formal do adolescente em ato infracional. */
    QUALIFICACAO_ADOLESCENTE(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Assentamentos funcionais do militar investigado ou acusado. */
    ASSENTAMENTOS_FUNCIONAIS(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Dados dos responsáveis legais para intimação e acompanhamento. */
    RESPONSAVEIS_LEGAIS(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Comprovação de filiação partidária. */
    FILIACAO_PARTIDARIA(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Qualificação do paciente em habeas corpus. */
    DOCUMENTO_PACIENTE(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Qualificação da autoridade coatora em mandado de segurança. */
    DOCUMENTO_AUTORIDADE_COATORA(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Certidão de nascimento — identificação civil. */
    CERTIDAO_NASCIMENTO(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Certidão de óbito — identificação civil post mortem. */
    CERTIDAO_OBITO(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Ficha funcional do servidor público. */
    FICHA_FUNCIONAL(CategoriaDocumento.DOC_QUALIFICACAO),

    /** Contrato de trabalho que comprova a relação contratual. */
    CONTRATO_TRABALHO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Prova pré-constituída que demonstra o direito invocado. */
    PROVA_PRE_CONSTITUIDA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Conjunto de provas documentais básicas de lastro mínimo da causa. */
    PROVAS_DOCUMENTAIS_BASICAS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Mídia probatória — suporte audiovisual ou digital. */
    MIDIA_PROBATORIA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cadeia de custódia digital — integridade da prova eletrônica. */
    CADEIA_CUSTODIA_DIGITAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Laudo pericial técnico (qualquer especialidade). */
    LAUDO_PERICIAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Laudo médico-clínico específico (saúde, capacidade, acidente). */
    LAUDO_MEDICO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Laudo técnico ambiental que demonstra o dano ambiental. */
    LAUDO_TECNICO_AMBIENTAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Laudo socioambiental em contexto coletivo e territorial. */
    LAUDO_SOCIOAMBIENTAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Rol de testemunhas para instrução probatória. */
    ROL_TESTEMUNHAS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Quesitação sugerida para organização do plenário do júri. */
    QUESITACAO_SUGERIDA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Boletim de ocorrência — notícia-crime ou fato policial. */
    BOLETIM_OCORRENCIA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Auto de prisão em flagrante delito. */
    AUTO_PRISAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Guia de execução penal e cálculo de pena. */
    GUIA_EXECUCAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Atestado de conduta carcerária — avaliação para progressão ou benefícios. */
    ATESTADO_CONDUTA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Boletim disciplinar de acompanhamento da execução penal. */
    BOLETIM_DISCIPLINAR(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cálculo de pena — remição, progressão e controle. */
    CALCULO_PENA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Liquidação estimada dos pedidos iniciais. */
    CALCULO_INICIAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Memória de cálculo do valor da causa. */
    MEMORIA_CALCULO_VALOR_CAUSA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Memória do valor da causa para fins de fixação da alçada trabalhista. */
    MEMORIA_VALOR_CAUSA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ata de audiência como suporte de revisão ou instrução. */
    ATA_AUDIENCIA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentos que comprovam liquidez dos pedidos e valor da alçada reduzida. */
    DOCUMENTOS_LIQUIDEZ_PEDIDOS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentos que comprovam a estabilidade do empregado. */
    DOCUMENTO_ESTABILIDADE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ato de suspensão do empregado com marco temporal. */
    ATO_SUSPENSAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Lastro documental da falta grave imputada. */
    PROVA_FALTA_GRAVE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Contagem do prazo decadencial de 30 dias no inquérito de falta grave. */
    CONTROLE_PRAZO_DECADENCIAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Convenção, acordo ou sentença normativa — instrumento coletivo. */
    INSTRUMENTO_COLETIVO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cláusula do instrumento coletivo descumprida. */
    CLAUSULA_DESCUMPRIDA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentação mínima do inadimplemento coletivo. */
    PROVAS_DESCUMPRIMENTO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentos sindicais de representação coletiva ou substituição processual. */
    DOCUMENTOS_SINDICAIS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ata de assembleia sindical que deliberou sobre o dissídio. */
    ATA_ASSEMBLEIA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Pauta de reivindicações do dissídio coletivo. */
    PAUTA_REIVINDICACOES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Comprovação de tentativa de negociação prévia. */
    COMPROVACAO_NEGOCIACAO_PREVIA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Instrumentos coletivos anteriores para contextualização histórica. */
    INSTRUMENTOS_COLETIVOS_ANTERIORES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cadastro Nacional de Informações Sociais — histórico contributivo previdenciário. */
    CNIS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Requerimento administrativo prévio. */
    REQUERIMENTO_ADMINISTRATIVO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Carta de indeferimento administrativo. */
    CARTA_INDEFERIMENTO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Exames médicos ou laboratoriais de apoio pericial. */
    EXAMES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentos de comprovação de labor rural. */
    DOCUMENTOS_RURAIS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ato administrativo ou tributário objeto da impugnação. */
    ATO_ADMINISTRATIVO_OU_TRIBUTARIO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Auto de infração ou Certidão de Dívida Ativa — título fiscal. */
    AUTO_INFRACAO_OU_CDA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Planilha de débito — memória do crédito fiscal. */
    PLANILHA_DEBITO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Comprovante de recolhimento — prova de pagamento ou depósito. */
    COMPROVANTE_RECOLHIMENTO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Portaria de instauração formal do procedimento. */
    PORTARIA_INSTAURACAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Designação da comissão processante ou disciplinar. */
    DESIGNACAO_COMISSAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Designação da autoridade competente. */
    DESIGNACAO_AUTORIDADE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Termo de citação do acusado no PAD. */
    TERMO_CITACAO_ACUSADO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Peças instrutórias do processo administrativo disciplinar. */
    PECAS_INSTRUTORIAS_PAD(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cronologia dos atos do PAD. */
    CRONOLOGIA_DISCIPLINAR(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ato final ou decisão impugnada da autoridade disciplinar. */
    DECISAO_AUTORIDADE_DISCIPLINAR(CategoriaDocumento.DOC_INSTRUCAO),

    /** Edital do concurso público — regra vinculante do certame. */
    EDITAL_CONCURSO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ato impugnado em concurso público (resultado, eliminação, indeferimento). */
    ATO_IMPUGNADO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Comprovantes de inscrição e resultado no certame. */
    COMPROVANTES_INSCRICAO_RESULTADO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Espelho de correção ou notas do concurso. */
    ESPELHO_CORRECAO_OU_NOTAS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cronologia do certame concursal. */
    CRONOLOGIA_CERTAME(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ato funcional do servidor questionado. */
    ATO_FUNCIONAL_IMPUGNADO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Contracheques e documentos de reflexos remuneratórios. */
    COMPROVANTES_REMUNERATORIOS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Planilha de valores funcionais ou remuneratórios. */
    PLANILHA_VALORES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Mídia digital — arquivos eletrônicos de prova ou contas. */
    MIDIA_DIGITAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ata notarial ou hash — integridade da prova digital eleitoral. */
    ATA_NOTARIAL_OU_HASH(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cronologia dos fatos eleitorais. */
    CRONOLOGIA_FATOS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Mídia da propaganda eleitoral impugnada. */
    MIDIA_PROPAGANDA(CategoriaDocumento.DOC_INSTRUCAO),

    /** URL identificada da propaganda eleitoral. */
    URL_IDENTIFICADA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Pedido de tutela de urgência fundamentado (liminar, antecipação). */
    PEDIDO_LIMINAR_FUNDAMENTADO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Pedido liminar quando houver urgência em habeas corpus. */
    PEDIDO_LIMINAR(CategoriaDocumento.DOC_INSTRUCAO),

    /** Pedido de tutela de urgência em representação eleitoral. */
    PEDIDO_TUTELA_URGENTE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Demonstrativo de regularidade do partido ou federação (DRAP). */
    DRAP(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ata de convenção partidária que escolheu o candidato. */
    ATA_CONVENCAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Certidão de quitação eleitoral — condição de elegibilidade. */
    CERTIDAO_QUITACAO_ELEITORAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Certidões criminais para verificação de inelegibilidades. */
    CERTIDOES_CRIMINAIS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Certidões de elegibilidade ou inelegibilidade. */
    CERTIDOES_ELEGIBILIDADE(CategoriaDocumento.DOC_INSTRUCAO),

    /** DRAP e documentos do registro de candidatura — contexto da AIRC. */
    DRAP_E_DOCUMENTOS_CANDIDATURA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cronologia eleitoral dos fatos impugnados. */
    CRONOLOGIA_ELEITORAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Relatório analítico de organização técnica dos fatos. */
    RELATORIO_ANALITICO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Diploma ou mandato — comprovação de diplomação ou exercício. */
    DIPLOMA_OU_MANDATO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Extratos bancários de movimentação financeira da campanha. */
    EXTRATOS_BANCARIOS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Recibos eleitorais de receita e despesa. */
    RECIBOS_ELEITORAIS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Relação consolidada de receitas e despesas da campanha. */
    RELACAO_RECEITAS_DESPESAS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Notas fiscais de suporte dos gastos eleitorais. */
    NOTAS_FISCAIS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Parecer técnico das contas eleitorais. */
    PARECER_TECNICO_CONTAS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Parecer técnico de suporte material à causa. */
    PARECER_TECNICO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Peças essenciais do inquérito policial militar (IPM). */
    PECAS_IPM(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ata dos atos disciplinares apuratórios no PAD militar. */
    ATA_ATOS_DISCIPLINARES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Balanços e demonstrações financeiras da empresa em recuperação ou falência. */
    BALANCOS_DEMONSTRACOES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Relação de credores — quadro geral da recuperação ou falência. */
    RELACAO_CREDORES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Fluxo de caixa — planejamento econômico empresarial. */
    FLUXO_CAIXA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Plano de recuperação judicial ou extrajudicial. */
    PLANO_RECUPERACAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Contratos essenciais para continuidade empresarial. */
    CONTRATOS_ESSENCIAIS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Boletim de ocorrência ou representação de ato infracional. */
    BOLETIM_OU_REPRESENTACAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Relatório socioeducativo — subsídio técnico inicial em ato infracional. */
    RELATORIO_SOCIOEDUCATIVO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Estudo psicossocial em adoção ou ato infracional. */
    ESTUDO_PSICOSSOCIAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Relatório de estágio de convivência em adoção. */
    RELATORIO_ESTAGIO_CONVIVENCIA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Estudo social ou avaliação protetiva sociofamiliar. */
    ESTUDO_SOCIAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Comprovante de vínculo familiar ou responsabilidade invocada. */
    COMPROVANTE_VINCULO_FAMILIAR(CategoriaDocumento.DOC_INSTRUCAO),

    /** Parecer do Conselho Tutelar com informações protetivas. */
    PARECER_CONSELHO_TUTELAR(CategoriaDocumento.DOC_INSTRUCAO),

    /** Matrícula do imóvel — titularidade ou registro rural. */
    MATRICULA_IMOVEL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Mapa georreferenciado para delimitação da área agrária. */
    MAPA_GEOREFERENCIADO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cadeia dominial — histórico da propriedade rural. */
    CADEIA_DOMINIAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentos de comprovação possessória. */
    DOCUMENTOS_POSSE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Auto de infração ambiental — contexto administrativo. */
    AUTO_INFRACAO_AMBIENTAL(CategoriaDocumento.DOC_INSTRUCAO),

    /** Mapa da área afetada pelo dano ambiental. */
    MAPA_AREA_AFETADA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Fotografias e mídias de registro visual do dano. */
    FOTOGRAFIAS_MIDIAS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Plano de recuperação da área degradada. */
    PLANO_RECUPERACAO_AREA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Tradução juramentada de documentos estrangeiros. */
    TRADUCAO_JURAMENTADA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Decisão estrangeira para homologação pelo STJ. */
    DECISAO_ESTRANGEIRA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Carta rogatória — pedido de cooperação jurídica internacional. */
    CARTA_ROGATORIA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Prova de autenticidade na cooperação internacional. */
    PROVA_AUTENTICIDADE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Comprovante de citação ou garantias do contraditório no exterior. */
    COMPROVANTE_CITACAO_OU_DEFESA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Base consensual do procedimento autocompositivo. */
    CONVENCAO_ARBITRAGEM_OU_TERMO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Proposta de acordo — material negocial. */
    PROPOSTA_ACORDO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentos de suporte em mediação ou arbitragem. */
    DOCUMENTOS_SUPORTE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Prova da cidadania ativa — legitimidade em ação popular. */
    PROVA_CIDADANIA_ATIVA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Documentação administrativa complementar. */
    DOCUMENTOS_ADMINISTRATIVOS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Legislação pertinente — normas questionadas ou aplicáveis. */
    LEGISLACAO_PERTINENTE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Peças relevantes para exame da coação em habeas corpus. */
    PECAS_RELEVANTES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Ato coator identificado em mandado de segurança. */
    ATO_COATOR(CategoriaDocumento.DOC_INSTRUCAO),

    /** Decisão coatora impugnada em habeas corpus. */
    DECISAO_COATORA(CategoriaDocumento.DOC_INSTRUCAO),

    /** Demonstração do constrangimento ilegal em habeas corpus. */
    PROVA_ILEGALIDADE(CategoriaDocumento.DOC_INSTRUCAO),

    /** Holerites — prova de remuneração. */
    HOLERITES(CategoriaDocumento.DOC_INSTRUCAO),

    /** Cartão-ponto — prova de jornada de trabalho. */
    CARTAO_PONTO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Título executivo extrajudicial. */
    TITULO_EXECUTIVO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Contrato de locação. */
    CONTRATO_LOCACAO(CategoriaDocumento.DOC_INSTRUCAO),

    /** Certidão negativa de débitos. */
    CERTIDAO_NEGATIVA_DEBITOS(CategoriaDocumento.DOC_INSTRUCAO),

    /** Relatório escolar e médico — evidências complementares em procedimentos de infância e juventude. */
    RELATORIO_ESCOLAR_MEDICO(CategoriaDocumento.DOC_INSTRUCAO);

    private final CategoriaDocumento categoria;

    TipoDocumento(CategoriaDocumento categoria) {
        this.categoria = categoria;
    }

    public CategoriaDocumento categoria() {
        return categoria;
    }
}
