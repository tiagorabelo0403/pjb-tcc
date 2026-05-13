package com.tcc.pjb.backend.ai.skills.v1;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.skills.IASkill;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Locale;

@Slf4j
@Component
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class SkillAnaliseNivelProcessualV1 implements IASkill {

    private static final Pattern REGEX_PROCESSO =
            Pattern.compile("\\b\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}\\b|\\b\\d{10,20}\\b");

    @Override
    public boolean suporta(IARequest request) {
        if (request == null) return false;
        String acao = Objects.toString(request.getAcao(), "");
        return acao.toUpperCase(Locale.ROOT).contains("ANALISE_CONTEXTO");
    }

    @Override
    public IAResponse executar(IARequest request, Map<String, Object> context) {

        String textoOriginal = request.getSafeString("texto");

        if (textoOriginal == null || textoOriginal.isBlank()) {
            return IAResponse.builder()
                    .origem(getNome())
                    .status(IAResponse.StatusIA.ALERTA)
                    .texto("Texto não informado para análise contextual.")
                    .confianca(0.0)
                    .dataGeracao(Instant.now())
                    .build();
        }

        String texto = textoOriginal.toLowerCase(Locale.ROOT);

        IntencaoDetectada intencao = identificarIntencao(texto);
        String numeroProcesso = extrairNumeroProcesso(textoOriginal);

        double confianca = numeroProcesso != null
                ? 0.95
                : intencao.getConfiancaBase();

        Map<String, Object> metadados = new HashMap<>();
        metadados.put("intencao", intencao.name());

        if (numeroProcesso != null) {
            metadados.put("numero_processo", numeroProcesso);
        }

        return IAResponse.builder()
                .origem(getNome())
                .status(IAResponse.StatusIA.SUCESSO)
                .texto("Intenção identificada: " + intencao.getDescricao())
                .confianca(confianca)
                .metadados(metadados)
                .dataGeracao(Instant.now())
                .build();
    }

    @Override
    public String getNome() {
        return "SKILL_ANALISE_CONTEXTUAL_V1";
    }

    

    private IntencaoDetectada identificarIntencao(String texto) {
        return Arrays.stream(IntencaoDetectada.values())
                .filter(i -> i.match(texto))
                .findFirst()
                .orElse(IntencaoDetectada.OUTROS);
    }

    private String extrairNumeroProcesso(String texto) {
        Matcher matcher = REGEX_PROCESSO.matcher(texto);
        return matcher.find() ? matcher.group() : null;
    }

    @Getter
    @RequiredArgsConstructor
    private enum IntencaoDetectada {

        CONSULTA_PROCESSO("Consulta Processual", 0.9,
                List.of("andamento", "meu processo", "sentença")),

        CALCULO_VALORES("Cálculo", 0.85,
                List.of("calcular", "valor", "rmi")),

        DOCUMENTOS("Documentos", 0.8,
                List.of("laudo", "perícia", "atestado")),

        OUTROS("Indefinido", 0.3, List.of());

        private final String descricao;
        private final double confiancaBase;
        private final List<String> palavrasChave;

        public boolean match(String texto) {
            return palavrasChave.stream().anyMatch(texto::contains);
        }
    }
}
