package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.processo.anomalia.application.ProcessoAntifraudeOperacionalApplicationService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAntifraudeOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaItem;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.intelligence.ProcessFraudRiskResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessFraudRiskService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoAntifraudeOperacionalApplicationService processoAntifraudeOperacionalApplicationService;
    private final RadarPadroesService radarPadroesService;
    private final DocumentoNacionalValidator documentoNacionalValidator;

    public ProcessFraudRiskService(ProcessoRepository processoRepository,
                                   PjbAuthorizationService authorizationService,
                                   ProcessoAntifraudeOperacionalApplicationService processoAntifraudeOperacionalApplicationService,
                                   RadarPadroesService radarPadroesService,
                                   DocumentoNacionalValidator documentoNacionalValidator) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.processoAntifraudeOperacionalApplicationService = Objects.requireNonNull(processoAntifraudeOperacionalApplicationService);
        this.radarPadroesService = Objects.requireNonNull(radarPadroesService);
        this.documentoNacionalValidator = Objects.requireNonNull(documentoNacionalValidator);
    }

    @Transactional
    public ProcessFraudRiskResponse analyze(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return analyze(processo);
    }

    @Transactional
    public ProcessFraudRiskResponse analyze(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        ProcessoAntifraudeOperacionalAggregate aggregate = processoAntifraudeOperacionalApplicationService.acionar(processo.getId());
        RadarPadroesService.AnaliseRadarResultado radar = radarPadroesService.analisarERegistrar(processo);
        boolean cpfValido = isDocumentValid(processo.getParteAutoraCpf()) && isDocumentValid(processo.getParteReuCpf());
        boolean enderecoSuspeito = containsAny(upper(join(processo.getComarca(), processo.getUf(), processo.getObjetoProcessual(), processo.getResumoIA())), "ENDERECO DESCONHECIDO", "SEM NUMERO", "NAO LOCALIZADO");
        boolean litiganciaMassificada = radar.perfilAutor() != null && radar.perfilAutor().ehLitiganteSerial();
        int score = Math.max(aggregate.scoreGlobal(), (int) Math.round(radar.scoreGeral()));
        if (!cpfValido) {
            score = Math.min(100, score + 18);
        }
        if (enderecoSuspeito) {
            score = Math.min(100, score + 10);
        }
        if (litiganciaMassificada) {
            score = Math.min(100, score + 12);
        }
        ArrayList<ProcessFraudRiskResponse.FraudSignal> sinais = new ArrayList<>();
        for (ProcessoAnomaliaMalhaItem item : aggregate.itensAcionados()) {
            sinais.add(new ProcessFraudRiskResponse.FraudSignal(
                    item.codigo(),
                    item.categoria(),
                    item.nivel(),
                    item.score(),
                    item.titulo(),
                    item.fundamentos()
            ));
        }
        radar.alertas().stream().limit(4).forEach(alerta -> {
            ArrayList<String> fundamentosRadar = new ArrayList<>();
            if (alerta.evidenciasObjetivas() != null && !alerta.evidenciasObjetivas().isBlank()) {
                fundamentosRadar.add(alerta.evidenciasObjetivas());
            }
            if (alerta.orientacaoMagistrado() != null && !alerta.orientacaoMagistrado().isBlank()) {
                fundamentosRadar.add(alerta.orientacaoMagistrado());
            }
            sinais.add(new ProcessFraudRiskResponse.FraudSignal(
                    alerta.tipoPadrao().name(),
                    alerta.tipoPadrao().name(),
                    alerta.nivel().name(),
                    (int) Math.round(alerta.score()),
                    alerta.descricaoTecnica(),
                    List.copyOf(fundamentosRadar)
            ));
        });
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(aggregate.fundamentos());
        fundamentos.add(radar.resumoTecnico());
        if (!cpfValido) {
            fundamentos.add("Há documento pessoal fora do padrão de validação nacional e o caso deve passar por revisão humana.");
        }
        if (enderecoSuspeito) {
            fundamentos.add("Foram identificados sinais textuais de endereçamento frágil ou inconsistente.");
        }
        if (litiganciaMassificada) {
            fundamentos.add("O padrão do polo ativo sugere litigância massificada ou serial relevante para a triagem.");
        }
        ArrayList<String> recomendacoes = new ArrayList<>();
        recomendacoes.add("Submeter o caso à triagem humana antes da distribuição definitiva quando houver score elevado.");
        recomendacoes.add("Conferir documento pessoal, representação processual e endereçamento antes de liberar atos de citação/intimação.");
        if (litiganciaMassificada) {
            recomendacoes.add("Checar identidade da tese, repetição estrutural de petições e compatibilidade do valor da causa.");
        }
        String nivel = score >= 85 ? "CRITICO" : score >= 70 ? "ALTO" : score >= 45 ? "MODERADO" : "CONTROLADO";
        return new ProcessFraudRiskResponse(
                processo.getId(),
                nivel,
                score,
                score >= 70 || !cpfValido,
                cpfValido,
                enderecoSuspeito,
                litiganciaMassificada,
                List.copyOf(sinais.stream().distinct().limit(8).toList()),
                List.copyOf(fundamentos.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).limit(12).toList()),
                List.copyOf(recomendacoes)
        );
    }

    private boolean isDocumentValid(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String digits = value.replaceAll("\\D+", "");
        if (digits.length() == 11) {
            return documentoNacionalValidator.cpfValido(digits);
        }
        if (digits.length() == 14) {
            return documentoNacionalValidator.cnpjValido(digits);
        }
        return false;
    }

    private String join(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(value.trim());
            }
        }
        return sb.toString();
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
