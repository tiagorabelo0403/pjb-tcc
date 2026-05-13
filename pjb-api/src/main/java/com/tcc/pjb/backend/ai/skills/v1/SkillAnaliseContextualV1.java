package com.tcc.pjb.backend.ai.skills.v1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.skills.IASkill;

@Component
public class SkillAnaliseContextualV1 implements IASkill {

    private static final Pattern REGEX_PROCESSO =
            Pattern.compile("\\b\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}\\b|\\b\\d{10,20}\\b");
    private static final Pattern REGEX_DATA =
            Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b|\\b\\d{1,2}-\\d{1,2}-\\d{2,4}\\b");

    @Override
    public boolean suporta(IARequest request) {
        return suportaAcao(request, getAcaoSuportada());
    }

    @Override
    public IAResponse executar(IARequest request, Map<String, Object> contexto) {
        String texto = request != null ? request.getSafeString("texto") : null;
        if (texto == null || texto.isBlank()) {
            return IAResponse.builder()
                    .origem(getNome())
                    .status(IAResponse.StatusIA.INDETERMINADO)
                    .texto("Texto não informado para análise contextual.")
                    .confianca(0.0)
                    .dataGeracao(Instant.now())
                    .build();
        }

        ContextoDetectado contextoDetectado = analisarContexto(texto);
        return IAResponse.builder()
                .origem(getNome())
                .status(contextoDetectado.status())
                .texto(contextoDetectado.resumo())
                .confianca(contextoDetectado.confianca())
                .dataGeracao(Instant.now())
                .metadados(contextoDetectado.metadados())
                .build();
    }

    @Override
    public String getNome() {
        return "SKILL_ANALISE_CONTEXTUAL_V1";
    }

    private ContextoDetectado analisarContexto(String textoOriginal) {
        String texto = normalizar(textoOriginal);
        String numeroProcesso = extrairNumeroProcesso(textoOriginal);
        List<String> datas = extrairDatas(textoOriginal);
        Set<MarcadorContextual> marcadores = detectarMarcadores(texto);
        IntencaoContextual intencao = IntencaoContextual.detectar(texto, marcadores, numeroProcesso != null);
        double confianca = calcularConfianca(numeroProcesso != null, datas, marcadores, intencao);
        IAResponse.StatusIA status = marcadores.contains(MarcadorContextual.URGENCIA)
                ? IAResponse.StatusIA.ALERTA
                : IAResponse.StatusIA.SUCESSO;

        Map<String, Object> metadados = new LinkedHashMap<>();
        metadados.put("possui_numero_processo", numeroProcesso != null);
        metadados.put("intencao", intencao.name());
        metadados.put("intencao_label", intencao.descricao());
        metadados.put("marcadores", marcadores.stream().map(Enum::name).toList());
        metadados.put("quantidade_marcadores", marcadores.size());
        metadados.put("quantidade_datas", datas.size());
        if (numeroProcesso != null) {
            metadados.put("numero_processo", numeroProcesso);
        }
        if (!datas.isEmpty()) {
            metadados.put("datas_detectadas", List.copyOf(datas));
        }

        return new ContextoDetectado(
                status,
                construirResumo(intencao, numeroProcesso, datas, marcadores),
                confianca,
                Map.copyOf(metadados)
        );
    }

    private Set<MarcadorContextual> detectarMarcadores(String texto) {
        LinkedHashSet<MarcadorContextual> encontrados = new LinkedHashSet<>();
        for (MarcadorContextual marcador : MarcadorContextual.values()) {
            if (marcador.detecta(texto)) {
                encontrados.add(marcador);
            }
        }
        return Set.copyOf(encontrados);
    }

    private List<String> extrairDatas(String texto) {
        Matcher matcher = REGEX_DATA.matcher(texto);
        ArrayList<String> datas = new ArrayList<>();
        while (matcher.find()) {
            String data = matcher.group();
            if (!datas.contains(data)) {
                datas.add(data);
            }
        }
        return List.copyOf(datas);
    }

    private String extrairNumeroProcesso(String texto) {
        Matcher matcher = REGEX_PROCESSO.matcher(texto);
        return matcher.find() ? matcher.group() : null;
    }

