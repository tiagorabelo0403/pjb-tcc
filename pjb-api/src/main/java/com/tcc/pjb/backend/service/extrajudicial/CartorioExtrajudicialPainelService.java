package com.tcc.pjb.backend.service.extrajudicial;

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
public class CartorioExtrajudicialPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;

    public CartorioExtrajudicialPainelService(PerfilDashboardContextFactory contextFactory, PainelServiceCommons commons) {
        this.contextFactory = contextFactory;
        this.commons = commons;
    }

    public PerfilDashboardPayload.CartorioExtrajudicialPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<String> atos = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "AVERBACAO", "CERTIDAO", "INDISPONIBILIDADE", "PENHORA")).limit(8).map(commons::resumo).toList();
        int certidoes = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "CERTIDAO")).count();
        int indisponibilidades = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "INDISPONIBILIDADE", "CNIB")).count();
        int averbacoes = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "AVERBACAO", "REGISTRO")).count();
        String etag = commons.etag("CARTORIO", usuario.getId(), certidoes, indisponibilidades, averbacoes, atos, ctx.behavioralAudit());
        return new PerfilDashboardPayload.CartorioExtrajudicialPayload(etag, ctx.generatedAt(), ctx.perfilAtivo(), ctx.tratamento(), ctx.pendencias(), ctx.prazoRadar(), ctx.sessionRisk(), ctx.sigiloAtivo(), ctx.plantao(), ctx.onboarding(), ctx.externalSystems(), ctx.behavioralAudit(), usuario.getComarca(), usuario.getTipoUsuario().name(), certidoes, indisponibilidades, averbacoes, atos);
    }

    public List<Map<String, Object>> listarAtosPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(item -> commons.titleContains(item, "AVERBACAO", "CERTIDAO", "INDISPONIBILIDADE", "PENHORA")).map(commons::mapWorkItem).toList();
    }

    public List<Map<String, Object>> listarCertidoesPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "CERTIDAO", "BUSCA", "MATRICULA"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public List<Map<String, Object>> listarIndisponibilidadesPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "INDISPONIBILIDADE", "CNIB", "AVERBACAO_PREMONITORIA"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public Map<String, Object> monitoramentoOperacional() {
        Usuario usuario = contextFactory.build().usuario();
        long certidoes = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "CERTIDAO", "BUSCA", "MATRICULA")).count();
        long indisponibilidades = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "INDISPONIBILIDADE", "CNIB", "AVERBACAO_PREMONITORIA")).count();
        long averbacoes = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "AVERBACAO", "REGISTRO", "PENHORA")).count();
        long urgentes = commons.inboxHibrido(usuario, 20).stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("usuario", usuario.getNome());
        out.put("comarca", usuario.getComarca());
        out.put("certidoesPendentes", certidoes);
        out.put("indisponibilidadesPendentes", indisponibilidades);
        out.put("averbacoesPendentes", averbacoes);
        out.put("itensUrgentes", urgentes);
        out.put("nivelPressao", urgentes >= 3 ? "ELEVADA" : urgentes >= 1 ? "MODERADA" : "ESTAVEL");
        return out;
    }

}
