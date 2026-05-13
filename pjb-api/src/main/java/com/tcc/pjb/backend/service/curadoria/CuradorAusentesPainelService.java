package com.tcc.pjb.backend.service.curadoria;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;

@Service
public class CuradorAusentesPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;

    public CuradorAusentesPainelService(PerfilDashboardContextFactory contextFactory, PainelServiceCommons commons) {
        this.contextFactory = contextFactory;
        this.commons = commons;
    }

    public PerfilDashboardPayload.CuradorAusentesPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        int bens = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "BEM", "PATRIMONIO", "INVENTARIO")).count();
        int contas = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "PRESTACAO", "CONTAS")).count();
        int urgentes = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).count();
        List<String> expedientes = commons.inboxHibrido(usuario, 20).stream().limit(8).map(commons::resumo).toList();
        String etag = commons.etag("CURADOR_AUSENTES", usuario.getId(), bens, contas, urgentes, expedientes, ctx.behavioralAudit());
        return new PerfilDashboardPayload.CuradorAusentesPayload(etag, ctx.generatedAt(), ctx.perfilAtivo(), ctx.tratamento(), ctx.pendencias(), ctx.prazoRadar(), ctx.sessionRisk(), ctx.sigiloAtivo(), ctx.plantao(), ctx.onboarding(), ctx.externalSystems(), ctx.behavioralAudit(), bens, contas, urgentes, expedientes);
    }

    public List<Map<String, Object>> listarExpedientesPrioritarios() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().map(commons::mapWorkItem).toList();
    }

    public List<Map<String, Object>> listarBensSobCuradoria() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "BEM", "PATRIMONIO", "INVENTARIO", "ARROLAMENTO"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public List<Map<String, Object>> listarPrestacoesContas() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "PRESTACAO", "CONTAS", "BALANCO", "RELATORIO"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public Map<String, Object> resumoRiscoPatrimonial() {
        Usuario usuario = contextFactory.build().usuario();
        List<?> inbox = commons.inboxHibrido(usuario, 20);
        long bens = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "BEM", "PATRIMONIO", "INVENTARIO", "ARROLAMENTO")).count();
        long urgentes = commons.inboxHibrido(usuario, 20).stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).count();
        long contas = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "PRESTACAO", "CONTAS", "BALANCO", "RELATORIO")).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("usuario", usuario.getNome());
        out.put("comarca", usuario.getComarca());
        out.put("bensSobCuradoria", bens);
        out.put("prestacoesContas", contas);
        out.put("itensUrgentes", urgentes);
        out.put("volumeInbox", inbox.size());
        out.put("nivelRisco", urgentes >= 3 ? "ALTO" : urgentes >= 1 ? "MODERADO" : "CONTROLADO");
        return out;
    }

}