    private double calcularConfianca(boolean possuiNumeroProcesso,
                                     List<String> datas,
                                     Set<MarcadorContextual> marcadores,
                                     IntencaoContextual intencao) {
        double confianca = intencao.confiancaBase();
        if (possuiNumeroProcesso) {
            confianca += 0.22;
        }
        if (!datas.isEmpty()) {
            confianca += 0.06;
        }
        confianca += Math.min(0.18, marcadores.size() * 0.04);
        return Math.min(0.99, confianca);
    }

    private String construirResumo(IntencaoContextual intencao,
                                   String numeroProcesso,
                                   List<String> datas,
                                   Set<MarcadorContextual> marcadores) {
        List<String> partes = new ArrayList<>();
        partes.add("Análise contextual concluída");
        partes.add("intenção predominante: " + intencao.descricao().toLowerCase(Locale.ROOT));
        if (numeroProcesso != null) {
            partes.add("processo identificado");
        }
        if (!datas.isEmpty()) {
            partes.add("marcos temporais detectados");
        }
        if (!marcadores.isEmpty()) {
            partes.add("sinais: " + marcadores.stream()
                    .map(MarcadorContextual::descricao)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList());
        }
        return String.join("; ", partes) + ".";
    }

    private String normalizar(String texto) {
        return Objects.toString(texto, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private enum MarcadorContextual {
        URGENCIA("urgência", List.of("urgente", "liminar", "plantão", "tutela", "perigo de dano")),
        AUDIENCIA("audiência", List.of("audiência", "sessão", "instrução", "conciliação")),
        DECISAO("decisão", List.of("sentença", "decisão", "despacho", "acórdão")),
        RECURSAL("recursal", List.of("recurso", "apelação", "agravo", "embargos")),
        PRAZO("prazo", List.of("prazo", "intimação", "intimado", "publicação", "tempestivo")),
        PARTICIPANTES("participantes", List.of("autor", "réu", "requerente", "requerido", "advogado", "promotor", "defensor"));

        private final String descricao;
        private final List<String> gatilhos;

        MarcadorContextual(String descricao, List<String> gatilhos) {
            this.descricao = descricao;
            this.gatilhos = List.copyOf(gatilhos);
        }

        String descricao() {
            return descricao;
        }

        boolean detecta(String texto) {
            return gatilhos.stream().anyMatch(texto::contains);
        }
    }

    private enum IntencaoContextual {
        CONSULTA_PROCESSUAL("Consulta processual", 0.72),
        ANALISE_DECISORIA("Análise decisória", 0.76),
        CONTROLE_PRAZO("Controle de prazo", 0.78),
        MOVIMENTO_RECURSAL("Movimento recursal", 0.8),
        GESTAO_AUDIENCIA("Gestão de audiência", 0.74),
        CONTEXTO_GERAL("Contexto geral", 0.58);

        private final String descricao;
        private final double confiancaBase;

        IntencaoContextual(String descricao, double confiancaBase) {
            this.descricao = descricao;
            this.confiancaBase = confiancaBase;
        }

        String descricao() {
            return descricao;
        }

        double confiancaBase() {
            return confiancaBase;
        }

        static IntencaoContextual detectar(String texto,
                                           Set<MarcadorContextual> marcadores,
                                           boolean possuiNumeroProcesso) {
            if (marcadores.contains(MarcadorContextual.RECURSAL)) {
                return MOVIMENTO_RECURSAL;
            }
            if (marcadores.contains(MarcadorContextual.PRAZO) || texto.contains("tempest")) {
                return CONTROLE_PRAZO;
            }
            if (marcadores.contains(MarcadorContextual.DECISAO)) {
                return ANALISE_DECISORIA;
            }
            if (marcadores.contains(MarcadorContextual.AUDIENCIA)) {
                return GESTAO_AUDIENCIA;
            }
            if (possuiNumeroProcesso || texto.contains("processo") || texto.contains("autos")) {
                return CONSULTA_PROCESSUAL;
            }
            return CONTEXTO_GERAL;
        }
    }

    private record ContextoDetectado(IAResponse.StatusIA status,
                                     String resumo,
                                     double confianca,
                                     Map<String, Object> metadados) {
    }
}
