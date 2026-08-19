package com.tcc.pjb.backend.service.prazo;

import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.model.dto.prazo.PrazoCartorioItemResponse;
import com.tcc.pjb.backend.model.dto.prazo.PrazoCartorioPainelResponse;
import com.tcc.pjb.backend.model.dto.prazo.PrazoCertidaoDecursoItemResponse;
import com.tcc.pjb.backend.model.dto.prazo.PrazoCertidaoDecursoLoteResponse;
import com.tcc.pjb.backend.model.dto.prazo.PrazoCertidaoTempestividadeResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrazoCartorioPainelService {

    private static final int LIMITE_ITENS = 500;

    private final CienciaProcessualRepository cienciaProcessualRepository;
    private final ProcessoRepository processoRepository;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;

    public PrazoCartorioPainelService(CienciaProcessualRepository cienciaProcessualRepository,
                                      ProcessoRepository processoRepository,
                                      ProcessoPrazoApplicationService processoPrazoApplicationService) {
        this.cienciaProcessualRepository = Objects.requireNonNull(cienciaProcessualRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
    }

    @Transactional(readOnly = true)
    public PrazoCartorioPainelResponse painelPorVara(String vara, int diasJanela) {
        Objects.requireNonNull(vara, "vara");
        Instant agora = Instant.now();
        Instant ateData = agora.plus(Math.max(1, diasJanela), ChronoUnit.DAYS);
        List<CienciaProcessual> pendentes = cienciaProcessualRepository.findPendentesPorVaraAteData(
                vara, ateData, PageRequest.of(0, LIMITE_ITENS));

        long vencidos = 0;
        long vencendoEm7 = 0;
        long vencendoEm15 = 0;
        List<PrazoCartorioItemResponse> itens = pendentes.stream()
                .map(ciencia -> {
                    boolean vencido = ciencia.getDataExpiracao().isBefore(agora);
                    long diasRestantes = vencido ? 0 : ChronoUnit.DAYS.between(agora, ciencia.getDataExpiracao());
                    return new PrazoCartorioItemResponse(
                            ciencia.getId(),
                            ciencia.getProcesso() == null ? null : ciencia.getProcesso().getId(),
                            ciencia.getNumeroProcesso(),
                            ciencia.getTipoCiencia() == null ? null : ciencia.getTipoCiencia().name(),
                            ciencia.getDataExpiracao(),
                            diasRestantes,
                            vencido);
                })
                .toList();

        for (PrazoCartorioItemResponse item : itens) {
            if (item.vencido()) {
                vencidos++;
                vencendoEm7++;
                vencendoEm15++;
                continue;
            }
            if (item.diasRestantes() <= 7) {
                vencendoEm7++;
            }
            if (item.diasRestantes() <= 15) {
                vencendoEm15++;
            }
        }

        return new PrazoCartorioPainelResponse(vara, itens.size(), vencidos, vencendoEm7, vencendoEm15, itens);
    }

    @Transactional
    public PrazoCertidaoDecursoLoteResponse certificarDecursoEmLote(String vara) {
        Objects.requireNonNull(vara, "vara");
        Instant agora = Instant.now();
        List<CienciaProcessual> vencidas = cienciaProcessualRepository.findPendentesPorVaraAteData(
                vara, agora, PageRequest.of(0, LIMITE_ITENS));

        List<PrazoCertidaoDecursoItemResponse> certidoes = new ArrayList<>();
        for (CienciaProcessual ciencia : vencidas) {
            ciencia.expirar(agora);
            if (!ciencia.isFicta()) {
                continue;
            }
            certidoes.add(new PrazoCertidaoDecursoItemResponse(
                    ciencia.getId(),
                    ciencia.getProcesso() == null ? null : ciencia.getProcesso().getId(),
                    ciencia.getNumeroProcesso(),
                    montarTextoCertidao(ciencia),
                    agora));
        }
        cienciaProcessualRepository.saveAll(vencidas);
        return new PrazoCertidaoDecursoLoteResponse(vara, certidoes.size(), List.copyOf(certidoes));
    }

    @Transactional(readOnly = true)
    public PrazoCertidaoTempestividadeResponse certificarTempestividade(Long processoId,
                                                                        NationalPrazoEngine.TipoPrazo tipoPrazo,
                                                                        LocalDate dataPratica) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(tipoPrazo, "tipoPrazo");
        Objects.requireNonNull(dataPratica, "dataPratica");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoPrazoMarco marco = processoPrazoApplicationService.calcular(processoId, tipoPrazo);
        boolean tempestivo = !dataPratica.isAfter(marco.vencimento());
        String texto = montarTextoTempestividade(processo, marco, dataPratica, tempestivo);
        return new PrazoCertidaoTempestividadeResponse(
                processoId,
                processo.getNumeroProcesso(),
                marco.codigo(),
                marco.titulo(),
                marco.vencimento(),
                dataPratica,
                tempestivo,
                texto,
                Instant.now());
    }

    private String montarTextoTempestividade(Processo processo, ProcessoPrazoMarco marco, LocalDate dataPratica, boolean tempestivo) {
        return "CERTIDÃO DE " + (tempestivo ? "TEMPESTIVIDADE" : "INTEMPESTIVIDADE")
                + " — processo " + processo.getNumeroProcesso()
                + ". Certifico que o ato relativo a \"" + marco.titulo() + "\" foi praticado em " + dataPratica
                + ", " + (tempestivo ? "dentro do" : "fora do") + " prazo legal, que venceu em " + marco.vencimento()
                + ". Fundamento: " + String.join("; ", marco.fundamentos()) + ".";
    }

    private String montarTextoCertidao(CienciaProcessual ciencia) {
        String tipo = ciencia.getTipoCiencia() == null ? "prazo processual" : ciencia.getTipoCiencia().label();
        return "CERTIDÃO DE DECURSO DE PRAZO — processo " + ciencia.getNumeroProcesso()
                + ". Certifico, para os devidos fins, o decurso do prazo de " + tipo
                + " sem manifestação da parte, com vencimento em " + ciencia.getDataExpiracao()
                + ", nos termos do art. 231 do CPC.";
    }
}
