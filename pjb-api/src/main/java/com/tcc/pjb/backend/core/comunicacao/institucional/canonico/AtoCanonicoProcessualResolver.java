package com.tcc.pjb.backend.core.comunicacao.institucional.canonico;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

@Service
public class AtoCanonicoProcessualResolver {

    private final AtoCanonicoComunicacaoMapper mapper;

    public AtoCanonicoProcessualResolver(AtoCanonicoComunicacaoMapper mapper) {
        this.mapper = mapper;
    }

    public ResolucaoAtoCanonicoResult resolver(ResolucaoAtoCanonicoRequest request) {
        List<ScoredAto> candidatos = new ArrayList<>(10);
        avaliarMpInteresseIncapaz(request, candidatos);
        avaliarMpAcaoColetiva(request, candidatos);
        avaliarMpFalenciaRecuperacao(request, candidatos);
        avaliarDefensoriaCuradoriaEspecial(request, candidatos);
        avaliarFazendaPublica(request, candidatos);
        avaliarEstudoPsicossocial(request, candidatos);
        avaliarConselhoTutelar(request, candidatos);
        avaliarCejusc(request, candidatos);
        avaliarPericia(request, candidatos);
        avaliarPessoaCustodiada(request, candidatos);
        avaliarContadoria(request, candidatos);
        avaliarCartorio(request, candidatos);
        avaliarCooperacao(request, candidatos);
        avaliarOrgaoTecnico(request, candidatos);
        avaliarAdvocaciaPublica(request, candidatos);
        ScoredAto escolhido = candidatos.stream()
                .sorted(Comparator.comparingInt(ScoredAto::score).reversed().thenComparing(scored -> scored.ato().name()))
                .findFirst()
                .orElseGet(() -> new ScoredAto(AtoCanonicoProcessual.NENHUM, 0, List.of("nenhum gatilho canônico obrigatório identificado")));
        PoliticaAtoCanonicoProcessual politica = mapper.resolve(escolhido.ato());
        List<String> justificativas = new ArrayList<>(politica.justificativasPadrao());
        justificativas.addAll(escolhido.reasons());
        String hash = Hashes.sha256Hex(String.join("|",
                String.valueOf(request.processoId()),
                String.valueOf(request.processoNumero()),
                String.valueOf(request.ramoDireito()),
                String.valueOf(request.faseProcessual()),
                String.valueOf(request.classeProcessual()),
                String.valueOf(request.assunto()),
                escolhido.ato().name(),
                String.valueOf(escolhido.score()),
                String.join(";", justificativas)
        ));
        return new ResolucaoAtoCanonicoResult(escolhido.ato(), politica, escolhido.score(), List.copyOf(justificativas), hash);
    }

    private void avaliarMpInteresseIncapaz(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(6);
        if (request.presencaIncapaz()) {
            score += 90;
            reasons.add("presença de incapaz");
        }
        if (request.interesseCriancaAdolescente()) {
            score += 80;
            reasons.add("interesse de criança/adolescente");
        }
        if (request.isFamiliaOuInfancia()) {
            score += 35;
            reasons.add("ramo família/infância");
        }
        if (containsAny(request.corpus(), "divorcio", "divórcio", "guarda", "alimentos", "convivencia", "convivência", "tutela", "regulamentacao de visitas", "regulamentação de visitas")) {
            score += 24;
            reasons.add("matéria típica de família com reflexos em incapaz");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.ABRIR_VISTA_MP_INTERESSE_INCAPAZ, score, reasons);
    }

    private void avaliarMpAcaoColetiva(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(5);
        if (request.demandaColetiva()) {
            score += 95;
            reasons.add("demanda coletiva sinalizada");
        }
        if (containsAny(request.corpus(), "acao civil publica", "ação civil pública", "acao coletiva", "ação coletiva", "mandado de seguranca coletivo", "mandado de segurança coletivo", "interesse difuso", "interesse coletivo", "improbidade")) {
            score += 60;
            reasons.add("texto indica tutela coletiva/interesse público");
        }
        if (request.ramoDireito() == RamoDireito.AMBIENTAL || request.ramoDireito() == RamoDireito.CONSUMIDOR) {
            score += 20;
            reasons.add("ramo com recorrência de tutela coletiva");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.ABRIR_VISTA_MP_ACAO_COLETIVA, score, reasons);
    }

