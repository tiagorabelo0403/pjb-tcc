package com.tcc.pjb.backend.service.cidadao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.tcc.pjb.backend.core.prazos.policy.PrazoInteligenteService;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.Links;
import com.tcc.pjb.backend.model.dto.cidadao.PrazoInfoDto;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.service.ui.UiHintService;

@Service
public class CidadaoProcessoCardMapper {

    private final UiHintService ui;
    private final PrazoInteligenteService prazoService;
    private final ProcessoRitoSnapshotService ritoSnapshotService;

    public CidadaoProcessoCardMapper(UiHintService ui,
                                    PrazoInteligenteService prazoService,
                                    ProcessoRitoSnapshotService ritoSnapshotService) {
        this.ui = Objects.requireNonNull(ui);
        this.prazoService = Objects.requireNonNull(prazoService);
        this.ritoSnapshotService = Objects.requireNonNull(ritoSnapshotService);
    }

    public CidadaoProcessoCardDto toCard(Processo p,
                                        MovimentacaoProcessual ultima,
                                        long docCount,
                                        Audiencia nextAud,
                                        JulgamentoColegiado nextJulg) {
        if (p == null || p.getId() == null) {
            return null;
        }

        String ultimaDesc = ultima != null ? ultima.getDescricao() : null;
        LocalDateTime movData = toLocalDateTime(ultima != null ? ultima.getDataMovimentacao() : null);

        List<String> tok = ui.tokenSetForProcess(p).stream().map(Enum::name).toList();

        var ritoSnapshot = ritoSnapshotService.resolve(p, ultimaDesc);
        String ritoCode = ritoSnapshot.ritoCode();
        String ritoTitle = ritoSnapshot.ritoTitle();
        String ramo = ritoSnapshot.ramo();
        Double conf = ritoSnapshot.confidence();
        boolean needsReview = ritoSnapshot.needsReview();
        List<String> reasons = needsReview && !ritoSnapshot.reasons().isEmpty() ? ritoSnapshot.reasons() : null;

        LocalDateTime referencia = movData != null ? movData : p.getDataUltimaMovimentacao();
        var prazoInfo = prazoService.calcularPrazo(p, referencia, ultimaDesc);
        PrazoInfoDto prazoDto = prazoInfo == null ? null : new PrazoInfoDto(
                prazoInfo.dias(),
                prazoInfo.regime() != null ? prazoInfo.regime().name() : null,
                prazoInfo.inicio(),
                prazoInfo.fim(),
                prazoInfo.diasRestantes(),
                prazoInfo.urgente()
        );

        LocalDateTime audData = nextAud != null ? nextAud.getDataHora() : null;
        String audTipo = nextAud != null && nextAud.getTipo() != null ? nextAud.getTipo().name() : null;
        String audModal = nextAud != null && nextAud.getModalidade() != null ? nextAud.getModalidade().name() : null;
        String audLocal = nextAud != null ? nextAud.getLocal() : null;

        LocalDateTime nextJulgData = nextJulg != null ? nextJulg.getPautaDataHora() : null;
        String nextJulgResumo = nextJulg != null ? (
                (nextJulg.getGrau() != null ? nextJulg.getGrau().getLabel() : null) + " - " +
                        (nextJulg.getTribunalSigla() != null ? nextJulg.getTribunalSigla() : "") +
                        (nextJulg.getOrgaoJulgador() != null ? " " + nextJulg.getOrgaoJulgador() : "") +
                        " - " + (nextJulg.getStatus() != null ? nextJulg.getStatus().name() : "")
        ) : null;

        Links links = linksFor(p.getId());

        return new CidadaoProcessoCardDto(
                p.getId(),
                p.getNumeroUnificado(),
                p.getClasseProcessual(),
                p.getAssunto(),
                ritoCode,
                ritoTitle,
                ramo,
                conf,
                needsReview,
                reasons,
                p.getStatusProcesso() != null ? p.getStatusProcesso().name() : null,
                p.getFaseAtual() != null ? p.getFaseAtual().name() : null,
                p.getNivelSigilo() != null ? p.getNivelSigilo().name() : null,
                p.getDataUltimaMovimentacao(),
                tok,
                safeShort(ultimaDesc, 180),
                movData,
                prazoDto,
                audData,
                audTipo,
                audModal,
                audLocal,
                nextJulgData,
                nextJulgResumo,
                docCount,
                links
        );
    }

    public static Links linksFor(Long processoId) {
        return new Links(
                "/api/v1/timeline/processo/" + processoId,
                "/api/v1/cidadao/processos/" + processoId + "/documentos",
                "/api/v1/cidadao/processos/" + processoId + "/documentos/busca",
                "/api/v1/ui/history?processoId=" + processoId,
                "/api/v1/ui/history/stream?processoId=" + processoId,
                "/api/v1/cidadao/processos/" + processoId + "/instancias",
                "/api/v1/cidadao/processos/" + processoId + "/julgamentos"
        );
    }

    private static LocalDateTime toLocalDateTime(Instant i) {
        if (i == null) return null;
        return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    private static String safeShort(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() <= max) return t;
        return t.substring(0, max - 1) + "…";
    }

}
