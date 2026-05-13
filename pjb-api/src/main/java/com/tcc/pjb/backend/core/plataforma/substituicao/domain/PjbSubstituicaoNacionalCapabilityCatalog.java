package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class PjbSubstituicaoNacionalCapabilityCatalog {

    private static final Set<PjbSubstituicaoSistemaLegado> TODOS = EnumSet.allOf(PjbSubstituicaoSistemaLegado.class);

    private static final List<PjbSubstituicaoCapacidadeNacional> CAPACIDADES = List.of(
            presente("tramite-processual-nacional", "Tramitação processual nacional", TODOS, "core.processo, core.procedural, service.secretariat", "Ciclo de vida processual, classificação, competência, secretaria, gabinete e ritos", "Readiness por tribunal, grau, unidade e competência"),
            presente("interoperabilidade-mni-datajud", "Interoperabilidade MNI, PDPJ e DataJud", todos(PjbSubstituicaoSistemaLegado.PJE, PjbSubstituicaoSistemaLegado.PJE_2X, PjbSubstituicaoSistemaLegado.EPROC), "integration.mni, integration.judicial", "MNI, DataJud, conectores judiciais, replay e health de conectores", "Capability registry por tribunal e homologação de conector"),
            presente("malha-recursal", "Malha recursal multigrau", todos(PjbSubstituicaoSistemaLegado.PJE, PjbSubstituicaoSistemaLegado.PJE_2X, PjbSubstituicaoSistemaLegado.ESAJ, PjbSubstituicaoSistemaLegado.EPROC, PjbSubstituicaoSistemaLegado.CRETA), "core.kernel.recursal", "State machine, workspace, formalização, preparo, admissibilidade e acórdão", "Incidentes de uniformização e TNU como capability explícita"),
            parcial("portal-publico-profissional", "Portal público e profissional", todos(PjbSubstituicaoSistemaLegado.ESAJ, PjbSubstituicaoSistemaLegado.EPROC, PjbSubstituicaoSistemaLegado.PJE, PjbSubstituicaoSistemaLegado.PJE_2X), "core.frontend.delivery, core.processo.busca, core.peticionamento, core.frontend.publicaccess", "Consulta, entrega frontend, peticionamento, capacidades públicas, acesso por chave, push e conferência documental", "Consolidar controladores públicos finais sem duplicar superfícies"),
            parcial("preservacao-documental-lta", "Preservação documental e assinatura de longo prazo", TODOS, "core.icp, core.document, core.kernel.recursal", "ICP-Brasil, metadados, evidência documental, hash, HSM mock e PAdES-LTA", "TSA real em produção, QR público e política documental por ato"),
            parcial("migracao-acervo", "Migração industrial de acervo", TODOS, "core.plataforma.substituicao, core.processo.migracao", "Batch de migração, homologação de tribunal e execução nacional", "Reconciliação de documentos, partes, movimentos, classes, assuntos, sigilo e protocolo original"),
            parcial("operacao-nacional", "Operação nacional e resiliência", TODOS, "core.observability, platform.runtime, integration.judicial", "SLO, runtime pressure, telemetria, crise operacional e replay", "Runbook nacional, fila morta, disaster recovery e readiness operacional por tribunal"),
            presente("readiness-tribunal-producao", "Readiness de produção por tribunal", TODOS, "core.plataforma.substituicao.readiness", "Snapshot de capabilities, bloqueios, homologação e status de produção por tribunal", "Conectar ao painel administrativo e aos probes de homologação"),
            presente("indisponibilidade-prazos", "Indisponibilidade e impacto em prazos", TODOS, "core.observability.unavailability, core.prazos", "Política de impacto por serviço externo crítico, janela final e próximo dia útil", "Emitir certidão pública com trilha técnica"),
            presente("compatibilidade-mni", "Matriz de compatibilidade MNI", todos(PjbSubstituicaoSistemaLegado.PJE, PjbSubstituicaoSistemaLegado.PJE_2X, PjbSubstituicaoSistemaLegado.EPROC), "integration.mni.compatibility", "Nível por tribunal e operação com status verificado, degradado, bloqueado ou não declarado", "Materializar evidências de homologação por endpoint"),
            parcial("implantacao-compacta-projudi", "Implantação compacta para tribunais pequenos e médios", todos(PjbSubstituicaoSistemaLegado.PROJUDI), "infra, scripts, core.plataforma.substituicao", "Docker, Kubernetes, scripts, guards e núcleo processual já existem", "Perfil operacional compacto com backup, restore, atualização segura e administração mínima"),
            presente("chave-processo-documento", "Chave de processo e documento", todos(PjbSubstituicaoSistemaLegado.EPROC, PjbSubstituicaoSistemaLegado.ESAJ), "core.security.accesskey, core.processo.sigilo, core.icp", "Política de chave revogável com expiração, escopo, sigilo e decisão auditável", "Persistência e superfície pública controlada por perfil"),
            faltante("jef-creta-produto", "Produto completo de Juizado Especial Federal", todos(PjbSubstituicaoSistemaLegado.CRETA, PjbSubstituicaoSistemaLegado.EPROC), "core.procedural, core.kernel.recursal, core.processo.painel", "Ritos, previdenciário, recursal e painéis processuais já existem", "Atermação, perícia, cálculo, audiência, sentença, execução simplificada e TNU")
    );

    private PjbSubstituicaoNacionalCapabilityCatalog() {
    }

    public static List<PjbSubstituicaoCapacidadeNacional> capacidades() {
        return CAPACIDADES;
    }

    public static List<PjbSubstituicaoCapacidadeNacional> pendencias() {
        return CAPACIDADES.stream().filter(PjbSubstituicaoCapacidadeNacional::pendente).toList();
    }

    public static List<PjbSubstituicaoCapacidadeNacional> porSistema(PjbSubstituicaoSistemaLegado sistema) {
        return CAPACIDADES.stream().filter(capacidade -> capacidade.sistemasLegados().contains(sistema)).toList();
    }

    private static PjbSubstituicaoCapacidadeNacional presente(String codigo,
                                                              String titulo,
                                                              Set<PjbSubstituicaoSistemaLegado> sistemas,
                                                              String eixo,
                                                              String existente,
                                                              String proxima) {
        return capacidade(codigo, titulo, sistemas, PjbSubstituicaoCapacidadeStatus.PRESENTE, eixo, existente, proxima);
    }

    private static PjbSubstituicaoCapacidadeNacional parcial(String codigo,
                                                             String titulo,
                                                             Set<PjbSubstituicaoSistemaLegado> sistemas,
                                                             String eixo,
                                                             String existente,
                                                             String proxima) {
        return capacidade(codigo, titulo, sistemas, PjbSubstituicaoCapacidadeStatus.PARCIAL, eixo, existente, proxima);
    }

    private static PjbSubstituicaoCapacidadeNacional faltante(String codigo,
                                                              String titulo,
                                                              Set<PjbSubstituicaoSistemaLegado> sistemas,
                                                              String eixo,
                                                              String existente,
                                                              String proxima) {
        return capacidade(codigo, titulo, sistemas, PjbSubstituicaoCapacidadeStatus.FALTANTE, eixo, existente, proxima);
    }

    private static PjbSubstituicaoCapacidadeNacional capacidade(String codigo,
                                                                String titulo,
                                                                Set<PjbSubstituicaoSistemaLegado> sistemas,
                                                                PjbSubstituicaoCapacidadeStatus status,
                                                                String eixo,
                                                                String existente,
                                                                String proxima) {
        return new PjbSubstituicaoCapacidadeNacional(codigo, titulo, sistemas, status, eixo, existente, proxima);
    }

    private static Set<PjbSubstituicaoSistemaLegado> todos(PjbSubstituicaoSistemaLegado primeiro,
                                                           PjbSubstituicaoSistemaLegado... restantes) {
        EnumSet<PjbSubstituicaoSistemaLegado> sistemas = EnumSet.of(primeiro, restantes);
        return Set.copyOf(sistemas);
    }
}