    private void avaliarMpFalenciaRecuperacao(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(5);
        if (request.falenciaOuRecuperacao()) {
            score += 95;
            reasons.add("falência/recuperação explicitada");
        }
        if (containsAny(request.corpus(), "falencia", "falência", "recuperacao judicial", "recuperação judicial", "recuperacao extrajudicial", "recuperação extrajudicial", "administrador judicial")) {
            score += 65;
            reasons.add("texto indica falência ou recuperação");
        }
        if (request.ramoDireito() == RamoDireito.EMPRESARIAL) {
            score += 18;
            reasons.add("ramo empresarial");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.ABRIR_VISTA_MP_FALENCIA_RECUPERACAO, score, reasons);
    }

    private void avaliarDefensoriaCuradoriaEspecial(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(4);
        if (request.curadoriaEspecial()) {
            score += 100;
            reasons.add("curadoria especial explicitada");
        }
        if (containsAny(request.corpus(), "curadoria especial", "reu revel citado por edital", "réu revel citado por edital", "incapaz sem representante", "incapaz sem representante legal")) {
            score += 70;
            reasons.add("texto indica curadoria especial");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.INTIMAR_DEFENSORIA_CURADORIA_ESPECIAL, score, reasons);
    }

    private void avaliarFazendaPublica(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(4);
        if (request.fazendaPublicaNoPolo()) {
            score += 95;
            reasons.add("fazenda pública no polo");
        }
        if (containsAny(request.corpus(), "municipio de", "município de", "estado do", "uniao", "união", "inss", "autarquia", "fazenda publica", "fazenda pública")) {
            score += 40;
            reasons.add("texto indica ente público ou representação fazendária");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.INTIMAR_FAZENDA_PUBLICA_REPRESENTACAO, score, reasons);
    }

    private void avaliarEstudoPsicossocial(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(5);
        if (request.estudoPsicossocialNecessario()) {
            score += 100;
            reasons.add("estudo psicossocial sinalizado");
        }
        if (containsAny(request.corpus(), "estudo psicossocial", "estudo social", "relatorio social", "relatório social", "equipe interdisciplinar", "alienacao parental", "alienação parental", "acolhimento institucional")) {
            score += 70;
            reasons.add("texto indica suporte psicossocial");
        }
        if (request.isFamiliaOuInfancia()) {
            score += 15;
            reasons.add("ramo família/infância" );
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.REQUISITAR_ESTUDO_PSICOSSOCIAL, score, reasons);
    }

    private void avaliarConselhoTutelar(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(5);
        if (request.conselhoTutelarNecessario()) {
            score += 100;
            reasons.add("conselho tutelar sinalizado");
        }
        if (containsAny(request.corpus(), "conselho tutelar", "medida protetiva eca", "medida protetiva eca", "violacao de direitos de crianca", "violação de direitos de criança", "evasao escolar", "evasão escolar")) {
            score += 75;
            reasons.add("texto indica atuação do conselho tutelar");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.COMUNICAR_CONSELHO_TUTELAR, score, reasons);
    }

    private void avaliarCejusc(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(5);
        if (request.derivacaoCejusc()) {
            score += 95;
            reasons.add("derivação CEJUSC sinalizada");
        }
        if (containsAny(request.corpus(), "conciliacao", "conciliação", "mediacao", "mediação", "autocomposicao", "autocomposição", "cejust", "cejusc")) {
            score += 55;
            reasons.add("texto indica autocomposição");
        }
        if (request.ramoDireito() == RamoDireito.CIVIL || request.ramoDireito() == RamoDireito.FAMILIA || request.ramoDireito() == RamoDireito.CONSUMIDOR) {
            score += 12;
            reasons.add("ramo compatível com derivação consensual");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.ENCAMINHAR_CEJUSC, score, reasons);
    }

    private void avaliarPericia(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(4);
        if (request.periciaNecessaria()) {
            score += 100;
            reasons.add("perícia sinalizada");
        }
        if (request.faseProcessual() == FaseProcessual.PERICIA_TECNICA) {
            score += 80;
            reasons.add("fase pericial");
        }
        if (containsAny(request.corpus(), "pericia", "perícia", "perito", "laudo tecnico", "laudo técnico", "insalubridade", "incapacidade laboral", "engenharia", "exame de corpo de delito")) {
            score += 65;
            reasons.add("texto indica prova pericial");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.NOMEAR_PERITO_E_ABRIR_ACEITE, score, reasons);
    }

