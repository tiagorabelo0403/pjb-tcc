package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

@Service
public class PoliticaEntregaInstitucionalService {

    public PlanoEntregaInstitucional resolver(ResolucaoRoteamentoInstitucionalRequest request,
                                              UnidadeInstitucional unidade,
                                              TipoComunicacaoJudicial tipoComunicacaoEfetiva) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(unidade, "unidade");
        Objects.requireNonNull(tipoComunicacaoEfetiva, "tipoComunicacaoEfetiva");

        List<CanalEntregaInstitucional> juridicos = unidade.canais().stream()
                .filter(canal -> canal.canal().isPrincipalJuridico())
                .toList();
        List<String> justificativas = new ArrayList<>();
        CanalEntregaInstitucional principal = resolvePreferred(request, tipoComunicacaoEfetiva, juridicos, justificativas);
        List<CanalEntregaInstitucional> fallbacks = juridicos.stream()
                .filter(canal -> canal.canal() != principal.canal())
                .sorted(Comparator.comparingInt(canal -> prioridade(request, tipoComunicacaoEfetiva, canal.canal())))
                .toList();
        boolean avisoInformativo = unidade.canais().stream().anyMatch(CanalEntregaInstitucional::isCanalAviso);
        boolean forcarOficial = principal.canal() == CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL;
        boolean forcarDigital = principal.canal() != CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL;
        return new PlanoEntregaInstitucional(
                principal,
                List.copyOf(fallbacks),
                avisoInformativo,
                forcarDigital,
                forcarOficial,
                List.copyOf(justificativas)
        );
    }

    private CanalEntregaInstitucional resolvePreferred(ResolucaoRoteamentoInstitucionalRequest request,
                                                       TipoComunicacaoJudicial tipoComunicacaoEfetiva,
                                                       List<CanalEntregaInstitucional> juridicos,
                                                       List<String> justificativas) {
        if (request.canalPreferencial() != null) {
            CanalEntregaInstitucional preferred = juridicos.stream()
                    .filter(canal -> canal.canal() == request.canalPreferencial())
                    .findFirst()
                    .orElse(null);
            if (preferred != null && isCompativel(request, tipoComunicacaoEfetiva, preferred.canal())) {
                justificativas.add("canalPreferencial=" + preferred.canal().name());
                return preferred;
            }
        }
        LinkedHashSet<CanalComunicacaoInstitucional> priorityOrder = buildPriorityOrder(request, tipoComunicacaoEfetiva);
        for (CanalComunicacaoInstitucional candidate : priorityOrder) {
            CanalEntregaInstitucional match = juridicos.stream()
                    .filter(canal -> canal.canal() == candidate)
                    .filter(canal -> isCompativel(request, tipoComunicacaoEfetiva, canal.canal()))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                justificativas.add("canalResolvido=" + match.canal().name());
                return match;
            }
        }
        CanalEntregaInstitucional fallback = juridicos.isEmpty()
                ? new CanalEntregaInstitucional(CanalComunicacaoInstitucional.PJB_INBOX, true, request.exigeCienciaPessoal(), 48, 120, null, "fallback sintético")
                : juridicos.getFirst();
        justificativas.add("canalFallback=" + fallback.canal().name());
        return fallback;
    }

    private LinkedHashSet<CanalComunicacaoInstitucional> buildPriorityOrder(ResolucaoRoteamentoInstitucionalRequest request,
                                                                            TipoComunicacaoJudicial tipoComunicacaoEfetiva) {
        LinkedHashSet<CanalComunicacaoInstitucional> priority = new LinkedHashSet<>();
        boolean exigePessoalidade = request.exigeCienciaPessoal() || tipoComunicacaoEfetiva.isExigePessoalidade();
        if (tipoComunicacaoEfetiva.isCitacao()) {
            priority.add(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO);
            priority.add(CanalComunicacaoInstitucional.PJB_INBOX);
            priority.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
            priority.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
            return priority;
        }
        if (tipoComunicacaoEfetiva == TipoComunicacaoJudicial.COMUNICACAO_COOPERACAO_NACIONAL) {
            priority.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
            priority.add(CanalComunicacaoInstitucional.PJB_INBOX);
            priority.add(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO);
            priority.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
            return priority;
        }
        if (tipoComunicacaoEfetiva == TipoComunicacaoJudicial.INTIMACAO_PUBLICA_DJE && !exigePessoalidade) {
            priority.add(CanalComunicacaoInstitucional.DJEN);
            priority.add(CanalComunicacaoInstitucional.PJB_INBOX);
            priority.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
            priority.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
            return priority;
        }
        if (exigePessoalidade) {
            priority.add(CanalComunicacaoInstitucional.PJB_INBOX);
            priority.add(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO);
            priority.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
            priority.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
            return priority;
        }
        priority.add(CanalComunicacaoInstitucional.PJB_INBOX);
        priority.add(CanalComunicacaoInstitucional.DJEN);
        priority.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
        priority.add(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO);
        priority.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
        return priority;
    }

    private boolean isCompativel(ResolucaoRoteamentoInstitucionalRequest request,
                                 TipoComunicacaoJudicial tipoComunicacaoEfetiva,
                                 CanalComunicacaoInstitucional canal) {
        if (canal == CanalComunicacaoInstitucional.DJEN) {
            return !request.exigeCienciaPessoal() && !tipoComunicacaoEfetiva.isExigePessoalidade() && tipoComunicacaoEfetiva.isIntimacao();
        }
        if (canal == CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO) {
            return tipoComunicacaoEfetiva.isCitacao() || request.exigeCienciaPessoal() || tipoComunicacaoEfetiva.isExigePessoalidade();
        }
        if (canal == CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL) {
            return !tipoComunicacaoEfetiva.isEdital();
        }
        if (canal == CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL) {
            return true;
        }
        return true;
    }

    private int prioridade(ResolucaoRoteamentoInstitucionalRequest request,
                           TipoComunicacaoJudicial tipoComunicacaoEfetiva,
                           CanalComunicacaoInstitucional canal) {
        int index = 0;
        for (CanalComunicacaoInstitucional candidate : buildPriorityOrder(request, tipoComunicacaoEfetiva)) {
            if (candidate == canal) {
                return index;
            }
            index++;
        }
        return 999;
    }
}
