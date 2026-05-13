package com.tcc.pjb.backend.service.leilao;

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
public class LeiloeiroJudicialPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;

    public LeiloeiroJudicialPainelService(PerfilDashboardContextFactory contextFactory, PainelServiceCommons commons) {
        this.contextFactory = contextFactory;
        this.commons = commons;
    }

    public PerfilDashboardPayload.LeiloeiroJudicialPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        int leiloes = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "LEILAO", "HASTA")).count();
        int editais = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "EDITAL")).count();
        int contas = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "PRESTACAO", "CONTAS")).count();
        List<String> hastas = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "LEILAO", "HASTA")).limit(8).map(commons::resumo).toList();
        String etag = commons.etag("LEILOEIRO", usuario.getId(), leiloes, editais, contas, hastas, ctx.behavioralAudit());
        return new PerfilDashboardPayload.LeiloeiroJudicialPayload(etag, ctx.generatedAt(), ctx.perfilAtivo(), ctx.tratamento(), ctx.pendencias(), ctx.prazoRadar(), ctx.sessionRisk(), ctx.sigiloAtivo(), ctx.plantao(), ctx.onboarding(), ctx.externalSystems(), ctx.behavioralAudit(), leiloes, editais, contas, hastas);
    }

    public List<Map<String, Object>> listarLeiloesPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(item -> commons.titleContains(item, "LEILAO", "HASTA", "EDITAL")).map(commons::mapWorkItem).toList();
    }

    public List<Map<String, Object>> listarEditaisPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "EDITAL", "PUBLICACAO"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public List<Map<String, Object>> listarPrestacoesContas() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "PRESTACAO", "CONTAS", "DEPOSITO", "REPASSE"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public Map<String, Object> resumoOperacional() {
        Usuario usuario = contextFactory.build().usuario();
        long leiloes = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "LEILAO", "HASTA")).count();
        long editais = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "EDITAL", "PUBLICACAO")).count();
        long contas = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "PRESTACAO", "CONTAS", "DEPOSITO", "REPASSE")).count();
        long urgentes = commons.inboxHibrido(usuario, 20).stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("usuario", usuario.getNome());
        out.put("leiloesPendentes", leiloes);
        out.put("editaisPendentes", editais);
        out.put("prestacoesContasPendentes", contas);
        out.put("itensUrgentes", urgentes);
        out.put("janelaOperacional", urgentes >= 3 ? "CRITICA" : urgentes >= 1 ? "ATENCAO" : "ESTAVEL");
        return out;
    }

}