    private void avaliarPessoaCustodiada(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int apresentacaoScore = 0;
        List<String> apresentacaoReasons = new ArrayList<>(5);
        if (request.reuPresoOuCustodiado()) {
            apresentacaoScore += 80;
            apresentacaoReasons.add("réu preso/custodiado");
        }
        if (containsAny(request.corpus(), "apresentacao do preso", "apresentação do preso", "escolta", "recolhido", "custodiado", "transferencia para audiencia", "transferência para audiência")) {
            apresentacaoScore += 70;
            apresentacaoReasons.add("texto indica requisição de apresentação");
        }
        if (request.audienciaDesignada()) {
            apresentacaoScore += 18;
            apresentacaoReasons.add("audiência designada");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.REQUISITAR_APRESENTACAO_REU_PRESO, apresentacaoScore, apresentacaoReasons);

        int unidadeScore = 0;
        List<String> unidadeReasons = new ArrayList<>(4);
        if (request.reuPresoOuCustodiado()) {
            unidadeScore += 75;
            unidadeReasons.add("réu preso/custodiado");
        }
        if (request.audienciaDesignada()) {
            unidadeScore += 35;
            unidadeReasons.add("audiência designada");
        }
        if (containsAny(request.corpus(), "audiencia", "audiência", "videoconferencia", "videoconferência", "unidade prisional", "presidio", "presídio")) {
            unidadeScore += 40;
            unidadeReasons.add("texto indica comunicação à unidade custodiante");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.COMUNICAR_UNIDADE_PRISIONAL_AUDIENCIA, unidadeScore, unidadeReasons);
    }

    private void avaliarContadoria(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(5);
        if (request.contadoriaJudicialNecessaria()) {
            score += 100;
            reasons.add("contadoria sinalizada");
        }
        if (containsAny(request.corpus(), "calculo", "cálculo", "liquidacao", "liquidação", "contadoria", "planilha de calculo", "planilha de cálculo", "atualizacao do debito", "atualização do débito")) {
            score += 70;
            reasons.add("texto indica apoio de contadoria");
        }
        if (request.faseProcessual() != null && request.faseProcessual().isExecutionLike()) {
            score += 14;
            reasons.add("fase executória/liquidação");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.EXPEDIR_OFICIO_CONTADORIA, score, reasons);
    }

    private void avaliarCartorio(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(5);
        if (request.cartorioExtrajudicialNecessario()) {
            score += 100;
            reasons.add("cartório extrajudicial sinalizado");
        }
        if (containsAny(request.corpus(), "cartorio", "cartório", "registro civil", "registro de imoveis", "registro de imóveis", "averbacao", "averbação", "certidao", "certidão")) {
            score += 68;
            reasons.add("texto indica serventia extrajudicial");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.EXPEDIR_OFICIO_CARTORIO, score, reasons);
    }

    private void avaliarCooperacao(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(4);
        if (request.cooperacaoJudicial()) {
            score += 100;
            reasons.add("cooperação judicial sinalizada");
        }
        if (containsAny(request.corpus(), "carta precatoria", "carta precatória", "juizo deprecado", "juízo deprecado", "cooperacao judicial", "cooperação judicial", "ato concertado")) {
            score += 70;
            reasons.add("texto indica cooperação judicial");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.EXPEDIR_COOPERACAO_JUIZO, score, reasons);
    }

    private void avaliarOrgaoTecnico(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(4);
        if (request.orgaoTecnicoConveniadoNecessario()) {
            score += 100;
            reasons.add("órgão técnico conveniado sinalizado");
        }
        if (containsAny(request.corpus(), "creas", "cras", "caps", "hospital conveniado", "orgao tecnico", "órgão técnico", "nucleo de apoio tecnico", "núcleo de apoio técnico")) {
            score += 60;
            reasons.add("texto indica apoio técnico conveniado");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.COMUNICAR_ORGAO_TECNICO_CONVENIADO, score, reasons);
    }

    private void avaliarAdvocaciaPublica(ResolucaoAtoCanonicoRequest request, List<ScoredAto> candidatos) {
        int score = 0;
        List<String> reasons = new ArrayList<>(4);
        if (request.fazendaPublicaNoPolo() && request.ramoDireito() == RamoDireito.TRIBUTARIO) {
            score += 78;
            reasons.add("tributário com representação institucional pública");
        }
        if (containsAny(request.corpus(), "agu", "procuradoria", "advocacia publica", "advocacia pública", "pgf", "pge", "pgm")) {
            score += 52;
            reasons.add("texto indica advocacia pública");
        }
        addIfPositive(candidatos, AtoCanonicoProcessual.INTIMAR_ADVOCACIA_PUBLICA_REPRESENTACAO, score, reasons);
    }

    private void addIfPositive(List<ScoredAto> candidatos, AtoCanonicoProcessual ato, int score, List<String> reasons) {
        if (score > 0) {
            candidatos.add(new ScoredAto(ato, score, List.copyOf(reasons)));
        }
    }

    private boolean containsAny(String corpus, String... terms) {
        String normalized = corpus == null ? "" : corpus.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private record ScoredAto(AtoCanonicoProcessual ato, int score, List<String> reasons) {
    }
}
